package com.example;

import io.quarkiverse.jsonrpc.api.JsonRPCApi;
import io.smallrye.mutiny.Multi;
import java.time.Duration;

@JsonRPCApi
public class GreetingResource {

    public String hello() {
        return "Hello from Quarkus JSON-RPC";
    }
    
    public String hello(String name) {
        return "Hello " + name;
    }
    
    public Multi<String> countdown(String name) {
       return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
               .select().first(5)
               .onItem().transform(n -> "(" + (5 - n) + ") Hello " + name);
   }
    
   public Person createPerson(String name, int age) {
       return new Person(name, age);
   }

   public String greetPerson(Person person) {
       return "Hello " + person.name() + ", age " + person.age();
   } 
}
