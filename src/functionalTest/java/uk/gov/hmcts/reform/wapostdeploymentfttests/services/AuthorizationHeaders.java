package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Headers;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.CredentialRequest;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;

import java.io.IOException;

public interface AuthorizationHeaders {
    Headers getWaSystemUserAuthorization();

    Headers getWaUserAuthorization(CredentialRequest request) throws IOException;

    UserInfo getUserInfo(String userToken);

    void cleanupTestUsers();
}
