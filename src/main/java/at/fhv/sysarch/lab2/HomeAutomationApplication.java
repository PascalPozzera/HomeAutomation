package at.fhv.sysarch.lab2;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import at.fhv.sysarch.lab2.homeautomation.HomeAutomation;
import at.fhv.sysarch.lab2.homeautomation.devices.ac.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.Fridge;
import at.fhv.sysarch.lab2.homeautomation.devices.media.MediaStation;
import at.fhv.sysarch.lab2.homeautomation.devices.weather.Blinds;
import at.fhv.sysarch.lab2.homeautomation.devices.weather.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.environment.EnvironmentSimulator;
import at.fhv.sysarch.lab2.homeautomation.environment.MqttEnvironmentSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SpringBootApplication
public class HomeAutomationApplication {

    private ActorSystem<HomeAutomation.Command> homeAutomationSystem;

    private ActorRef<HomeAutomation.Command> homeAutomationRef;

    @Bean
    @Primary
    public ActorSystem<HomeAutomation.Command> homeAutomationActorSystem() {
        homeAutomationSystem = ActorSystem.create(HomeAutomation.create(), "HomeAutomation");
        homeAutomationRef = homeAutomationSystem;

        homeAutomationRef.tell(new HomeAutomation.Start());

        return homeAutomationSystem;
    }

    @Bean
    public ActorRef<HomeAutomation.Command> homeAutomationActorRef() {
        return homeAutomationRef;
    }

    @Bean
    public ActorRef<AirCondition.AirConditionCommand> airConditionActorRef() {
        try {
            CompletionStage<akka.actor.ActorRef> future =
                    homeAutomationSystem.classicSystem()
                            .actorSelection("/user/air-condition")
                            .resolveOne(Duration.ofSeconds(5));

            akka.actor.ActorRef classicRef = future.toCompletableFuture().get(10, TimeUnit.SECONDS);
            return Adapter.toTyped(classicRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get AirCondition actor reference", e);
        }
    }

    @Bean
    public ActorRef<Blinds.BlindsCommand> blindsActorRef() {
        try {
            CompletionStage<akka.actor.ActorRef> future =
                    homeAutomationSystem.classicSystem()
                            .actorSelection("/user/blinds")
                            .resolveOne(Duration.ofSeconds(5));

            akka.actor.ActorRef classicRef = future.toCompletableFuture().get(10, TimeUnit.SECONDS);
            return Adapter.toTyped(classicRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Blinds actor reference", e);
        }
    }

    @Bean
    public ActorRef<WeatherSensor.WeatherCommand> weatherSensorActorRef() {
        try {
            CompletionStage<akka.actor.ActorRef> future =
                    homeAutomationSystem.classicSystem()
                            .actorSelection("/user/weather-sensor")
                            .resolveOne(Duration.ofSeconds(5));

            akka.actor.ActorRef classicRef = future.toCompletableFuture().get(10, TimeUnit.SECONDS);
            return Adapter.toTyped(classicRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get WeatherSensor actor reference", e);
        }
    }

    @Bean
    public ActorRef<MediaStation.MediaCommand> mediaStationActorRef() {
        try {
            CompletionStage<akka.actor.ActorRef> future =
                    homeAutomationSystem.classicSystem()
                            .actorSelection("/user/media-station")
                            .resolveOne(Duration.ofSeconds(5));

            akka.actor.ActorRef classicRef = future.toCompletableFuture().get(10, TimeUnit.SECONDS);
            return Adapter.toTyped(classicRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get MediaStation actor reference", e);
        }
    }

    @Bean
    public ActorRef<Fridge.FridgeCommand> fridgeActorRef() {
        try {
            CompletionStage<akka.actor.ActorRef> future =
                    homeAutomationSystem.classicSystem()
                            .actorSelection("/user/fridge")
                            .resolveOne(Duration.ofSeconds(5));

            akka.actor.ActorRef classicRef = future.toCompletableFuture().get(10, TimeUnit.SECONDS);
            return Adapter.toTyped(classicRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Fridge actor reference", e);
        }
    }

    @PreDestroy
    public void terminateActorSystem() {
        if (homeAutomationSystem != null) {
            homeAutomationSystem.terminate();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(HomeAutomationApplication.class, args);
    }
}