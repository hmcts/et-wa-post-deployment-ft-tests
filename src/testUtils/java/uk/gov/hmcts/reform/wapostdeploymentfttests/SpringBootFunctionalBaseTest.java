package uk.gov.hmcts.reform.wapostdeploymentfttests;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
public abstract class SpringBootFunctionalBaseTest {

    public static final int DEFAULT_TIMEOUT_SECONDS = 300;
    public static final int DEFAULT_POLL_INTERVAL_SECONDS = 4;
    @Autowired
    protected ObjectMapper objectMapper;

}
