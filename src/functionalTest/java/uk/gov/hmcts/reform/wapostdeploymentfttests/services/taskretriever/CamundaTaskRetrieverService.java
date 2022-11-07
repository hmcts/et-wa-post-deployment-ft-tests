package uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionEvaluationLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.CamundaService;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.DeserializeValuesUtil;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_POLL_INTERVAL_SECONDS;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.SpringBootFunctionalBaseTest.DEFAULT_TIMEOUT_SECONDS;

@Component
@Slf4j
public class CamundaTaskRetrieverService implements TaskRetrieverService {

    @Autowired
    private CamundaService camundaService;
    @Autowired
    private DeserializeValuesUtil deserializeValuesUtil;

    @Override
    public void retrieveTask(Map<String, Object> clauseValues, TestScenario scenario, String caseId) {

        Map<String, Object> deserializedClauseValues = deserializeValuesUtil.expandMapValues(clauseValues, emptyMap());

        await()
            .ignoreException(AssertionError.class)
            .conditionEvaluationListener(new ConditionEvaluationLogger(log::info))
            .pollInterval(DEFAULT_POLL_INTERVAL_SECONDS, SECONDS)
            .atMost(DEFAULT_TIMEOUT_SECONDS, SECONDS)
            .until(
                () -> {
                    camundaService.searchByCaseIdJurisdictionAndCaseType(deserializedClauseValues, scenario, caseId);
                    // TODO: Do we want to verify responses for camunda?
                    return true;
                });

    }
}
