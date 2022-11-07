package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
public class RestMessageService {

    @Value("${wa_case_event_handler.url}")
    private String caseEventHandlerUrl;

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    public void sendMessage(String message, String caseId, boolean fromDlq) {
        String messageId = randomMessageId();

        System.out.println(
            format("Attempting to inject a message into Case Event Handler using REST endpoint with "
                       + "caseId: %s, messageId: %s", caseId, messageId)
        );

        Headers systemUserUserToken = authorizationHeadersProvider.getWaSystemUserAuthorization();

        Response result = given()
            .headers(systemUserUserToken)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(message)
            .when()
            .post(caseEventHandlerUrl + "/messages/" + messageId + (fromDlq ? "?from_dlq=true" : "?from_dlq=false"));

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("MessageId", is(messageId));


        String actualResponseBody = result.then()
            .extract()
            .body().asString();

        System.out.println("Message injected successfully using Case Event Handler REST endpoint");
        System.out.println("REST response message body: " + actualResponseBody);
    }

    public String getCaseMessages(String caseId) {
        System.out.println(
            format("Attempting to retrieve messages from Case Event Handler using REST endpoint with "
                       + "caseId: %s", caseId)
        );

        Headers systemUserUserToken = authorizationHeadersProvider.getWaSystemUserAuthorization();

        Response result = given()
            .headers(systemUserUserToken)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get(caseEventHandlerUrl + "/messages/query?case_id=" + caseId);

        return result.then().extract().body().asString();
    }

    @NotNull
    private String randomMessageId() {
        return "messageId_" + ThreadLocalRandom.current().nextLong(1000000000);
    }

}
