package at.fhv.sysarch.lab2.homeautomation;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.ac.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.Fridge;
import at.fhv.sysarch.lab2.homeautomation.devices.media.MediaStation;
import at.fhv.sysarch.lab2.homeautomation.devices.weather.Blinds;
import at.fhv.sysarch.lab2.homeautomation.devices.weather.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.environment.EnvironmentSimulator;
import at.fhv.sysarch.lab2.homeautomation.environment.MqttEnvironmentSource;

import java.util.Optional;

public class HomeAutomation extends AbstractBehavior<HomeAutomation.Command> {

    public interface Command {}

    public static final class Start implements Command {}

    public static final class SwitchEnvironmentSource implements Command {
        public enum SourceType { INTERNAL, EXTERNAL, MANUAL }

        final SourceType sourceType;

        public SwitchEnvironmentSource(SourceType sourceType) {
            this.sourceType = sourceType;
        }
    }

    public static final class SetEnvironmentValues implements Command {
        final Optional<Double> temperature;
        final Optional<EnvironmentSimulator.WeatherCondition> weatherCondition;

        public SetEnvironmentValues(Optional<Double> temperature,
                                    Optional<EnvironmentSimulator.WeatherCondition> weatherCondition) {
            this.temperature = temperature;
            this.weatherCondition = weatherCondition;
        }
    }

    private static final class ForwardTemperature implements Command {
        final double temperature;

        private ForwardTemperature(double temperature) {
            this.temperature = temperature;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(HomeAutomation::new);
    }

    private final ActorRef<EnvironmentSimulator.EnvironmentCommand> environmentSimulator;
    private final ActorRef<MqttEnvironmentSource.MqttCommand> mqttEnvironmentSource;
    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private final ActorRef<MediaStation.MediaCommand> mediaStation;
    private final ActorRef<Fridge.FridgeCommand> fridge;
    private SwitchEnvironmentSource.SourceType currentEnvironmentSource =
            SwitchEnvironmentSource.SourceType.INTERNAL;

    private HomeAutomation(ActorContext<Command> context) {
        super(context);

        this.blinds = context.spawn(Blinds.create("living-room"), "blinds");

        this.weatherSensor = context.spawn(
                WeatherSensor.create(blinds),
                "weather-sensor");

        this.airCondition = context.spawn(
                AirCondition.create("living-room"),
                "air-condition");

        ActorRef<EnvironmentSimulator.TemperatureCommand> temperatureAdapter =
                context.messageAdapter(
                        EnvironmentSimulator.TemperatureCommand.class,
                        msg -> {
                            if (msg instanceof EnvironmentSimulator.TemperatureUpdate) {
                                EnvironmentSimulator.TemperatureUpdate update = (EnvironmentSimulator.TemperatureUpdate) msg;
                                return new ForwardTemperature(update.getTemperature());
                            }
                            return new Command() {};
                        });

        ActorRef<EnvironmentSimulator.WeatherCommand> weatherAdapter =
                context.messageAdapter(
                        EnvironmentSimulator.WeatherCommand.class,
                        msg -> {
                            if (msg instanceof EnvironmentSimulator.WeatherUpdate) {
                                EnvironmentSimulator.WeatherUpdate update = (EnvironmentSimulator.WeatherUpdate) msg;
                                weatherSensor.tell(new WeatherSensor.EnvironmentWeatherUpdate(update.getCondition()));
                            }
                            return new Command() {};
                        });

        this.environmentSimulator = context.spawn(
                EnvironmentSimulator.create(temperatureAdapter, weatherAdapter),
                "environment-simulator");

        this.mqttEnvironmentSource = context.spawn(
                MqttEnvironmentSource.create(temperatureAdapter, weatherAdapter),
                "mqtt-environment-source");

        this.mediaStation = context.spawn(
                MediaStation.create("living-room", blinds),
                "media-station");

        this.fridge = context.spawn(
                Fridge.create("kitchen", 30.0, 50, "localhost"),
                "fridge");

        getContext().getLog().info("Home Automation system initialized");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Start.class, this::onStart)
                .onMessage(SwitchEnvironmentSource.class, this::onSwitchEnvironmentSource)
                .onMessage(SetEnvironmentValues.class, this::onSetEnvironmentValues)
                .onMessage(ForwardTemperature.class, this::onForwardTemperature)
                .build();
    }

    private Behavior<Command> onStart(Start msg) {
        getContext().getLog().info("Starting Home Automation system with internal environment simulator");

        environmentSimulator.tell(new EnvironmentSimulator.Start());

        return this;
    }

    private Behavior<Command> onSwitchEnvironmentSource(SwitchEnvironmentSource msg) {
        stopCurrentEnvironmentSource();

        currentEnvironmentSource = msg.sourceType;

        switch (currentEnvironmentSource) {
            case INTERNAL:
                getContext().getLog().info("Switching to internal environment simulation");
                environmentSimulator.tell(new EnvironmentSimulator.Start());
                break;

            case EXTERNAL:
                getContext().getLog().info("Switching to external environment source (MQTT)");
                mqttEnvironmentSource.tell(new MqttEnvironmentSource.Connect());
                break;

            case MANUAL:
                getContext().getLog().info("Switching to manual environment values");
                break;
        }

        return this;
    }

    private Behavior<Command> onForwardTemperature(ForwardTemperature msg) {
        airCondition.tell(new AirCondition.EnrichedTemperature(msg.temperature, "Celsius"));
        return this;
    }

    private Behavior<Command> onSetEnvironmentValues(SetEnvironmentValues msg) {
        if (currentEnvironmentSource == SwitchEnvironmentSource.SourceType.MANUAL) {
            if (msg.temperature.isPresent()) {
                double temp = msg.temperature.get();
                getContext().getLog().info("Manually setting temperature to {}°C", temp);
                airCondition.tell(new AirCondition.EnrichedTemperature(temp, "Celsius"));
            }

            if (msg.weatherCondition.isPresent()) {
                EnvironmentSimulator.WeatherCondition weather = msg.weatherCondition.get();
                getContext().getLog().info("Manually setting weather to {}", weather);
                weatherSensor.tell(new WeatherSensor.ReadWeather(weather));
            }
        } else {
            getContext().getLog().warn("Cannot set environment values manually when not in MANUAL mode");
        }

        return this;
    }

    private void stopCurrentEnvironmentSource() {
        switch (currentEnvironmentSource) {
            case INTERNAL:
                environmentSimulator.tell(new EnvironmentSimulator.Stop());
                break;

            case EXTERNAL:
                mqttEnvironmentSource.tell(new MqttEnvironmentSource.Disconnect());
                break;

            case MANUAL:
                break;
        }
    }
}