package uk.gov.hmcts.reform.wapostdeploymentfttests.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureServiceBusConfiguration {

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;
    @Value("${azure.servicebus.topic}")
    private String topic;

    @Bean
    public ServiceBusSenderClient createConnection() {

        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .topicName(topic)
            .buildClient();
    }

}
