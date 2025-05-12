package at.fhv.sysarch.lab2.homeautomation.devices.ac;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.environment.EnvironmentSimulator;

import java.time.Duration;

public class TemperatureSensor extends AbstractBehavior<TemperatureSensor.TemperatureCommand> {

    public interface TemperatureCommand {}

    public static final class ReadTemperature implements TemperatureCommand {
        final Double value;

        public ReadTemperature(Double value) {
            this.value = value;
        }
    }

    public static final class SwitchMode implements TemperatureCommand {
        final boolean simulate;

        public SwitchMode(boolean simulate) {
            this.simulate = simulate;
        }
    }

    public static final class Tick implements TemperatureCommand {}

    public static final class EnvironmentTemperatureUpdate implements TemperatureCommand {
        final double temperature;

        public EnvironmentTemperatureUpdate(double temperature) {
            this.temperature = temperature;
        }

        public double getTemperature() {
            return temperature;
        }
    }

    public static Behavior<TemperatureCommand> create(ActorRef<AirCondition.AirConditionCommand> airCondition) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers ->
                new TemperatureSensor(context, airCondition, timers)
        ));
    }

    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final TimerScheduler<TemperatureCommand> timers;
    private boolean simulateMode = false;

    private TemperatureSensor(ActorContext<TemperatureCommand> context,
                              ActorRef<AirCondition.AirConditionCommand> airCondition,
                              TimerScheduler<TemperatureCommand> timers) {
        super(context);
        this.airCondition = airCondition;
        this.timers = timers;

        getContext().getLog().info("TemperatureSensor started in manual mode");
    }

    @Override
    public Receive<TemperatureCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadTemperature.class, this::onReadTemperature)
                .onMessage(SwitchMode.class, this::onSwitchMode)
                .onMessage(Tick.class, this::onTick)
                .onMessage(EnvironmentTemperatureUpdate.class, this::onEnvironmentTemperatureUpdate)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<TemperatureCommand> onReadTemperature(ReadTemperature r) {
        if (!simulateMode) {
            getContext().getLog().info("Manual temperature input: {}", r.value);
            airCondition.tell(new AirCondition.EnrichedTemperature(r.value, "Celsius"));
        }
        return this;
    }

    private Behavior<TemperatureCommand> onSwitchMode(SwitchMode msg) {
        simulateMode = msg.simulate;

        if (simulateMode) {
            getContext().getLog().info("Switched to simulation mode");
            timers.startTimerAtFixedRate(new Tick(), Duration.ofSeconds(5));
        } else {
            getContext().getLog().info("Switched to manual mode");
            timers.cancel(new Tick());
        }

        return this;
    }

    private Behavior<TemperatureCommand> onTick(Tick t) {
        if (simulateMode) {
            double simulated = 20 + Math.random() * 10; // 20–30°C
            getContext().getLog().info("Simulated temperature: {}", simulated);
            airCondition.tell(new AirCondition.EnrichedTemperature(simulated, "Celsius"));
        }
        return this;
    }

    private Behavior<TemperatureCommand> onEnvironmentTemperatureUpdate(EnvironmentTemperatureUpdate msg) {
        if (simulateMode) {
            getContext().getLog().info("Environment temperature update: {}", msg.getTemperature());
            airCondition.tell(new AirCondition.EnrichedTemperature(msg.getTemperature(), "Celsius"));
        }
        return this;
    }

    private TemperatureSensor onPostStop() {
        getContext().getLog().info("TemperatureSensor actor stopped");
        return this;
    }
}