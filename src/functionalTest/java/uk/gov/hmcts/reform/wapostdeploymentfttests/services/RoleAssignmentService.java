package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.clients.RoleAssignmentServiceApiClient;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.StringResourceLoader;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ofPattern;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider.AUTHORIZATION;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor.extractOrDefault;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor.extractOrThrow;

@Component
@Slf4j
public class RoleAssignmentService {

    public static final DateTimeFormatter ROLE_ASSIGNMENT_DATA_TIME_FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

    private final ObjectMapper objectMapper;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;
    private final RoleAssignmentServiceApiClient roleAssignmentServiceApi;

    public RoleAssignmentService(AuthorizationHeadersProvider authorizationHeadersProvider,
                                 ObjectMapper objectMapper,
                                 RoleAssignmentServiceApiClient roleAssignmentServiceApi) {
        this.authorizationHeadersProvider = authorizationHeadersProvider;
        this.objectMapper = objectMapper;
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
    }

    public void processRoleAssignments(TestScenario scenario,
                                       Map<String, Object> postRoleAssignmentValues) throws IOException {

        String caseId = scenario.getAssignedCaseId("defaultCaseId");
        String requestCredentials = extractOrThrow(postRoleAssignmentValues, "credentials");

        Map<String, Object> roleDataValues = extractOrThrow(postRoleAssignmentValues, "roleData");
        Map<String, Object> replacementsValues = extractOrThrow(roleDataValues, "replacements");

        String jurisdiction = extractOrThrow(replacementsValues, "jurisdiction");
        Map<String, String> templatesByFilename =
            StringResourceLoader.load(
                "/templates/" + jurisdiction.toLowerCase(Locale.ENGLISH) + "/roleAssignment/*.json"
            );
        String templateFilename = extractOrThrow(roleDataValues, "template");
        String template = templatesByFilename.get(templateFilename);

        String roleName = extractOrThrow(replacementsValues, "roleName");
        String caseType = extractOrThrow(replacementsValues, "caseType");
        String grantType = extractOrDefault(replacementsValues, "grantType", "STANDARD");
        String roleType = extractOrThrow(replacementsValues, "roleType");
        String classification = extractOrDefault(replacementsValues, "classification", "PUBLIC");
        String roleCategory = extractOrDefault(replacementsValues, "roleCategory", "LEGAL_OPERATIONS");


        Headers requestAuthorizationHeaders = authorizationHeadersProvider
            .getAuthorizationHeaders(requestCredentials);
        Headers requestAuthorizationHeaders_WaSystemUser = authorizationHeadersProvider
            .getAuthorizationHeaders("WaSystemUser");


        String assignerToken = requestAuthorizationHeaders_WaSystemUser.getValue(AUTHORIZATION);
        UserInfo assignerUserInfo = authorizationHeadersProvider.getUserInfo(assignerToken);


        String assigneeToken = requestAuthorizationHeaders.getValue(AUTHORIZATION);
        String serviceToken = requestAuthorizationHeaders.getValue(SERVICE_AUTHORIZATION);
        UserInfo assigneeUserInfo = authorizationHeadersProvider.getUserInfo(assigneeToken);

        postRoleAssignment(
            caseId,
            assignerToken,
            serviceToken,
            assigneeUserInfo.getUid(),
            roleName,
            toJsonString(Map.of(
                "caseId", caseId,
                "caseType", caseType,
                "jurisdiction", jurisdiction,
                "substantive", "Y"
            )),
            template,
            grantType,
            roleCategory,
            toJsonString(List.of()),
            roleType,
            classification,
            roleName,
            UUID.randomUUID().toString(),
            true,
            false,
            null,
            "2023-01-01T00:00:00Z",
            null,
            assignerUserInfo.getUid()
        );
    }

    private String toJsonString(Map<String, String> attributes) {
        String json = null;

        try {
            json = objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    private String toJsonString(List<String> attributes) {
        String json = null;

        try {
            json = objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }

    private void postRoleAssignment(String caseId,
                                    String bearerUserToken,
                                    String s2sToken,
                                    String actorId,
                                    String roleName,
                                    String attributes,
                                    String resourceFile,
                                    String grantType,
                                    String roleCategory,
                                    String authorisations,
                                    String roleType,
                                    String classification,
                                    String process,
                                    String reference,
                                    boolean replaceExisting,
                                    Boolean readOnly,
                                    String notes,
                                    String beginTime,
                                    String endTime,
                                    String assignerId) {

        String body = getBody(caseId, actorId, roleName, resourceFile, attributes, grantType, roleCategory,
            authorisations, roleType, classification, process, reference, replaceExisting,
            readOnly, notes, beginTime, endTime, assignerId);

        log.info("actorId------>:{}",actorId);
        log.info("");
        log.info("Auth token------>:{}",bearerUserToken);
        log.info("");
        log.info("Service token------>:{}",s2sToken);
        log.info("");
        log.info("Request body------>:{}",body);
        log.info("");

        roleAssignmentServiceApi.createRoleAssignment(
            body,
            bearerUserToken,
            s2sToken
        );
    }

    private String getBody(final String caseId,
                           String actorId,
                           final String roleName,
                           final String resourceFile,
                           final String attributes,
                           final String grantType,
                           String roleCategory,
                           String authorisations,
                           String roleType,
                           String classification,
                           String process,
                           String reference,
                           boolean replaceExisting,
                           Boolean readOnly,
                           String notes,
                           String beginTime,
                           String endTime,
                           String assignerId) {

        String assignmentRequestBody = null;

        assignmentRequestBody = resourceFile;
        assignmentRequestBody = assignmentRequestBody.replace("{ACTOR_ID_PLACEHOLDER}", actorId);
        assignmentRequestBody = assignmentRequestBody.replace("{ROLE_NAME_PLACEHOLDER}", roleName);
        assignmentRequestBody = assignmentRequestBody.replace("{GRANT_TYPE}", grantType);
        assignmentRequestBody = assignmentRequestBody.replace("{ROLE_CATEGORY}", roleCategory);
        assignmentRequestBody = assignmentRequestBody.replace("{ROLE_TYPE}", roleType);
        assignmentRequestBody = assignmentRequestBody.replace("{CLASSIFICATION}", classification);
        assignmentRequestBody = assignmentRequestBody.replace("{PROCESS}", process);
        assignmentRequestBody = assignmentRequestBody.replace("{ASSIGNER_ID_PLACEHOLDER}", assignerId);

        assignmentRequestBody = assignmentRequestBody.replace(
            "\"replaceExisting\": \"{REPLACE_EXISTING}\"",
            String.format("\"replaceExisting\": %s", replaceExisting)
        );

        if (beginTime != null) {
            assignmentRequestBody = assignmentRequestBody.replace(
                "{BEGIN_TIME_PLACEHOLDER}",
                beginTime
            );
        } else {
            assignmentRequestBody = assignmentRequestBody
                .replace(",\n" + "      \"beginTime\": \"{BEGIN_TIME_PLACEHOLDER}\"", "");
        }

        if (endTime != null) {
            assignmentRequestBody = assignmentRequestBody.replace(
                "{END_TIME_PLACEHOLDER}",
                endTime
            );
        } else {
            assignmentRequestBody = assignmentRequestBody.replace(
                "{END_TIME_PLACEHOLDER}",
                ZonedDateTime.now().plusHours(2).format(ROLE_ASSIGNMENT_DATA_TIME_FORMATTER)
            );
        }

        if (attributes != null) {
            assignmentRequestBody = assignmentRequestBody
                .replace("\"{ATTRIBUTES_PLACEHOLDER}\"", attributes);
        }

        if (caseId != null) {
            assignmentRequestBody = assignmentRequestBody.replace("{CASE_ID_PLACEHOLDER}", caseId);
        }

        assignmentRequestBody = assignmentRequestBody.replace("{REFERENCE}", reference);


        if (notes != null) {
            assignmentRequestBody = assignmentRequestBody.replace(
                "\"notes\": \"{NOTES}\"",
                String.format("\"notes\": [%s]", notes)
            );
        } else {
            assignmentRequestBody = assignmentRequestBody
                .replace(",\n" + "      \"notes\": \"{NOTES}\"", "");
        }

        if (readOnly != null) {
            assignmentRequestBody = assignmentRequestBody.replace(
                "\"readOnly\": \"{READ_ONLY}\"",
                String.format("\"readOnly\": %s", readOnly)
            );
        } else {
            assignmentRequestBody = assignmentRequestBody
                .replace(",\n" + "      \"readOnly\": \"{READ_ONLY}\"", "");
        }

        if (authorisations != null) {
            assignmentRequestBody = assignmentRequestBody.replace("\"{AUTHORISATIONS}\"", authorisations);
        } else {
            assignmentRequestBody = assignmentRequestBody
                .replace(",\n" + "      \"authorisations\": \"{AUTHORISATIONS}\"", "");
        }

        return assignmentRequestBody;
    }
}
