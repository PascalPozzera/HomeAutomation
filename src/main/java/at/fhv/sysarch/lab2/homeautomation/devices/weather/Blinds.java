package at.fhv.sysarch.lab2.homeautomation.devices.weather;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.environment.EnvironmentSimulator;

public class Blinds extends AbstractBehavior<Blinds.BlindsCommand> {

    public interface BlindsCommand {}

    public static final class EnrichedWeather implements BlindsCommand {
        final EnvironmentSimulator.WeatherCondition condition;

        public EnrichedWeather(EnvironmentSimulator.WeatherCondition condition) {
            this.condition = condition;
        }
    }

    public static final class MovieStateChange implements BlindsCommand {
        final boolean isPlaying;

        public MovieStateChange(boolean isPlaying) {
            this.isPlaying = isPlaying;
        }
    }

    public static final class ManualOverride implements BlindsCommand {
        final boolean open;

        public ManualOverride(boolean open) {
            this.open = open;
        }
    }

    private final String identifier;
    private boolean isOpen = true;
    private boolean moviePlaying = false;
    private boolean manualMode = false;

    public static Behavior<BlindsCommand> create(String identifier) {
        return Behaviors.setup(context -> new Blinds(context, identifier));
    }

    private Blinds(ActorContext<BlindsCommand> context, String identifier) {
        super(context);
        this.identifier = identifier;
        getContext().getLog().info("Blinds {} started - currently {}", identifier, isOpen ? "OPEN" : "CLOSED");
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(EnrichedWeather.class, this::onWeatherChanged)
                .onMessage(MovieStateChange.class, this::onMovieStateChanged)
                .onMessage(ManualOverride.class, this::onManualOverride)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<BlindsCommand> onWeatherChanged(EnrichedWeather msg) {
        if (!manualMode && !moviePlaying) {
            if (msg.condition == EnvironmentSimulator.WeatherCondition.SUNNY && isOpen) {
                isOpen = false;
                getContext().getLog().info("Blinds {} CLOSED due to sunny weather", identifier);
            } else if (msg.condition != EnvironmentSimulator.WeatherCondition.SUNNY && !isOpen) {
                isOpen = true;
                getContext().getLog().info("Blinds {} OPENED due to non-sunny weather", identifier);
            }
        }

        return this;
    }

    private Behavior<BlindsCommand> onMovieStateChanged(MovieStateChange msg) {
        moviePlaying = msg.isPlaying;

        if (moviePlaying && isOpen) {
            isOpen = false;
            getContext().getLog().info("Blinds {} CLOSED due to movie playing", identifier);
        } else if (!moviePlaying && !manualMode) {
            getContext().getLog().info("Movie stopped, blinds {} remain {}", identifier, isOpen ? "OPEN" : "CLOSED");
        }

        return this;
    }

    private Behavior<BlindsCommand> onManualOverride(ManualOverride msg) {
        manualMode = true;

        if (msg.open != isOpen) {
            isOpen = msg.open;
            getContext().getLog().info("Blinds {} manually {}", identifier, isOpen ? "OPEN" : "CLOSED");
        }

        return this;
    }

    private Blinds onPostStop() {
        getContext().getLog().info("Blinds actor {} stopped", identifier);
        return this;
    }
}
