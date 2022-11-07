package uk.gov.hmcts.reform.wapostdeploymentfttests.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "role-assignment-api",
    url = "${role-assignment-service.url}"

)
@SuppressWarnings("checkstyle:LineLength")
public interface RoleAssignmentServiceApiClient {

    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @DeleteMapping(
        value = "/am/role-assignments/{role-assignment-id}",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void deleteRoleAssignmentById(@PathVariable("role-assignment-id") String roleAssignmentId,
                                  @RequestHeader(AUTHORIZATION) String userToken,
                                  @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken);

    @PostMapping(
        value = "/am/role-assignments",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void createRoleAssignment(@RequestBody String body,
                              @RequestHeader(AUTHORIZATION) String userToken,
                              @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken);

}
