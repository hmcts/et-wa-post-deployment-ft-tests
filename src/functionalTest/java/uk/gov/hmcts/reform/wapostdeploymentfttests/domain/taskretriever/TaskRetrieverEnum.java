package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.taskretriever;

import lombok.Getter;

@Getter
public enum TaskRetrieverEnum {
    CAMUNDA_API("camunda-api"),
    TASK_MGM_API("task-management-api");

    private final String id;

    TaskRetrieverEnum(String id) {
        this.id = id;
    }
}
