package uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.http.Headers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionEvaluationLogger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.TaskManagementService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.DeserializeValuesUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.Logger;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapMerger;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapSerializer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.StringResourceLoader;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.Verifier;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_POLL_INTERVAL_SECONDS;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_TIMEOUT_SECONDS;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_FAILED;

@Component
@Slf4j
public class TaskMgmApiRetrieverService implements TaskRetrieverService {

    private final TaskManagementService taskManagementService;
    private final DeserializeValuesUtil deserializeValuesUtil;
    private final List<Verifier> verifiers;

    public TaskMgmApiRetrieverService(TaskManagementService taskManagementService,
                                      DeserializeValuesUtil deserializeValuesUtil,
                                      List<Verifier> verifiers) {
        this.taskManagementService = taskManagementService;
        this.deserializeValuesUtil = deserializeValuesUtil;
        this.verifiers = verifiers;
    }

    @SneakyThrows
    @Override
    public void retrieveTask(Map<String, Object> clauseValues, TestScenario scenario, String caseId) {
        retrieveTask(clauseValues, scenario, singletonList(caseId));
    }

    @SneakyThrows
    public void retrieveTask(Map<String, Object> clauseValues, TestScenario scenario, List<String> caseIds) {

        Map<String, String> taskTemplatesByFilename =
            StringResourceLoader.load(
                "/templates/" + scenario.getJurisdiction().toLowerCase(Locale.ENGLISH) + "/task/*.json"
            );

        Map<String, String> additionalValues;
        if (scenario.getAssignedCaseIdMap() != null && scenario.getAssignedCaseIdMap().size() > 1) {
            additionalValues = scenario.getAssignedCaseIdMap();
        } else {
            additionalValues = Map.of("caseId", caseIds.get(0));
        }

        Map<String, Object> deserializedClauseValues =
            deserializeValuesUtil.expandMapValues(clauseValues, additionalValues);

        AtomicBoolean isTestPassed = new AtomicBoolean(false);

        await()
            .ignoreException(AssertionError.class)
            .conditionEvaluationListener(new ConditionEvaluationLogger(log::info))
            .pollInterval(DEFAULT_POLL_INTERVAL_SECONDS, SECONDS)
            .atMost(DEFAULT_TIMEOUT_SECONDS, SECONDS)
            .until(
                () -> {

                    String searchByCaseIdResponseBody = taskManagementService.search(
                        deserializedClauseValues,
                        caseIds,
                        scenario
                    );

                    String expectedResponseBody = buildTaskExpectationResponseBody(
                        deserializedClauseValues,
                        taskTemplatesByFilename,
                        additionalValues
                    );

                    if (searchByCaseIdResponseBody.isBlank()) {
                        log.error("Find my case ID response is empty. Test will now fail");
                        return false;
                    }

                    Map<String, Object> actualResponse = MapSerializer.deserialize(
                        MapSerializer.sortCollectionElement(
                            searchByCaseIdResponseBody,
                            "tasks",
                            taskTitleComparator()
                        ));
                    Map<String, Object> expectedResponse = MapSerializer.deserialize(expectedResponseBody);

                    verifiers.forEach(verifier ->
                        verifier.verify(
                            clauseValues,
                            expectedResponse,
                            actualResponse
                        )
                    );

                    List<Map<String, Object>> tasks = MapValueExtractor.extract(actualResponse, "tasks");

                    if (tasks == null || tasks.isEmpty()) {
                        log.error("Task list is empty. Test will now fail");
                        return false;
                    }

                    String taskId = MapValueExtractor.extract(tasks.get(0), "id");
                    log.info("task id is {}", taskId);

                    String retrieveTaskRolePermissionsResponseBody =
                        taskManagementService.retrieveTaskRolePermissions(
                            clauseValues,
                            taskId,
                            scenario.getExpectationAuthorizationHeaders()
                        );

                    if (retrieveTaskRolePermissionsResponseBody.isBlank()) {
                        log.error("Task role permissions response is empty. Test will now fail");
                        return false;
                    }

                    String rolesExpectationResponseBody = buildRolesExpectationResponseBody(
                        deserializedClauseValues,
                        additionalValues
                    );

                    log.info("expected roles: {}", rolesExpectationResponseBody);
                    Map<String, Object> actualRoleResponse = MapSerializer.deserialize(
                        retrieveTaskRolePermissionsResponseBody);
                    Map<String, Object> expectedRoleResponse = MapSerializer.deserialize(
                        rolesExpectationResponseBody);

                    verifiers.forEach(verifier ->
                        verifier.verify(
                            clauseValues,
                            expectedRoleResponse,
                            actualRoleResponse
                        )
                    );


                    isTestPassed.set(true);
                    return true;
                });

        if (!isTestPassed.get()) {
            Logger.say(SCENARIO_FAILED, scenario.getScenarioMapValues().get("description"));
        }
    }

    private Comparator<JsonNode> taskTitleComparator() {
        return (j1, j2) -> {
            String title1 = j1.findValue("task_title").asText();
            String title2 = j2.findValue("task_title").asText();
            return title1.compareTo(title2);
        };
    }

    private String buildTaskExpectationResponseBody(Map<String, Object> clauseValues,
                                                    Map<String, String> taskTemplatesByFilename,
                                                    Map<String, String> additionalValues) throws IOException {

        Map<String, Object> scenario = deserializeValuesUtil.expandMapValues(clauseValues, additionalValues);

        Map<String, Object> taskData = MapValueExtractor.extract(scenario, "taskData");

        String templateFilename = MapValueExtractor.extract(taskData, "template");

        Map<String, Object> taskDataExpectation = deserializeValuesUtil.deserializeStringWithExpandedValues(
            taskTemplatesByFilename.get(templateFilename),
            additionalValues
        );

        Map<String, Object> taskDataDataReplacements = MapValueExtractor.extract(taskData, "replacements");
        if (taskDataDataReplacements != null) {
            MapMerger.merge(taskDataExpectation, taskDataDataReplacements);
        }

        return MapSerializer.serialize(taskDataExpectation);

    }

    private String buildRolesExpectationResponseBody(Map<String, Object> clauseValues,
                                                     Map<String, String> additionalValues) throws IOException {

        Map<String, Object> scenario = deserializeValuesUtil.expandMapValues(clauseValues, additionalValues);
        Map<String, Object> roleData = MapValueExtractor.extract(scenario, "roleData");
        return MapSerializer.serialize(roleData);
    }

    public void performOperation(Headers authorizationHeaders) {
        await()
            .ignoreException(AssertionError.class)
            .conditionEvaluationListener(new ConditionEvaluationLogger(log::info))
            .pollInterval(DEFAULT_POLL_INTERVAL_SECONDS, SECONDS)
            .atMost(DEFAULT_TIMEOUT_SECONDS, SECONDS)
            .until(
                () -> {
                    taskManagementService.performOperation(authorizationHeaders);
                    return true;
                });
    }
}
