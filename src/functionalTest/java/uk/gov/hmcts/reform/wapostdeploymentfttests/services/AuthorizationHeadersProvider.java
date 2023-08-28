package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wapostdeploymentfttests.clients.IdamWebApi;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.CredentialRequest;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.RoleCode;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthorizationHeadersProvider  implements AuthorizationHeaders {

    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private static final String WA_USER_PASSWORD = "System01";

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, UserInfo> userInfo = new ConcurrentHashMap<>();
    private final Map<String, String> testUserAccounts = new ConcurrentHashMap<>();
    @Value("${idam.redirectUrl}")
    protected String idamRedirectUrl;
    @Value("${idam.scope}")
    protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}")
    protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}")
    protected String idamClientSecret;
    @Value("${idam.test.userCleanupEnabled:false}")
    private boolean testUserDeletionEnabled;

    @Autowired
    private IdamWebApi idamWebApi;

    @Autowired
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @Autowired
    private RoleAssignmentService roleAssignmentService;

    @Override
    public Headers getWaSystemUserAuthorization() {
        return new Headers(
            getUserAuthorizationHeader(
                "WaSystemUser",
                System.getenv("WA_SYSTEM_USERNAME"),
                System.getenv("WA_SYSTEM_PASSWORD")
            ),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getWaCaseOfficerAuthorization() {
        return new Headers(
            getUserAuthorizationHeader(
                "WaCaseOfficer",
                System.getenv("WA_CASEOFFICER_USERNAME"),
                System.getenv("WA_CASEOFFICER_PASSWORD")
            ),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getEtCaseWorkerAuthorization() {
        return new Headers(
            getUserAuthorizationHeader(
                "ETCaseWorker",
                System.getenv("ET_CASEOFFICER_USERNAME"),
                System.getenv("ET_CASEOFFICER_PASSWORD")
            ),
            getServiceAuthorizationHeader()
        );
    }

    @Override
    public Headers getWaUserAuthorization(CredentialRequest request) throws IOException {
        if ("WaSystemUser".equals(request.getCredentialsKey())) {
            return getWaSystemUserAuthorization();
        } else if ("WaCaseOfficer".equals(request.getCredentialsKey())) {
            return getWaCaseOfficerAuthorization();
        } else if ("ETCaseWorker".equals(request.getCredentialsKey())) {
            return getEtCaseWorkerAuthorization();
        } else {
            String userEmail = findOrGenerateUserAccount(request.getCredentialsKey(), request.isGranularPermission());

            return new Headers(
                getUserAuthorizationHeader(request.getCredentialsKey(), userEmail),
                getServiceAuthorizationHeader()
            );
        }
    }

    @Override
    public UserInfo getUserInfo(String userToken) {
        return userInfo.computeIfAbsent(
            userToken,
            user -> idamWebApi.userInfo(userToken)
        );
    }

    @Override
    public void cleanupTestUsers() {
        testUserAccounts.entrySet()
            .forEach(entry -> {
                Headers headers =  new Headers(
                    getUserAuthorizationHeader(entry.getKey(), entry.getValue()),
                    getServiceAuthorizationHeader()
                );
                UserInfo userInfo = getUserInfo(headers.getValue(AUTHORIZATION));
                roleAssignmentService.clearAllRoleAssignments(headers, userInfo);
                deleteAccount(entry.getValue());
            });
    }

    private void deleteAccount(String username) {
        if (testUserDeletionEnabled) {
            log.info("Deleting test account '{}'", username);
            idamWebApi.deleteTestUser(username);
        } else {
            log.info("Test User deletion feature flag was not enabled, user '{}' was not deleted", username);
        }
    }

    public Header getServiceAuthorizationHeader() {
        String serviceToken = tokens.computeIfAbsent(
            SERVICE_AUTHORIZATION,
            user -> serviceAuthTokenGenerator.generate()
        );

        return new Header(SERVICE_AUTHORIZATION, serviceToken);
    }

    private Header getUserAuthorizationHeader(String key, String username) {
        return getUserAuthorizationHeader(key, username, WA_USER_PASSWORD);
    }

    private Header getUserAuthorizationHeader(String key, String username, String password) {
        MultiValueMap<String, String> body = createIdamRequest(username, password);

        String accessToken = tokens.computeIfAbsent(
            key,
            user -> "Bearer " + idamWebApi.token(body).getAccessToken()
        );
        return new Header(AUTHORIZATION, accessToken);
    }

    private MultiValueMap<String, String> createIdamRequest(String username, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("redirect_uri", idamRedirectUrl);
        body.add("client_id", idamClientId);
        body.add("client_secret", idamClientSecret);
        body.add("username", username);
        body.add("password", password);
        body.add("scope", userScope);

        return body;
    }

    private String findOrGenerateUserAccount(String credentialsKey, boolean granularPermission) throws IOException {
        return testUserAccounts.computeIfAbsent(
            credentialsKey,
            user -> generateUserAccount(credentialsKey, granularPermission)
        );
    }

    private String generateUserAccount(String credentialsKey, boolean granularPermission) {
        String emailPrefix = granularPermission ? "wa-granular-permission-pdt" : "wa-pdt-";
        String userEmail = emailPrefix + UUID.randomUUID() + "@fake.hmcts.net";

        List<RoleCode> requiredRoles = new ArrayList<>(List.of(
            new RoleCode("caseworker"),
            new RoleCode("caseworker-employment"),
            new RoleCode("caseworker-employment-englandwales"),
            new RoleCode("caseworker-employment-scotland"),
            new RoleCode("caseworker-wa"),
            new RoleCode("caseworker-wa-task-configuration")
        ));

        log.info("Attempting to create a new test account {}", userEmail);

        Map<String, Object> body = requestBody(userEmail, requiredRoles);

        idamWebApi.createTestUser(body);

        log.info("Test account created successfully");

        List<String> roleAssignments = findRoleAssignmentRequirements(credentialsKey);
        log.info("Assigning role assignments {}", roleAssignments);

        for (String r : roleAssignments) {
            assignRoleAssignment(userEmail, credentialsKey, r);
        }

        return userEmail;
    }

    @NotNull
    private Map<String, Object> requestBody(String userEmail, List<RoleCode> requiredRoles) {
        RoleCode userGroup = new RoleCode("caseworker");

        Map<String, Object> body = new ConcurrentHashMap<>();
        body.put("email", userEmail);
        body.put("password", WA_USER_PASSWORD);
        body.put("forename", "WAPDTAccount");
        body.put("surname", "Functional");
        body.put("roles", requiredRoles);
        body.put("userGroup", userGroup);
        return body;
    }

    private List<String> findRoleAssignmentRequirements(String credentialsKey) {
        List<String> roleAssignments = new ArrayList<>();
        switch (credentialsKey) {
            case "WaCaseOfficer":
                roleAssignments.add("case-allocator");
                roleAssignments.add("task-supervisor");
                roleAssignments.add("tribunal-caseworker");
                break;
            case "WaTribunalCaseOfficer":
                roleAssignments.add("case-allocator");
                roleAssignments.add("hearing-centre-admin");
                break;
            case "WaSeniorTribunalCaseOfficer":
                roleAssignments.add("senior-tribunal-caseworker");
                break;
            case "WaTaskSupervisor":
                roleAssignments.add("task-supervisor");
                break;
            default:
                throw new IllegalStateException("Credentials implementation for '" + credentialsKey + "' not found");
        }
        return roleAssignments;
    }

    private void assignRoleAssignment(String userEmail, String credentialsKey, String roleName) {
        Headers authenticationHeaders = new Headers(
            getUserAuthorizationHeader(credentialsKey, userEmail),
            getServiceAuthorizationHeader()
        );
        UserInfo userInfo = getUserInfo(authenticationHeaders.getValue(AUTHORIZATION));
        roleAssignmentService.setupRoleAssignment(authenticationHeaders, userInfo, roleName);
    }
}
