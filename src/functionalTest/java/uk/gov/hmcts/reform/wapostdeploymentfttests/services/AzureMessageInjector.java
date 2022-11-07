package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
public class AzureMessageInjector {

    @Autowired
    private ServiceBusSenderClient senderClient;

    @Value("${azure.servicebus.message-author}")
    private String messageAuthor;

    public void sendMessage(String message, String caseId, String jurisdictionId) {
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(message);

        serviceBusMessage.getApplicationProperties().put("message_context", "wa-ft-" + caseId);
        serviceBusMessage.getApplicationProperties().put("message_author", messageAuthor);
        serviceBusMessage.getApplicationProperties().put("jurisdiction_id", jurisdictionId);
        serviceBusMessage.setSessionId(caseId);

        System.out.println(
            format("Attempting to inject a message into azure service bus to session with ID: %s", caseId)
        );
        senderClient.sendMessage(serviceBusMessage);
        System.out.println("Message injected successfully into Azure Service Bus");
    }

}
