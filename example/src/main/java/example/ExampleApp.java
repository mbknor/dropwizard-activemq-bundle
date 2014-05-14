package example;

import com.kjetland.dropwizard.activemq.ActiveMQBundle;
import com.kjetland.dropwizard.activemq.ActiveMQSender;
import example.data.Animal;
import example.resources.AnimalResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExampleApp extends Application<Config> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) throws Exception {
        new ExampleApp().run(args);
    }

    private ActiveMQBundle activeMQBundle;

    @Override
    public void initialize(Bootstrap<Config> configBootstrap) {

        // Create the bundle and store reference to it
        this.activeMQBundle = new ActiveMQBundle();
        // Add the bundle
        configBootstrap.addBundle(activeMQBundle);
    }

    @Override
    public void run(Config config, Environment environment) throws Exception {

        final String queueName = config.getQueueName();

        // Set up the sender for our queue
        ActiveMQSender sender = activeMQBundle.createSender( queueName, false);

        // Set up a receiver that just logs the messages we receive on our queue
        activeMQBundle.registerReceiver(
                queueName,
                (animal) -> log.info("\n*****\nWe received an animal from activeMq: \n{}\n*****", animal),
                Animal.class,
                true);

        // Create our resource
        environment.jersey().register( new AnimalResource(sender) );

    }

}
