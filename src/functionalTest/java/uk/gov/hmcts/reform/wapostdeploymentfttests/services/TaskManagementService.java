package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.DeserializeValuesUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
public class TaskManagementService {

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    @Autowired
    private MapValueExpander mapValueExpander;
    @Autowired
    private DeserializeValuesUtil deserializeValuesUtil;
    @Autowired
    private TaskMonitorService taskMonitorService;

    @Value("${wa_task_management_api.url}")
    private String taskManagementUrl;

    public String search(Map<String, Object> clauseValues,
                         List caseIds,
                         TestScenario scenario) {

        Headers authorizationHeaders = scenario.getExpectationAuthorizationHeaders();

        Map<String, Object> searchParameter = Map.of(
            "key", "caseId",
            "operator", "IN",
            "values", caseIds
        );

        scenario.addSearchMap(searchParameter);
        Map<String, Set<Map<String, Object>>> requestBody = Map.of("search_parameters", scenario.getSearchMap());

        //Also trigger (CRON) Jobs programmatically
        taskMonitorService.triggerInitiationJob(authorizationHeaders);
        taskMonitorService.triggerTerminationJob(authorizationHeaders);
        taskMonitorService.triggerReconfigurationJob(authorizationHeaders);

        int expectedStatus = MapValueExtractor.extractOrDefault(
            clauseValues, "status", 200);
        int expectedTasks = MapValueExtractor.extractOrDefault(
            clauseValues, "numberOfTasksAvailable", 1);

        Response result = given()
            .headers(authorizationHeaders)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(requestBody)
            .when()
            .post(taskManagementUrl + "/task/extended-search");

        result.then().assertThat()
            .statusCode(expectedStatus)
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", is(expectedTasks));

        String actualResponseBody = result.then()
            .extract()
            .body().asString();

        log.info("Response body: " + actualResponseBody);

        return actualResponseBody;
    }

    public String retrieveTaskRolePermissions(Map<String, Object> clauseValues,
                                              String taskId,
                                              Headers authorizationHeaders) {

        Response result = given()
            .headers(authorizationHeaders)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get(taskManagementUrl + "/task/" + taskId + "/roles");


        int expectedStatus = MapValueExtractor.extractOrDefault(
            clauseValues, "status", 200);

        int expectedRoles = MapValueExtractor.extractOrDefault(
            clauseValues, "numberOfRolesAvailable", 4);

        result.then().assertThat()
            .statusCode(expectedStatus)
            .contentType(APPLICATION_JSON_VALUE)
            .body("roles.size()", is(expectedRoles));

        String actualResponseBody = result.then()
            .extract()
            .body().asString();

        log.info("Response body: " + actualResponseBody);

        return actualResponseBody;
    }

    public void performOperation(Headers authorizationHeaders) {
        taskMonitorService.triggerReconfigurationJob(authorizationHeaders);
    }
}
