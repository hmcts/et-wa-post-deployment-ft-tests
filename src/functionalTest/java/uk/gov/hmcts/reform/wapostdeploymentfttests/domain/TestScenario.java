package uk.gov.hmcts.reform.wapostdeploymentfttests.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

public class TestScenario {

    private final Map<String, Object> scenarioMapValues;
    private final String scenarioSource;
    private final Map<String, Object> beforeClauseValues;
    private final List<Map<String, Object>> testClauseValues;
    private final Map<String, Object> postRoleAssignmentClauseValues;
    private final Map<String, Object> updateCaseClauseValues;
    private final String jurisdiction;
    private final String caseType;

    private final List<String> taskIds;
    private String assigneeId;
    private final Map<String, String> caseIdMap;
    private final Set<Map<String, Object>> searchMap;

    //private Headers requestAuthorizationHeaders
    //private Headers expectationAuthorizationHeaders

    public TestScenario(@NotNull Map<String, Object> scenarioMapValues,
                        @NotNull String scenarioSource,
                        @NotNull String jurisdiction,
                        @NotNull String caseType,
                        @Nullable Map<String, Object> beforeClauseValues,
                        @NotNull List<Map<String, Object>> testClauseValues,
                        @Nullable Map<String, Object> postRoleAssignmentClauseValues,
                        @Nullable Map<String, Object> updateCaseClauseValues) {
        this.scenarioMapValues = scenarioMapValues;
        this.scenarioSource = scenarioSource;
        this.jurisdiction = jurisdiction;
        this.caseType = caseType;
        this.beforeClauseValues = beforeClauseValues;
        this.testClauseValues = testClauseValues;
        this.postRoleAssignmentClauseValues = postRoleAssignmentClauseValues;
        this.updateCaseClauseValues = updateCaseClauseValues;
        this.caseIdMap = new HashMap<>();
        this.searchMap = new HashSet<>();
        this.taskIds = new ArrayList<>();
    }

    public Map<String, Object> getScenarioMapValues() {
        return scenarioMapValues;
    }

    @NotNull
    public String getScenarioSource() {
        return scenarioSource;
    }


    public void addAssignedCaseId(String key, String caseId) {
        caseIdMap.put(key, caseId);
    }

    public String getAssignedCaseId(String key) {
        return caseIdMap.get(key);
    }

    public Map<String, String> getAssignedCaseIdMap() {
        return caseIdMap;
    }

    public String getCaseType() {
        return caseType;
    }

    public Map<String, Object> getBeforeClauseValues() {
        return beforeClauseValues;
    }

    public List<Map<String, Object>> getTestClauseValues() {
        return testClauseValues;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public Map<String, Object> getPostRoleAssignmentClauseValues() {
        return postRoleAssignmentClauseValues;
    }

    public void addSearchMap(Map<String, Object> map) {
        searchMap.add(map);
    }

    public Set<Map<String, Object>> getSearchMap() {
        return searchMap;
    }

    public Map<String, Object> getUpdateCaseClauseValues() {
        return updateCaseClauseValues;
    }

    public void addTaskId(String taskId) {
        assertNotNull(taskId);
        taskIds.add(taskId);
    }

    public List<String> getTaskIds() {
        return taskIds;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    /*
    public Headers getRequestAuthorizationHeaders() {
        return requestAuthorizationHeaders;
    }

    public void setRequestAuthorizationHeaders(Headers requestAuthorizationHeaders) {
        this.requestAuthorizationHeaders = requestAuthorizationHeaders;
    }

    public Headers getExpectationAuthorizationHeaders() {
        return expectationAuthorizationHeaders;
    }

    public void setExpectationAuthorizationHeaders(Headers expectationAuthorizationHeaders) {
        this.expectationAuthorizationHeaders = expectationAuthorizationHeaders;
    }*/
}
