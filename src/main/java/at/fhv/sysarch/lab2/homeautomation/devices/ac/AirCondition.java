package at.fhv.sysarch.lab2.homeautomation.devices.ac;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorRef;

public class AirCondition extends AbstractBehavior<AirCondition.AirConditionCommand> {

    public interface AirConditionCommand {}
    private final String identifier;
    private final ActorRef<TemperatureSensor.TemperatureCommand> tempSensor;
    private Boolean isOn = false;

    public AirCondition(ActorContext<AirConditionCommand> context, String identifier) {
        super(context);
        this.identifier = identifier;

        //Create Temperature sensor
        this.tempSensor = context.spawn(TemperatureSensor.create(getContext().getSelf()), "temperatureSensor");

        // Start Temperatur sensor in Simulationsmode
        this.tempSensor.tell(new TemperatureSensor.SwitchMode(true));

        getContext().getLog().info("AirCondition started");
    }

    //MessageType (PowerAirCondition, EnrichedTemperature)
    public static final class PowerAirCondition implements AirConditionCommand {
        final Boolean value;

        public PowerAirCondition(Boolean value) {
            this.value = value;
        }
    }

    public static final class EnrichedTemperature implements AirConditionCommand {
        Double value;
        String unit;

        public EnrichedTemperature(Double value, String unit) {
            this.value = value;
            this.unit = unit;
        }
    }

    public static Behavior<AirConditionCommand> create(String identifier) {
        return Behaviors.setup(context -> new AirCondition(context, identifier));
    }

    @Override
    public Receive<AirConditionCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(EnrichedTemperature.class, this::onReadTemperature)
                .onMessage(PowerAirCondition.class, this::onPowerCommand)
                .onMessage(SwitchSensorMode.class, this::onSwitchSensorMode)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<AirConditionCommand> onPowerCommand(PowerAirCondition cmd) {
        isOn = cmd.value;
        getContext().getLog().info("AC manually switched {}", isOn ? "ON" : "OFF");
        return Behaviors.same();
    }

    private Behavior<AirConditionCommand> onReadTemperature(EnrichedTemperature cmd) {
        getContext().getLog().info("AirCondition reading {} {}", cmd.value, cmd.unit);

        if (cmd.value > 25.0 && !isOn) {
            getContext().getSelf().tell(new PowerAirCondition(true));
        } else if (cmd.value <= 25.0 && isOn) {
            getContext().getSelf().tell(new PowerAirCondition(false));
        }

        return Behaviors.same();
    }

    private Behavior<AirConditionCommand> onSwitchSensorMode(SwitchSensorMode cmd) {
        getContext().getLog().info("Switching sensor mode to {}", cmd.simulate ? "SIMULATION" : "MANUAL");
        tempSensor.tell(new TemperatureSensor.SwitchMode(cmd.simulate));
        return Behaviors.same();
    }

    private AirCondition onPostStop() {
        getContext().getLog().info("AirCondition actor {}-{} stopped", identifier);
        return this;
    }

    public static final class SwitchSensorMode implements AirConditionCommand {
        public final boolean simulate;

        public SwitchSensorMode(boolean simulate) {
            this.simulate = simulate;
        }
    }
}
