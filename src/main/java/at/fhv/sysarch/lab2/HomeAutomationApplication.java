package at.fhv.sysarch.lab2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import at.fhv.sysarch.lab2.homeautomation.devices.ac.AirCondition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HomeAutomationApplication {

    //todo: create common ActorSystem with several spawn to ensure that only one actor system is running
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
