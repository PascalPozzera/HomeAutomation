package at.fhv.sysarch.lab2.homeautomation.environment;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class MqttEnvironmentSource extends AbstractBehavior<MqttEnvironmentSource.MqttCommand> {

    public interface MqttCommand {}

    public static final class Connect implements MqttCommand {}
    public static final class Disconnect implements MqttCommand {}

    private static final String BROKER_URL = "tcp://10.0.40.161:1883";
    private static final String CLIENT_ID = "HomeAutomationClient-" + System.currentTimeMillis();
    private static final String TEMPERATURE_TOPIC = "environment/temperature";
    private static final String WEATHER_TOPIC = "environment/weather";

    private MqttClient mqttClient;
    private final ActorRef<EnvironmentSimulator.TemperatureCommand> temperatureListener;
    private final ActorRef<EnvironmentSimulator.WeatherCommand> weatherListener;

    private MqttEnvironmentSource(ActorContext<MqttCommand> context,
                                  ActorRef<EnvironmentSimulator.TemperatureCommand> temperatureListener,
                                  ActorRef<EnvironmentSimulator.WeatherCommand> weatherListener) {
        super(context);
        this.temperatureListener = temperatureListener;
        this.weatherListener = weatherListener;
    }

    public static Behavior<MqttCommand> create(
            ActorRef<EnvironmentSimulator.TemperatureCommand> temperatureListener,
            ActorRef<EnvironmentSimulator.WeatherCommand> weatherListener) {
        return Behaviors.setup(context ->
                new MqttEnvironmentSource(context, temperatureListener, weatherListener)
        );
    }

    @Override
    public Receive<MqttCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Connect.class, this::onConnect)
                .onMessage(Disconnect.class, this::onDisconnect)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<MqttCommand> onConnect(Connect msg) {
        try {
            getContext().getLog().info("Connecting to MQTT broker: {}", BROKER_URL);
            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setAutomaticReconnect(true);
            connectOptions.setConnectionTimeout(10);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    getContext().getLog().warn("Connection to MQTT broker lost: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    try {
                        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                        getContext().getLog().debug("Message received on topic {}: {}", topic, payload);

                        if (topic.equals(TEMPERATURE_TOPIC)) {
                            JSONObject json = new JSONObject(payload);
                            double temperature = json.getDouble("value");
                            if (temperatureListener != null) {
                                temperatureListener.tell(new EnvironmentSimulator.TemperatureUpdate(temperature));
                            }
                        } else if (topic.equals(WEATHER_TOPIC)) {
                            JSONObject json = new JSONObject(payload);
                            String weatherStr = json.getString("condition");
                            try {
                                EnvironmentSimulator.WeatherCondition condition =
                                        EnvironmentSimulator.WeatherCondition.valueOf(weatherStr.toUpperCase());
                                if (weatherListener != null) {
                                    weatherListener.tell(new EnvironmentSimulator.WeatherUpdate(condition));
                                }
                            } catch (IllegalArgumentException e) {
                                getContext().getLog().warn("Unknown weather condition: {}", weatherStr);
                            }
                        }
                    } catch (Exception e) {
                        getContext().getLog().error("Error processing MQTT message: {}", e.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect(connectOptions);
            getContext().getLog().info("Connected to MQTT broker");

            mqttClient.subscribe(TEMPERATURE_TOPIC, 0);
            mqttClient.subscribe(WEATHER_TOPIC, 0);
            getContext().getLog().info("Subscribed to topics: {} and {}", TEMPERATURE_TOPIC, WEATHER_TOPIC);

        } catch (MqttException e) {
            getContext().getLog().error("Failed to connect to MQTT broker: {}", e.getMessage());
        }

        return this;
    }

    private Behavior<MqttCommand> onDisconnect(Disconnect msg) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                getContext().getLog().info("Disconnected from MQTT broker");
            }
        } catch (MqttException e) {
            getContext().getLog().error("Error disconnecting from MQTT broker: {}", e.getMessage());
        }
        return this;
    }

    private MqttEnvironmentSource onPostStop() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                getContext().getLog().info("Disconnected from MQTT broker on actor stop");
            }
        } catch (MqttException e) {
            getContext().getLog().error("Error disconnecting from MQTT broker: {}", e.getMessage());
        }
        return this;
    }
}