package example.resources;

import com.kjetland.dropwizard.activemq.ActiveMQSender;
import example.data.Message;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.HashMap;
import java.util.Map;

@Path("message")
public class MultiQResource {
    private Map<String, ActiveMQSender> sender = new HashMap<>(5);


    @GET
    public String sendMessage(
        @QueryParam("qName") String qName,
        @QueryParam("message") String message) {

        if ( qName == null || message == null) {
            return "You must specify qName and message as queryParams";
        }

        // Create our animal-object
        Message message1 = new Message(qName, message);

        // Send it via activeMq
        sender.getOrDefault(qName, sender.values().iterator().next()).send(message1);

        return String.format("Message: %s sent via ActiveMQ", message1);
    }

    public void addSender(String qName, ActiveMQSender activeMQSender){
        sender.put(qName, activeMQSender);
    }
}
