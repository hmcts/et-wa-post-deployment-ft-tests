package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.TestScenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor.extract;

public class CaseIdUtil {

    private CaseIdUtil() {
        //private constructor
    }

    private static final String ASSIGNED_CASE_ID_KEY_FIELD = "caseIdKey";
    private static final String DEFAULT_ASSIGNED_CASE_ID_KEY = "defaultCaseId";

    public static String extractAssignedCaseIdOrDefault(Map<String, Object> values, TestScenario scenario) {

        String assignedCaseIdKey = extract(values, ASSIGNED_CASE_ID_KEY_FIELD);

        if (StringUtils.isNotEmpty(assignedCaseIdKey)) {
            String assignedCaseId = scenario.getAssignedCaseId(assignedCaseIdKey);
            if (StringUtils.isNotEmpty(assignedCaseId)) {
                return assignedCaseId;
            } else {
                throw new IllegalStateException("Case Id not found for '" + assignedCaseIdKey + "'");
            }
        }

        return scenario.getAssignedCaseId(DEFAULT_ASSIGNED_CASE_ID_KEY);
    }

    public static List<String> extractAllAssignedCaseIdOrDefault(Map<String, Object> values, TestScenario scenario) {

        String assignedCaseIdKey = extract(values, ASSIGNED_CASE_ID_KEY_FIELD);

        if (StringUtils.isNotEmpty(assignedCaseIdKey)) {
            if (assignedCaseIdKey.contains(",")) {
                String[] keys = assignedCaseIdKey.split(",");
                List<String> caseIds = new ArrayList<>();
                for (String key : keys) {
                    caseIds.add(scenario.getAssignedCaseId(key));
                }
                return caseIds;
            } else {
                String assignedCaseId = scenario.getAssignedCaseId(assignedCaseIdKey);
                if (StringUtils.isNotEmpty(assignedCaseId)) {
                    return Collections.singletonList(assignedCaseId);
                } else {
                    throw new IllegalStateException("Case Id not found for '" + assignedCaseIdKey + "'");
                }
            }
        }

        return Collections.singletonList(scenario.getAssignedCaseId(DEFAULT_ASSIGNED_CASE_ID_KEY));
    }

    public static void addAssignedCaseId(Map<String, Object> values, String caseId, TestScenario scenario) {
        String assignedCaseIdKey = extract(values, ASSIGNED_CASE_ID_KEY_FIELD);
        if (StringUtils.isNotEmpty(assignedCaseIdKey)) {
            scenario.addAssignedCaseId(assignedCaseIdKey, caseId);
        } else {
            scenario.addAssignedCaseId(DEFAULT_ASSIGNED_CASE_ID_KEY, caseId);
        }
    }
}
