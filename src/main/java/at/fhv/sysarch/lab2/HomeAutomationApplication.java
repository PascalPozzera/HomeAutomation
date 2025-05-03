package at.fhv.sysarch.lab2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import at.fhv.sysarch.lab2.homeautomation.devices.ac.AirCondition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HomeAutomationApplication {

    @Bean
    public ActorRef<AirCondition.AirConditionCommand> airConditionActorRef() {
        ActorSystem<AirCondition.AirConditionCommand> system =
                ActorSystem.create(AirCondition.create("ac-1"), "HomeAutomation");
        return system;
    }

    public static void main(String[] args) {
        SpringApplication.run(HomeAutomationApplication.class, args);
    }
}
