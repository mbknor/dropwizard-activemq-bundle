Example Dropwizard Application using ActiveMQ
=============================================


When started, this application will **connect to an ActiveMQ-broker** running on **localhost**.
It will register one **REST-resource** receiving **animals** which it will send to the
ActiveMQ-queue *animalQueue* as **JSON**.

The same application will also **listen to this queue**, and when it receives *animals* on it, it will **log it**.


How to run it
-----------------------

Start a local ActiveMQ-broker

Build and run the sample like this:

    mvn package exec:java


The app should now be running and you can check its healthchecks:

    http://localhost:8081/healthcheck

Use this url to send an animal:

    http://localhost:8080/animal?type=dog&name=Nemi&age=7

You will then see that the animal is received from the browser, sent to ActiveMQ and received again like this:


    INFO  [2014-05-14 19:28:33,099] com.kjetland.dropwizard.activemq.ActiveMQSenderImpl: Sending to animalQueue: {"type":"dog","name":"Nemi","age":7}
    INFO  [2014-05-14 19:28:33,115] com.kjetland.dropwizard.activemq.ActiveMQReceiverHandler: Received {"type":"dog","name":"Nemi","age":7}
    INFO  [2014-05-14 19:28:33,120] example.ExampleApp:
    *****
    We received an animal from activeMq:
    Animal{type='dog', name='Nemi', age=7}
    *****

That's it..