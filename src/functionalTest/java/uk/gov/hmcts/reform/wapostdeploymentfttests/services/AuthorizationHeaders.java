package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Headers;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;

public interface AuthorizationHeaders {
    Headers getAuthorizationHeaders(String credentials);

    UserInfo getUserInfo(String userToken);
}
