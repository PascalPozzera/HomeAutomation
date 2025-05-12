package at.fhv.sysarch.lab2.homeautomation.devices.media;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import at.fhv.sysarch.lab2.homeautomation.devices.weather.Blinds;

import java.time.Duration;
import java.util.Optional;

public class MediaStation extends AbstractBehavior<MediaStation.MediaCommand> {

    public interface MediaCommand {}

    public static final class PlayMovie implements MediaCommand {
        final String movieTitle;
        final ActorRef<PlayMovieResponse> replyTo;

        public PlayMovie(String movieTitle, ActorRef<PlayMovieResponse> replyTo) {
            this.movieTitle = movieTitle;
            this.replyTo = replyTo;
        }
    }

    public static final class StopMovie implements MediaCommand {
        final ActorRef<StopMovieResponse> replyTo;

        public StopMovie(ActorRef<StopMovieResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class GetCurrentMovie implements MediaCommand {
        final ActorRef<CurrentMovieResponse> replyTo;

        public GetCurrentMovie(ActorRef<CurrentMovieResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    private static final class MovieEnded implements MediaCommand {
        final String movieTitle;

        private MovieEnded(String movieTitle) {
            this.movieTitle = movieTitle;
        }
    }

    public static class PlayMovieResponse {
        private final boolean success;
        private final String message;

        public PlayMovieResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class StopMovieResponse {
        private final boolean wasPlaying;
        private final String message;

        public StopMovieResponse(boolean wasPlaying, String message) {
            this.wasPlaying = wasPlaying;
            this.message = message;
        }

        public boolean wasPlaying() {
            return wasPlaying;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class CurrentMovieResponse {
        private final Optional<String> movieTitle;

        public CurrentMovieResponse(Optional<String> movieTitle) {
            this.movieTitle = movieTitle;
        }

        public Optional<String> getMovieTitle() {
            return movieTitle;
        }
    }

    private final String identifier;
    private final ActorRef<Blinds.BlindsCommand> blinds;
    private Optional<String> currentMovie = Optional.empty();

    public static Behavior<MediaCommand> create(String identifier, ActorRef<Blinds.BlindsCommand> blinds) {
        return Behaviors.setup(context -> new MediaStation(context, identifier, blinds));
    }

    private MediaStation(ActorContext<MediaCommand> context, String identifier, ActorRef<Blinds.BlindsCommand> blinds) {
        super(context);
        this.identifier = identifier;
        this.blinds = blinds;
        getContext().getLog().info("Media Station {} started", identifier);
    }

    @Override
    public Receive<MediaCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(PlayMovie.class, this::onPlayMovie)
                .onMessage(StopMovie.class, this::onStopMovie)
                .onMessage(GetCurrentMovie.class, this::onGetCurrentMovie)
                .onMessage(MovieEnded.class, this::onMovieEnded)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MediaCommand> onPlayMovie(PlayMovie msg) {
        if (currentMovie.isPresent()) {
            getContext().getLog().info("Cannot play '{}': Movie '{}' is already playing", msg.movieTitle, currentMovie.get());
            msg.replyTo.tell(new PlayMovieResponse(false, "Movie '" + currentMovie.get() + "' is already playing. Stop it first."));
        } else {
            currentMovie = Optional.of(msg.movieTitle);
            getContext().getLog().info("Started playing movie: {}", msg.movieTitle);

            blinds.tell(new Blinds.MovieStateChange(true));

            getContext().scheduleOnce(
                    Duration.ofMinutes(2),
                    getContext().getSelf(),
                    new MovieEnded(msg.movieTitle)
            );

            msg.replyTo.tell(new PlayMovieResponse(true, "Now playing: " + msg.movieTitle));
        }

        return this;
    }

    private Behavior<MediaCommand> onStopMovie(StopMovie msg) {
        if (currentMovie.isPresent()) {
            String stoppedMovie = currentMovie.get();
            currentMovie = Optional.empty();
            getContext().getLog().info("Stopped playing movie: {}", stoppedMovie);

            blinds.tell(new Blinds.MovieStateChange(false));

            msg.replyTo.tell(new StopMovieResponse(true, "Stopped playing: " + stoppedMovie));
        } else {
            getContext().getLog().info("No movie currently playing to stop");
            msg.replyTo.tell(new StopMovieResponse(false, "No movie was playing"));
        }

        return this;
    }

    private Behavior<MediaCommand> onGetCurrentMovie(GetCurrentMovie msg) {
        msg.replyTo.tell(new CurrentMovieResponse(currentMovie));
        return this;
    }

    private Behavior<MediaCommand> onMovieEnded(MovieEnded msg) {
        if (currentMovie.isPresent() && currentMovie.get().equals(msg.movieTitle)) {
            getContext().getLog().info("Movie ended: {}", msg.movieTitle);
            currentMovie = Optional.empty();

            blinds.tell(new Blinds.MovieStateChange(false));
        }

        return this;
    }

    private Behavior<MediaCommand> onPostStop() {
        getContext().getLog().info("Media Station actor {} stopped", identifier);
        return this;
    }
}
