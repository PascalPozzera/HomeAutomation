package at.fhv.sysarch.lab2.homeautomation.devices.weather;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.environment.EnvironmentSimulator;

import java.time.Duration;

public class WeatherSensor extends AbstractBehavior<WeatherSensor.WeatherCommand> {

    public interface WeatherCommand {}

    public static final class ReadWeather implements WeatherCommand {
        final EnvironmentSimulator.WeatherCondition condition;

        public ReadWeather(EnvironmentSimulator.WeatherCondition condition) {
            this.condition = condition;
        }
    }

    public static final class SwitchMode implements WeatherCommand {
        final boolean simulate;

        public SwitchMode(boolean simulate) {
            this.simulate = simulate;
        }
    }

    public static final class Tick implements WeatherCommand {}

    public static final class EnvironmentWeatherUpdate implements WeatherCommand {
        final EnvironmentSimulator.WeatherCondition condition;

        public EnvironmentWeatherUpdate(EnvironmentSimulator.WeatherCondition condition) {
            this.condition = condition;
        }

        public EnvironmentSimulator.WeatherCondition getCondition() {
            return condition;
        }
    }

    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final TimerScheduler<WeatherCommand> timers;
    private boolean simulateMode = false;
    private EnvironmentSimulator.WeatherCondition lastCondition = EnvironmentSimulator.WeatherCondition.CLOUDY;

    public static Behavior<WeatherCommand> create(ActorRef<Blinds.BlindsCommand> blinds) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers ->
                new WeatherSensor(context, blinds, timers)
        ));
    }

    private WeatherSensor(ActorContext<WeatherCommand> context,
                          ActorRef<Blinds.BlindsCommand> blinds,
                          TimerScheduler<WeatherCommand> timers) {
        super(context);
        this.blinds = blinds;
        this.timers = timers;
        getContext().getLog().info("WeatherSensor started in manual mode");
    }

    @Override
    public Receive<WeatherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ReadWeather.class, this::onReadWeather)
                .onMessage(SwitchMode.class, this::onSwitchMode)
                .onMessage(Tick.class, this::onTick)
                .onMessage(EnvironmentWeatherUpdate.class, this::onEnvironmentWeatherUpdate)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<WeatherCommand> onReadWeather(ReadWeather r) {
        if (!simulateMode) {
            getContext().getLog().info("Manual weather input: {}", r.condition);
            lastCondition = r.condition;
            notifyBlinds(r.condition);
        }
        return this;
    }

    private Behavior<WeatherCommand> onSwitchMode(SwitchMode msg) {
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

    private Behavior<WeatherCommand> onTick(Tick t) {
        if (simulateMode) {
            EnvironmentSimulator.WeatherCondition[] conditions = EnvironmentSimulator.WeatherCondition.values();
            EnvironmentSimulator.WeatherCondition simulated = conditions[(int) (Math.random() * conditions.length)];
            getContext().getLog().info("Simulated weather: {}", simulated);
            lastCondition = simulated;
            notifyBlinds(simulated);
        }
        return this;
    }

    private Behavior<WeatherCommand> onEnvironmentWeatherUpdate(EnvironmentWeatherUpdate msg) {
        if (simulateMode) {
            getContext().getLog().info("Environment weather update: {}", msg.getCondition());
            lastCondition = msg.getCondition();
            notifyBlinds(msg.getCondition());
        }
        return this;
    }

    private void notifyBlinds(EnvironmentSimulator.WeatherCondition condition) {
        blinds.tell(new Blinds.EnrichedWeather(condition));
    }

    private WeatherSensor onPostStop() {
        getContext().getLog().info("WeatherSensor actor stopped");
        return this;
    }
}