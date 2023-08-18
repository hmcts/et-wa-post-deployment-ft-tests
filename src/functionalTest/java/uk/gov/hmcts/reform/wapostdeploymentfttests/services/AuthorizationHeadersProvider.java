package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wapostdeploymentfttests.clients.IdamWebApi;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthorizationHeadersProvider  implements AuthorizationHeaders {

    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, UserInfo> userInfo = new ConcurrentHashMap<>();
    @Value("${idam.redirectUrl}")
    protected String idamRedirectUrl;
    @Value("${idam.scope}")
    protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}")
    protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}")
    protected String idamClientSecret;

    @Autowired
    private IdamWebApi idamWebApi;

    @Autowired
    private AuthTokenGenerator serviceAuthTokenGenerator;

    public Header getServiceAuthorizationHeader() {
        String serviceToken = tokens.computeIfAbsent(
            SERVICE_AUTHORIZATION,
            user -> serviceAuthTokenGenerator.generate()
        );

        return new Header(SERVICE_AUTHORIZATION, serviceToken);
    }

    public Headers getTribunalCaseworkerAAuthorization() {
        return new Headers(
            getCaseworkerAAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getWaSystemUserAuthorization() {
        return new Headers(
            getUserAuthorizationOnly(
                "WA_SYSTEM_USERNAME",
                "WA_SYSTEM_PASSWORD",
                "WaSystemUser"
            ),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getWaCaseOfficerAuthorization() {
        return new Headers(
            getUserAuthorizationOnly(
                "WA_CASEOFFICER_USERNAME",
                "WA_CASEOFFICER_PASSWORD",
                "WaCaseOfficer"
            ),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getEtCaseWorkerAuthorization() {
        return new Headers(
            getUserAuthorizationOnly(
                "ET_CASEOFFICER_USERNAME",
                "ET_CASEOFFICER_PASSWORD",
                "ETCaseWorker"
            ),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getLegalRepAuthorization() {
        Header requiredHeader = getLawFirmAuthorizationOnly();

        return new Headers(
            requiredHeader,
            getServiceAuthorizationHeader()
        );
    }

    public Header getCaseworkerAAuthorizationOnly() {

        return getUserAuthorizationOnly("TEST_WA_CASEOFFICER_PUBLIC_A_USERNAME",
                                        "TEST_WA_CASEOFFICER_PUBLIC_A_PASSWORD",
                                        "Caseworker A");
    }

    public Header getUserAuthorizationOnly(String username, String password, String key) {
        return getAuthorization(key, System.getenv(username), System.getenv(password));
    }

    public Header getLawFirmAuthorizationOnly() {
        return getUserAuthorizationOnly(
            "TEST_WA_LAW_FIRM_USERNAME",
            "TEST_WA_LAW_FIRM_PASSWORD",
            "LawFirm"
        );
    }

    public Header getWaDlqSystemUserAuthorization() {
        return getUserAuthorizationOnly(
            "TEST_WA_DLQ_PROCESS_USERNAME",
            "TEST_WA_DLQ_PROCESS_PASSWORD",
            "WaDlqSystemUser"
        );
    }

    private Header getAuthorization(String key, String username, String password) {

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

    @Override
    public UserInfo getUserInfo(String userToken) {
        return userInfo.computeIfAbsent(
            userToken,
            user -> idamWebApi.userInfo(userToken)
        );

    }

    @Override
    public Headers getAuthorizationHeaders(String credentials) {
        switch (credentials) {
            case "IALegalRepresentative":
                return getLegalRepAuthorization();
            case "IACaseworker":
                return getTribunalCaseworkerAAuthorization();
            case "WaSystemUser":
                return getWaSystemUserAuthorization();
            case "WaCaseOfficer":
                return getWaCaseOfficerAuthorization();
            case "ETCaseWorker":
                return getEtCaseWorkerAuthorization();
            default:
                throw new IllegalStateException("Credentials implementation for '" + credentials + "' not found");
        }

    }
}
