package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.CaseIdUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.DeserializeValuesUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapMerger;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapSerializer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.StringResourceLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider.AUTHORIZATION;

@Service
public class MessageInjector {

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    @Autowired
    private AzureMessageInjector azureMessageInjector;

    @Autowired
    private RestMessageService restMessageService;

    @Autowired
    private DeserializeValuesUtil deserializeValuesUtil;

    public void injectMessage(Map<String, Object> clauseValues,
                              TestScenario scenario,
                              String jurisdiction,
                              Headers authorizationHeaders) throws IOException {

        String jurisdictionId = jurisdiction.toLowerCase(Locale.ENGLISH);
        Map<String, String> eventMessageTemplatesByFilename =
            StringResourceLoader.load(
                "/templates/" + jurisdictionId + "/message/*.json"
            );

        String userToken = authorizationHeaders.getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationHeadersProvider.getUserInfo(userToken);

        List<Map<String, Object>> messagesToSend = new ArrayList<>(Objects.requireNonNull(
            MapValueExtractor.extract(clauseValues, "request.input.eventMessages")));

        messagesToSend.forEach(messageData -> {
            try {
                String testCaseId = CaseIdUtil.extractAssignedCaseIdOrDefault(messageData, scenario);
                String eventMessage = getMessageData(messageData,
                                                     eventMessageTemplatesByFilename,
                                                     testCaseId,
                                                     userInfo.getEmail());
                String destination = MapValueExtractor.extractOrDefault(messageData, "destination", "ASB");

                sendMessage(eventMessage, testCaseId, jurisdictionId, destination);
            } catch (IOException e) {
                System.out.println("Could not create a message");
                e.printStackTrace();
            }
        });
    }

    private void sendMessage(String message, String caseId, String jurisdictionId, String destination) {
        if ("RestEndpoint".equals(destination)) {
            restMessageService.sendMessage(message, caseId, false);
        } else if ("RestEndpointFromDLQ".equals(destination)) {
            restMessageService.sendMessage(message, caseId, true);
        } else {
            // inject into ASB
            azureMessageInjector.sendMessage(message, caseId, jurisdictionId);
        }
    }


    private String getMessageData(
        Map<String, Object> messageDataInput,
        Map<String, String> templatesByFilename,
        String caseId,
        String email
    ) throws IOException {

        String templateFilename = MapValueExtractor.extract(messageDataInput, "template");

        Map<String, String> additionalValues = Map.of(
            "caseId", caseId,
            "userId", email
        );

        Map<String, Object> eventMessageData = deserializeValuesUtil.deserializeStringWithExpandedValues(
            templatesByFilename.get(templateFilename),
            additionalValues
        );

        Map<String, Object> eventMessageDataReplacements = MapValueExtractor.extract(messageDataInput, "replacements");

        if (eventMessageDataReplacements != null) {
            MapMerger.merge(eventMessageData, eventMessageDataReplacements);
        }

        return MapSerializer.serialize(eventMessageData);
    }

}
