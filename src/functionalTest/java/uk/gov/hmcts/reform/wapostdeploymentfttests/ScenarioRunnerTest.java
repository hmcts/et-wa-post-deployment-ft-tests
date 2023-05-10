package uk.gov.hmcts.reform.wapostdeploymentfttests;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionEvaluationLogger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StopWatch;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever.TaskRetrieverEnum;
import uk.gov.hmcts.reform.wapostdeploymentfttests.preparers.Preparer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.CcdCaseCreator;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.MessageInjector;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.RestMessageService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.RoleAssignmentService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever.CamundaTaskRetrieverService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever.TaskMgmApiRetrieverService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.CaseIdUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.DeserializeValuesUtil;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.Logger;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapSerializer;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.StringResourceLoader;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.TaskDataVerifier;
import uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers.Verifier;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.CaseIdUtil.addAssignedCaseId;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_BEFORE_COMPLETED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_BEFORE_FOUND;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_DISABLED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_ENABLED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_FINISHED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_ROLE_ASSIGNMENT_COMPLETED;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_ROLE_ASSIGNMENT_FOUND;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_RUNNING;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_RUNNING_TIME;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_START;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.LoggerMessage.SCENARIO_SUCCESSFUL;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander.ENVIRONMENT_PROPERTIES;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor.extractOrDefault;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor.extractOrThrow;

@Slf4j
public class ScenarioRunnerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    protected MessageInjector messageInjector;
    @Autowired
    protected TaskDataVerifier taskDataVerifier;
    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @Autowired
    private CamundaTaskRetrieverService camundaTaskRetrievableService;
    @Autowired
    private TaskMgmApiRetrieverService taskMgmApiRetrievableService;
    @Autowired
    private RoleAssignmentService roleAssignmentService;
    @Autowired
    private Environment environment;
    @Autowired
    private DeserializeValuesUtil deserializeValuesUtil;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private List<Verifier> verifiers;
    @Autowired
    private List<Preparer> preparers;
    @Autowired
    private CcdCaseCreator ccdCaseCreator;
    @Autowired
    private RestMessageService restMessageService;
    @Value("${et-wa-post-deployment-test.environment}")
    protected String postDeploymentTestEnvironment;

    @Before
    public void setUp() {
        MapSerializer.setObjectMapper(objectMapper);
    }

    @Test
    public void scenarios_should_behave_as_specified() throws IOException, URISyntaxException {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();


        loadPropertiesIntoMapValueExpander();

        for (Preparer preparer : preparers) {
            preparer.prepare();
        }

        assertFalse("Verifiers configured successfully", verifiers.isEmpty());

        URL path = getClass().getClassLoader().getResource("scenarios");
        File[] directories = new File(path.toURI()).listFiles(File::isDirectory);
        Objects.requireNonNull(directories, "No directories found under 'scenarios'");

        for (File directory : directories) {
            runAllScenariosFor(directory.getName());
        }

        stopWatch.stop();
        Logger.say(SCENARIO_RUNNING_TIME, stopWatch.getTotalTimeSeconds());
    }

    private void runAllScenariosFor(String directoryName) throws IOException {
        String scenarioPattern = System.getProperty("scenario");
        if (scenarioPattern == null) {
            scenarioPattern = "*.json";
        } else {
            scenarioPattern = "*" + scenarioPattern + "*.json";
        }

        Collection<String> scenarioSources =
            StringResourceLoader
                .load("/scenarios/" + directoryName + "/" + scenarioPattern)
                .values();

        Logger.say(SCENARIO_START, scenarioSources.size() + " " + directoryName.toUpperCase(Locale.ROOT));

        for (String scenarioSource : scenarioSources) {

            Map<String, Object> scenarioValues = deserializeValuesUtil
                .deserializeStringWithExpandedValues(scenarioSource, emptyMap());

            String description = extractOrDefault(scenarioValues, "description", "Unnamed scenario");
            String testType = extractOrDefault(scenarioValues, "testType", "default");

            Boolean scenarioEnabled = extractOrDefault(scenarioValues, "enabled", true);

            if (!scenarioEnabled) {
                Logger.say(SCENARIO_DISABLED, description);
                continue;
            } else {
                Logger.say(SCENARIO_ENABLED, description);

                Map<String, Object> beforeClauseValues = extractOrDefault(scenarioValues, "before", null);
                Map<String, Object> testClauseValues = extractOrThrow(scenarioValues, "test");
                Map<String, Object> postRoleAssignmentClauseValues = extractOrDefault(scenarioValues,
                    "postRoleAssignments", null);

                String scenarioJurisdiction = extractOrThrow(scenarioValues, "jurisdiction");
                String caseType = extractOrThrow(scenarioValues, "caseType");

                TestScenario scenario = new TestScenario(
                    scenarioValues,
                    scenarioSource,
                    scenarioJurisdiction,
                    caseType,
                    beforeClauseValues,
                    testClauseValues,
                    postRoleAssignmentClauseValues
                );
                createBaseCcdCase(scenario);

                addSearchParameters(scenario, scenarioValues);

                if (scenario.getBeforeClauseValues() != null) {
                    Logger.say(SCENARIO_BEFORE_FOUND);
                    //If before was found process with before values
                    processBeforeClauseScenario(scenario);
                    Logger.say(SCENARIO_BEFORE_COMPLETED);
                }

                if (scenario.getPostRoleAssignmentClauseValues() != null) {
                    Logger.say(SCENARIO_ROLE_ASSIGNMENT_FOUND);
                    roleAssignmentService.processRoleAssignments(scenario, postRoleAssignmentClauseValues);
                    Logger.say(SCENARIO_ROLE_ASSIGNMENT_COMPLETED);
                }

                if (testType.equals("Reconfiguration")) {
                    updateBaseCcdCase(scenario);
                }

                Logger.say(SCENARIO_RUNNING);
                processTestClauseScenario(scenario, testType);

                Logger.say(SCENARIO_SUCCESSFUL, description);
                Logger.say(SCENARIO_FINISHED);
            }
        }
    }

    private void processBeforeClauseScenario(TestScenario scenario) throws IOException {
        processScenario(scenario.getBeforeClauseValues(), scenario, "");
    }

    private void processTestClauseScenario(TestScenario scenario, String testType) throws IOException {
        processScenario(scenario.getTestClauseValues(), scenario, testType);
    }

    private void createBaseCcdCase(TestScenario scenario) {
        Map<String, Object> scenarioValues = scenario.getScenarioMapValues();
        String requestCredentials = extractOrThrow(scenarioValues, "required.credentials");

        Headers requestAuthorizationHeaders = authorizationHeadersProvider
            .getAuthorizationHeaders(requestCredentials);

        scenario.setRequestAuthorizationHeaders(requestAuthorizationHeaders);

        List<Map<String, Object>> ccdCaseToCreate = new ArrayList<>(Objects.requireNonNull(
            MapValueExtractor.extract(scenarioValues, "required.ccd")));

        ccdCaseToCreate.forEach(caseValues -> {
            try {
                String caseId = ccdCaseCreator.createCase(
                    caseValues,
                    scenario.getJurisdiction(),
                    scenario.getCaseType(),
                    requestAuthorizationHeaders
                );
                addAssignedCaseId(caseValues, caseId, scenario);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void updateBaseCcdCase(TestScenario scenario) {
        Map<String, Object> scenarioValues = scenario.getScenarioMapValues();
        String requestCredentials = extractOrThrow(scenarioValues, "required.credentials");

        Headers requestAuthorizationHeaders = authorizationHeadersProvider
            .getAuthorizationHeaders(requestCredentials);

        scenario.setRequestAuthorizationHeaders(requestAuthorizationHeaders);

        List<Map<String, Object>> ccdCaseToUpdate = new ArrayList<>(Objects.requireNonNull(
            MapValueExtractor.extract(scenarioValues, "requiredUpdate.ccd")));

        ccdCaseToUpdate.forEach(caseValues -> {
            try {
                String caseId = CaseIdUtil.extractAssignedCaseIdOrDefault(caseValues, scenario);
                ccdCaseCreator.updateCase(
                    caseId,
                    caseValues,
                    scenario.getJurisdiction(),
                    scenario.getCaseType(),
                    requestAuthorizationHeaders
                );
                addAssignedCaseId(caseValues, caseId, scenario);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void processScenario(Map<String, Object> values,
                                 TestScenario scenario,
                                 String testType) throws IOException {

        messageInjector.injectMessage(
            values,
            scenario,
            scenario.getJurisdiction(),
            scenario.getRequestAuthorizationHeaders()
        );

        if (testType.equals("Reconfiguration") && postDeploymentTestEnvironment.equals("local")) {
            await()
                .ignoreException(AssertionError.class)
                .conditionEvaluationListener(new ConditionEvaluationLogger(log::info))
                .pollInterval(10, SECONDS)
                .atMost(DEFAULT_TIMEOUT_SECONDS, SECONDS)
                .until(
                    () -> {
                        taskMgmApiRetrievableService.performOperation(
                            scenario.getExpectationAuthorizationHeaders()
                        );
                        return true;
                    });
        }

        String taskRetrieverOption = MapValueExtractor.extract(
            scenario.getScenarioMapValues(),
            "options.taskRetrievalApi"
        );

        List<Map<String, Object>> expectations = new ArrayList<>(Objects.requireNonNull(
            MapValueExtractor.extract(values, "expectations")));

        expectations.forEach(expectationValue -> {

            int expectedTasks = MapValueExtractor.extractOrDefault(
                expectationValue, "numberOfTasksAvailable", 0);
            int expectedMessages = MapValueExtractor.extractOrDefault(
                expectationValue, "numberOfMessagesToCheck", 0);
            List<String> expectationCaseIds = CaseIdUtil.extractAllAssignedCaseIdOrDefault(expectationValue, scenario);

            verifyTasks(scenario, taskRetrieverOption, expectationValue, expectedTasks, expectationCaseIds);

            verifyMessages(expectationValue, expectedMessages, expectationCaseIds.get(0));
        });
    }

    private void verifyMessages(Map<String, Object> expectationValue, int expectedMessages, String expectationCaseId) {
        if (expectedMessages > 0) {
            await()
                .ignoreException(AssertionError.class)
                .conditionEvaluationListener(new ConditionEvaluationLogger(log::info))
                .pollInterval(DEFAULT_POLL_INTERVAL_SECONDS, SECONDS)
                .atMost(DEFAULT_TIMEOUT_SECONDS, SECONDS)
                .until(
                    () -> {
                        String actualMessageResponse = restMessageService.getCaseMessages(expectationCaseId);

                        String expectedMessageResponse = buildMessageExpectationResponseBody(
                            expectationValue,
                            Map.of("caseId", expectationCaseId)
                        );

                        Map<String, Object> actualResponse = MapSerializer.deserialize(actualMessageResponse);
                        Map<String, Object> expectedResponse = MapSerializer.deserialize(expectedMessageResponse);

                        verifiers.forEach(verifier ->
                            verifier.verify(
                                expectationValue,
                                expectedResponse,
                                actualResponse
                            )
                        );

                        return true;
                    });
        }
    }

    private void verifyTasks(TestScenario scenario, String taskRetrieverOption, Map<String, Object> expectationValue,
                             int expectedTasks, List<String> expectationCaseIds) {
        if (expectedTasks > 0) {
            String expectationCredentials = extractOrThrow(expectationValue, "credentials");
            Headers expectationAuthorizationHeaders = authorizationHeadersProvider
                .getAuthorizationHeaders(expectationCredentials);
            scenario.setExpectationAuthorizationHeaders(expectationAuthorizationHeaders);

            if (TaskRetrieverEnum.CAMUNDA_API.getId().equals(taskRetrieverOption)) {
                camundaTaskRetrievableService.retrieveTask(
                    expectationValue,
                    scenario,
                    expectationCaseIds.get(0)
                );
            } else {
                taskMgmApiRetrievableService.retrieveTask(
                    expectationValue,
                    scenario,
                    expectationCaseIds
                );
            }
        }
    }

    private String buildMessageExpectationResponseBody(Map<String, Object> clauseValues,
                                                       Map<String, String> additionalValues)
        throws IOException {

        Map<String, Object> scenario = deserializeValuesUtil.expandMapValues(clauseValues, additionalValues);
        Map<String, Object> roleData = MapValueExtractor.extract(scenario, "messageData");
        return MapSerializer.serialize(roleData);
    }


    private void loadPropertiesIntoMapValueExpander() {

        MutablePropertySources propertySources = ((AbstractEnvironment) environment).getPropertySources();
        StreamSupport
            .stream(propertySources.spliterator(), false)
            .filter(EnumerablePropertySource.class::isInstance)
            .map(propertySource -> ((EnumerablePropertySource) propertySource).getPropertyNames())
            .flatMap(Arrays::stream)
            .forEach(name -> ENVIRONMENT_PROPERTIES.setProperty(name, environment.getProperty(name)));
    }

    private void addSearchParameters(TestScenario scenario, Map<String, Object> scenarioValues) {

        List<Map<String, Object>> searchParameterObjects = new ArrayList<>();
        searchParameterObjects = extractOrDefault(scenarioValues, "searchParameters", searchParameterObjects);
        searchParameterObjects.forEach(scenario::addSearchMap);

    }
}
