package uk.gov.hmcts.reform.wapostdeploymentfttests.services.taskretriever;

import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;

import java.util.Map;

public interface TaskRetrieverService {

    void retrieveTask(Map<String, Object> clauseValues, TestScenario scenario, String caseId);
}
