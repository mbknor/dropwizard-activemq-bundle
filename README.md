Dropwizard ActiveMQ Bundle
==================================

Use it when you need to send and receive JSON (jackson) via ActiveMq in your Dropwizard 0.7.0 application.

Change History
-------

Current version is: **0.3.6**

Version 0.3.6

* Fixed [issue #5](https://github.com/mbknor/dropwizard-activemq-bundle/issues/5) - Use less verbose errors when 'The Consumer is closed'

Version 0.3.5

* Improved error handling for receiver + added healthCheck for each receiver
* idleTimeout is now specified in mills
* Added time to live for JMS message sending
* Added more flexible option for creating messages by passing a function

Version 0.3.4 - 20140428

* Removed resource-leakage when sending messages using multiple senders

Version 0.3.3 - 20140428

* Added more pool-config options
* HealthCheck is not using pooled factory anymore to prevent hang

Version 0.3.2 - 20140428

* Added activeMQ-HealthCheck
* Added max-give-up-time for graceful shutdown
* Added more debug logging

Version 0.3.1 - 20140411

* Added ActiveMQSenderFactory and JsonError

Version 0.3 - 20140411

* It is now possible to change between queues and topics by prefixing the destination-name

Version 0.2 - 20140410

* Added custom exception-handler

Version 0.1 - 20140410

* Initial version


Maven
----------------

Add it as a dependency:

```xml
    <dependency>
        <groupId>com.kjetland.dropwizard</groupId>
        <artifactId>dropwizard-activemq</artifactId>
        <version> INSERT LATEST VERSION HERE </version>
    </dependency>
```

Since this project is hosted on my own maven repo here on github, you also have to add this to your pom:

```xml
<repositories>
    <repository>
        <id>mbknor</id>
        <name>mbknor</name>
        <url>https://raw.githubusercontent.com/mbknor/mbknor.github.com/master/m2repo/releases</url>
    </repository>
</repositories>
```

Configuration
------------

Your config-class must implement ActiveMQConfigHolder like this:

```java
public class Config extends Configuration implements ActiveMQConfigHolder {

    @JsonProperty
    @NotNull
    @Valid
    private ActiveMQConfig activeMQ;

    public ActiveMQConfig getActiveMQ() {
        return activeMQ;
    }
}
```


And add the following to your config.yml:

```yaml

activeMQ:
  brokerUrl: tcp://localhost:61616


```

(Almost?) All config-options:

```yaml

activeMQ:
  brokerUrl: failover:(tcp://boker1.com:61616,tcp://broker2.com:61616)?randomize=false
  # shutdownWaitInSeconds: 20
  # healthCheckMillisecondsToWait: 2000
  # timeToLiveInSeconds: -1     (Default message time-to-live is off. Specify a maximum lifespan here in seconds for all messages.)
  pool:
    maxConnections: 1
    maximumActiveSessionPerConnection: 3
    blockIfSessionPoolIsFull: false
    idleTimeoutMills: 30000
    # expiryTimeoutMills:
    createConnectionOnStartup: false
    timeBetweenExpirationCheckMillis: 20000

```


Use it like this
--------------------

```java
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


        // Create a queue sender
        ActiveMQSender sender = activeMQBundle.createSender("test-queue", false);

        // or like this:
        ActiveMQSender sender2 = activeMQBundle.createSender("queue:test-queue", false);

        // where messages have a 60 second time-to-live:
        ActiveMQSender sender3 = activeMQBundle.createSender("queue:test-queue", false, Optional.of(60));

        // Create a topic-sender
        ActiveMQSender sender4 = activeMQBundle.createSender("topic:test-topic", false);

        // use it
        sender.send( someObject );
        sender.sendJson("{'a':2, 'b':3}");

        // If you require full control of message creation, pass a Java 8 function that takes a javax.jms.Session parameter:
        sender.send((Session session) -> {
            TextMessage message = session.createTextMessage();
            message.setText("{'a':2, 'b':3}");
            message.setJMSCorrelationID(myCorrelationId);
            return message;
        });


        // Create a receiver that consumes json-strings using Java 8
        activeMQBundle.registerReceiver(
                "test-queue", // default is queue. Prefix with 'topic:' or 'queue:' to choose
                (json) -> System.out.println("json: " + json),
                String.class,
                true);


        // Create a receiver that consumes SomeObject via Json using Java 8
        activeMQBundle.registerReceiver(
                            "test-queue-2",
                            (o) -> System.out.println("Value from o: " + o.getValue()),
                            SomeObject.class,
                            true);

        // Create a receiver that consumes SomeObject via Json using Java 7
        activeMQBundle.registerReceiver(
                            "test-queue-3",
                            new ActiveMQReceiver<SomeObject>() {
                                @Override
                                public void receive(SomeObject o) {
                                    System.out.println"Value from o: " + o.getValue());
                                }
                            },
                            SomeObject.class,
                            true);
    }
}
```

Topics and queue
----------------
By default all destination-names refers to *queues*.
If you want to specify topic (or queue), you prefix it with:

* topic:
* queue:


Custom exception-handling
-----------------

```java
activeMQBundle.registerReceiver(
    config.getInboundJmsQueue(),
    (ManualRequest m) -> myService.processMessage(m),
    ManualRequest.class,
    // Add your custom exception-handler here
    (message, exception) -> {
        System.out.println("Error with this message: " + message);
        exception.printStackTrace();
        return true;
    });
```

