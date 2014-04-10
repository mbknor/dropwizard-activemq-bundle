Dropwizard ActiveMQ Bundle
==================================

Use it when you need to send and receive JSON (jackson) via ActiveMq in your Dropwizard 0.7.0 application.


Maven
----------------

Currently this bundle is not deployed to any repository (This will change),
so you have to build it yourself.

    git clone git@github.com:mbknor/dropwizard-activemq-bundle.git
    cd dropwizard-activemq-bundl
    maven clean install


Now you can add it as a dependency:

    <dependency>
        <groupId>com.kjetland.dropwizard</groupId>
        <artifactId>dropwizard-activemq</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>



Configuration
------------

Your config-class must implement ActiveMQConfigHolder like this:

    public class Config extends Configuration implements ActiveMQConfigHolder {

        @JsonProperty
        @NotNull
        @Valid
        private ActiveMQConfig activeMQ;

        public ActiveMQConfig getActiveMQ() {
            return activeMQ;
        }
    }


Use it like this
--------------------

    public class ActiveMQApp extends Application<Config> {

        public static void main(String[] args) throws Exception {
            new ActiveMQApp().run(args);
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


            // Create a sender
            ActiveMQSender sender = activeMQBundle.createSender("test-queue", false);

            // use it
            sender.send( someObject );
            sender.sendJson("{'a':2, 'b':3}");



            // Create a receiver that consumes json-strings
            activeMQBundle.registerReceiver(
                    "test-queue",
                    (json) -> System.out.println("json: " + json),
                    String.class,
                    true);


            // Create a receiver that consumes SomeObject via json
            activeMQBundle.registerReceiver(
                                "test-queue-2",
                                (o) -> System.out.println("Value from o: " + o.getValue()),
                                SomeObject.class,
                                true);
        }
    }




