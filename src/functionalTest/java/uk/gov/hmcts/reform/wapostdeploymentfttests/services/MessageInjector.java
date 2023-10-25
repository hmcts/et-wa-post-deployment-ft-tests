package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import lombok.extern.slf4j.Slf4j;
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

@Service
@Slf4j
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
                              UserInfo userInfo) throws IOException {
        String jurisdictionId = scenario.getJurisdiction().toLowerCase(Locale.ENGLISH);
        Map<String, String> eventMessageTemplatesByFilename =
            StringResourceLoader.load(
                "/templates/" + jurisdictionId + "/message/*.json"
            );

        List<Map<String, Object>> messagesToSend = new ArrayList<>(Objects.requireNonNull(
            MapValueExtractor.extract(clauseValues, "input.eventMessages")));

        messagesToSend.forEach(messageData -> {
            try {
                String testCaseId = CaseIdUtil.extractAssignedCaseIdOrDefault(messageData, scenario);
                String eventMessage = getMessageData(messageData,
                                                     eventMessageTemplatesByFilename,
                                                     testCaseId,
                                                     userInfo.getEmail());
                String destination = MapValueExtractor.extractOrDefault(messageData, "destination", "ASB");

                log.info("Sending message case id: {}, destination: {}, message: {} ",
                         testCaseId, destination, eventMessage);
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
