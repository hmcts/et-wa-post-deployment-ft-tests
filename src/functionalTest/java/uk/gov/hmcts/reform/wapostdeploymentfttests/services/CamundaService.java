package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wapostdeploymentfttests.clients.CamundaClient;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.CamundaTask;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Service
@Slf4j
public class CamundaService {

    @Autowired
    private CamundaClient camundaClient;

    public void searchByCaseIdJurisdictionAndCaseType(Map<String, Object> clauseValues,
                                                      TestScenario scenario,
                                                      String caseId) {
        int expectedTasks = MapValueExtractor.extractOrDefault(
            clauseValues, "numberOfTasksAvailable", 1);

        List<CamundaTask> response = camundaClient.getTasksByTaskVariables(
            scenario.getExpectationAuthorizationHeaders().getValue("ServiceAuthorization"),
            "caseId_eq_" + caseId
            + ",jurisdiction_eq_" + scenario.getJurisdiction()
            + ",caseTypeId_eq_" + scenario.getCaseType(),
            "created",
            "desc"
        );
        assertEquals(response.size(), expectedTasks);
    }
}
