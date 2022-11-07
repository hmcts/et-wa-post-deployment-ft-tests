package uk.gov.hmcts.reform.wapostdeploymentfttests.verifiers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapFieldAsserter;
import uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExtractor;

import java.util.Map;

@Component
public class TaskDataVerifier implements Verifier {

    private final MapFieldAsserter mapFieldAsserter;

    @Autowired
    public TaskDataVerifier(MapFieldAsserter mapFieldAsserter) {
        this.mapFieldAsserter = mapFieldAsserter;
    }

    public void verify(
        Map<String, Object> scenario,
        Map<String, Object> expectedResponse,
        Map<String, Object> actualResponse
    ) {
        String description = MapValueExtractor.extract(scenario, "description");

        mapFieldAsserter.assertFields(expectedResponse, actualResponse, (description + ": "));
    }
}
