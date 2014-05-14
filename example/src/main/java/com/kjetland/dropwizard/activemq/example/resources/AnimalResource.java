package com.kjetland.dropwizard.activemq.example.resources;

import com.kjetland.dropwizard.activemq.ActiveMQSender;
import com.kjetland.dropwizard.activemq.example.data.Animal;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("animal")
public class AnimalResource {

    private final ActiveMQSender sender;

    public AnimalResource(ActiveMQSender sender) {
        this.sender = sender;
    }

    @GET
    public String sendAnimal(
            @QueryParam("type") String type,
            @QueryParam("name") String name,
            @QueryParam("age") int age) {

        if ( type == null && name == null && age == 0) {
            return "You must specify type, name and age as queryParams";
        }

        // Create our animal-object
        Animal animal = new Animal(type, name, age);

        // Send it via activeMq
        sender.send(animal);

        return "Animal " + animal + " sent via ActiveMQ";
    }
}
