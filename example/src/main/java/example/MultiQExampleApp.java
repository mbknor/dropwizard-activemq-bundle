package example;

import com.kjetland.dropwizard.activemq.ActiveMQBundle;
import com.kjetland.dropwizard.activemq.ActiveMQMultiBundle;
import example.data.Message;
import example.resources.MultiQResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class MultiQExampleApp extends Application<MultiQConfig> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) throws Exception {
        new MultiQExampleApp().run(args);
    }

    private ActiveMQMultiBundle activeMQBundle;

    @Override
    public void initialize(Bootstrap<MultiQConfig> configBootstrap) {

        // Create the bundle and store reference to it
        this.activeMQBundle = new ActiveMQMultiBundle();
        // Add the bundle
        configBootstrap.addBundle(activeMQBundle);
    }

    @Override
    public void run(MultiQConfig config, Environment environment) throws Exception {
        MultiQResource multiQResource = new MultiQResource();
        environment.jersey().register(multiQResource);
        activeMQBundle.getActiveMQBundleMap().entrySet()
            .stream()
            .forEach(activeMQEntry->activateQueue(multiQResource, activeMQEntry));

    }

    private void activateQueue(MultiQResource multiQResource, Map.Entry<String, ActiveMQBundle> activeMQEntry) {
        String queueName = activeMQEntry.getKey();
        ActiveMQBundle activeMQBundle = activeMQEntry.getValue();

        // Set up the sender for our queue
        multiQResource.addSender(queueName, activeMQBundle.createSender( queueName, false));

        // Set up a receiver that just logs the messages we receive on our queue
        activeMQBundle.registerReceiver(
            queueName,
            (message) -> log.info("\n*****\nWe received a message on: {} from activeMq: \n{}\n*****", queueName, message),
            Message.class,
            true);
    }

}
