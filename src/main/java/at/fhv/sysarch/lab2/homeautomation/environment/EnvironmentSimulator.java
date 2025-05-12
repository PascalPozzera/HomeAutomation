package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.time.Duration;
import java.util.Random;

public class EnvironmentSimulator extends AbstractBehavior<EnvironmentSimulator.EnvironmentCommand> {

    public interface EnvironmentCommand {}

    public static final class Start implements EnvironmentCommand {}
    public static final class Stop implements EnvironmentCommand {}
    public static final class Tick implements EnvironmentCommand {}
    public static final class SetTemperature implements EnvironmentCommand {
        private final double temperature;

        public SetTemperature(double temperature) {
            this.temperature = temperature;
        }

        public double getTemperature() {
            return temperature;
        }
    }

    public static final class SetWeatherCondition implements EnvironmentCommand {
        private final WeatherCondition weatherCondition;

        public SetWeatherCondition(WeatherCondition weatherCondition) {
            this.weatherCondition = weatherCondition;
        }

        public WeatherCondition getWeatherCondition() {
            return weatherCondition;
        }
    }

    public enum WeatherCondition {
        SUNNY, CLOUDY, RAINY, SNOWY
    }

    private final Random random = new Random();
    private double currentTemperature = 23.0;
    private WeatherCondition currentWeatherCondition = WeatherCondition.SUNNY;
    private boolean isRunning = false;
    private final TimerScheduler<EnvironmentCommand> timers;
    private final ActorRef<TemperatureCommand> temperatureListener;
    private final ActorRef<WeatherCommand> weatherListener;

    public interface TemperatureCommand {}
    public static final class TemperatureUpdate implements TemperatureCommand {
        private final double temperature;

        public TemperatureUpdate(double temperature) {
            this.temperature = temperature;
        }

        public double getTemperature() {
            return temperature;
        }
    }

    public interface WeatherCommand {}
    public static final class WeatherUpdate implements WeatherCommand {
        private final WeatherCondition condition;

        public WeatherUpdate(WeatherCondition condition) {
            this.condition = condition;
        }

        public WeatherCondition getCondition() {
            return condition;
        }
    }

    private EnvironmentSimulator(ActorContext<EnvironmentCommand> context,
                                 TimerScheduler<EnvironmentCommand> timers,
                                 ActorRef<TemperatureCommand> temperatureListener,
                                 ActorRef<WeatherCommand> weatherListener) {
        super(context);
        this.timers = timers;
        this.temperatureListener = temperatureListener;
        this.weatherListener = weatherListener;
    }

    public static Behavior<EnvironmentCommand> create(
            ActorRef<TemperatureCommand> temperatureListener,
            ActorRef<WeatherCommand> weatherListener) {
        return Behaviors.setup(context ->
                Behaviors.withTimers(timers ->
                        new EnvironmentSimulator(context, timers, temperatureListener, weatherListener)
                )
        );
    }

    @Override
    public Receive<EnvironmentCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Start.class, this::onStart)
                .onMessage(Stop.class, this::onStop)
                .onMessage(Tick.class, this::onTick)
                .onMessage(SetTemperature.class, this::onSetTemperature)
                .onMessage(SetWeatherCondition.class, this::onSetWeatherCondition)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<EnvironmentCommand> onStart(Start msg) {
        if (!isRunning) {
            getContext().getLog().info("Environment simulator started");
            isRunning = true;
            timers.startTimerAtFixedRate(new Tick(), Duration.ofSeconds(10));
            notifyListeners();
        }
        return this;
    }

    private Behavior<EnvironmentCommand> onStop(Stop msg) {
        if (isRunning) {
            getContext().getLog().info("Environment simulator stopped");
            isRunning = false;
            timers.cancel(new Tick());
        }
        return this;
    }

    private Behavior<EnvironmentCommand> onTick(Tick msg) {
        if (isRunning) {
            double change = (random.nextDouble() * 2.0 - 1.0);
            currentTemperature += change;
            currentTemperature = Math.round(currentTemperature * 10.0) / 10.0;

            if (random.nextDouble() < 0.1) {
                WeatherCondition[] conditions = WeatherCondition.values();
                currentWeatherCondition = conditions[random.nextInt(conditions.length)];
            }

            getContext().getLog().info("Environment update: {}°C, {}", currentTemperature, currentWeatherCondition);
            notifyListeners();
        }
        return this;
    }

    private Behavior<EnvironmentCommand> onSetTemperature(SetTemperature msg) {
        currentTemperature = msg.getTemperature();
        getContext().getLog().info("Temperature manually set to {}°C", currentTemperature);
        notifyListeners();
        return this;
    }

    private Behavior<EnvironmentCommand> onSetWeatherCondition(SetWeatherCondition msg) {
        currentWeatherCondition = msg.getWeatherCondition();
        getContext().getLog().info("Weather condition manually set to {}", currentWeatherCondition);
        notifyListeners();
        return this;
    }

    private void notifyListeners() {
        if (temperatureListener != null) {
            temperatureListener.tell(new TemperatureUpdate(currentTemperature));
        }
        if (weatherListener != null) {
            weatherListener.tell(new WeatherUpdate(currentWeatherCondition));
        }
    }

    private EnvironmentSimulator onPostStop() {
        getContext().getLog().info("Environment simulator stopped");
        return this;
    }
}