Dropwizard ActiveMQ Bundle
==================================

Use it when you need to send and receive JSON (jackson) via ActiveMq in your Dropwizard 0.7.0 application.


Maven
----------------


Current version is: **0.1**


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


        // Create a sender
        ActiveMQSender sender = activeMQBundle.createSender("test-queue", false);

        // use it
        sender.send( someObject );
        sender.sendJson("{'a':2, 'b':3}");



        // Create a receiver that consumes json-strings using Java 8
        activeMQBundle.registerReceiver(
                "test-queue",
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




