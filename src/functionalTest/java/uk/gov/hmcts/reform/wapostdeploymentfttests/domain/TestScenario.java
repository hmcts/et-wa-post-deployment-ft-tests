package uk.gov.hmcts.reform.wapostdeploymentfttests.domain;

import io.restassured.http.Headers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestScenario {

    private final Map<String, Object> scenarioMapValues;
    private final String scenarioSource;
    private final Map<String, Object> beforeClauseValues;
    private final Map<String, Object> testClauseValues;
    private final Map<String, Object> postRoleAssignmentClauseValues;
    private final String jurisdiction;
    private final String caseType;

    private final Map<String, String> caseIdMap;
    private final Set<Map<String, Object>> searchMap;
    private Headers requestAuthorizationHeaders;
    private Headers expectationAuthorizationHeaders;

    public TestScenario(@NotNull Map<String, Object> scenarioMapValues,
                        @NotNull String scenarioSource,
                        @NotNull String jurisdiction,
                        @NotNull String caseType,
                        @Nullable Map<String, Object> beforeClauseValues,
                        @NotNull Map<String, Object> testClauseValues,
                        @Nullable Map<String, Object> postRoleAssignmentClauseValues) {
        this.scenarioMapValues = scenarioMapValues;
        this.scenarioSource = scenarioSource;
        this.jurisdiction = jurisdiction;
        this.caseType = caseType;
        this.beforeClauseValues = beforeClauseValues;
        this.testClauseValues = testClauseValues;
        this.postRoleAssignmentClauseValues = postRoleAssignmentClauseValues;
        this.caseIdMap = new HashMap<>();
        this.searchMap = new HashSet<>();
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
    }

    public Map<String, Object> getBeforeClauseValues() {
        return beforeClauseValues;
    }

    public Map<String, Object> getTestClauseValues() {
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

}
