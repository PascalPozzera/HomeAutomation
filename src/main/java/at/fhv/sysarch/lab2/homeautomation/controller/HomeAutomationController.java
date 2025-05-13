package at.fhv.sysarch.lab2.homeautomation.controller;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import at.fhv.sysarch.lab2.homeautomation.HomeAutomation;
import at.fhv.sysarch.lab2.homeautomation.devices.ac.AirCondition;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.*;
import at.fhv.sysarch.lab2.homeautomation.devices.media.MediaStation;
import at.fhv.sysarch.lab2.homeautomation.devices.weather.Blinds;
import at.fhv.sysarch.lab2.homeautomation.devices.weather.WeatherSensor;
import at.fhv.sysarch.lab2.homeautomation.environment.EnvironmentSimulator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletionStage;

@RestController
@RequestMapping("/api")
public class HomeAutomationController {

    private final ActorRef<HomeAutomation.Command> homeAutomation;
    private final ActorRef<AirCondition.AirConditionCommand> airCondition;
    private final ActorRef<Blinds.BlindsCommand> blinds;
    private final ActorRef<WeatherSensor.WeatherCommand> weatherSensor;
    private final ActorRef<MediaStation.MediaCommand> mediaStation;
    private final ActorRef<Fridge.FridgeCommand> fridge;
    private final Scheduler scheduler;

    public HomeAutomationController(
            ActorSystem<HomeAutomation.Command> actorSystem,
            ActorRef<HomeAutomation.Command> homeAutomation,
            ActorRef<AirCondition.AirConditionCommand> airCondition,
            ActorRef<Blinds.BlindsCommand> blinds,
            ActorRef<WeatherSensor.WeatherCommand> weatherSensor,
            ActorRef<MediaStation.MediaCommand> mediaStation,
            ActorRef<Fridge.FridgeCommand> fridge) {
        this.homeAutomation = homeAutomation;
        this.airCondition = airCondition;
        this.blinds = blinds;
        this.weatherSensor = weatherSensor;
        this.mediaStation = mediaStation;
        this.fridge = fridge;
        this.scheduler = actorSystem.scheduler();
    }

    // Environment endpoints

    @PostMapping("/environment/source")
    public ResponseEntity<String> switchEnvironmentSource(@RequestParam String type) {
        HomeAutomation.SwitchEnvironmentSource.SourceType sourceType;

        try {
            sourceType = HomeAutomation.SwitchEnvironmentSource.SourceType.valueOf(type.toUpperCase());
            if (sourceType == HomeAutomation.SwitchEnvironmentSource.SourceType.MANUAL) {
                airCondition.tell(new AirCondition.SwitchSensorMode(false));
            } else {
                airCondition.tell(new AirCondition.SwitchSensorMode(true));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid source type. Use INTERNAL, EXTERNAL, or MANUAL");
        }

        homeAutomation.tell(new HomeAutomation.SwitchEnvironmentSource(sourceType));
        return ResponseEntity.ok("Switched to " + type + " environment source");
    }

    @PostMapping("/environment/temperature")
    public ResponseEntity<String> setTemperature(@RequestParam double value) {
        homeAutomation.tell(new HomeAutomation.SetEnvironmentValues(
                Optional.of(value), Optional.empty()));
        return ResponseEntity.ok("Temperature set to " + value + "°C");
    }

    @PostMapping("/environment/weather")
    public ResponseEntity<String> setWeather(@RequestParam String condition) {
        try {
            EnvironmentSimulator.WeatherCondition weatherCondition =
                    EnvironmentSimulator.WeatherCondition.valueOf(condition.toUpperCase());
            homeAutomation.tell(new HomeAutomation.SetEnvironmentValues(
                    Optional.empty(), Optional.of(weatherCondition)));
            return ResponseEntity.ok("Weather set to " + condition);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid weather condition. Use SUNNY, CLOUDY, RAINY, or SNOWY");
        }
    }

    // AC endpoints

    @PostMapping("/ac/power")
    public ResponseEntity<String> switchAC(@RequestParam boolean on) {
        airCondition.tell(new AirCondition.PowerAirCondition(on));
        return ResponseEntity.ok("Air conditioning " + (on ? "ON" : "OFF"));
    }

    @GetMapping("/ac/status")
    public ResponseEntity<Boolean> getACStatus() {
        CompletionStage<AirCondition.StatusResponse> result =
                AskPattern.ask(
                        airCondition,
                        AirCondition.GetStatus::new,
                        Duration.ofSeconds(2),
                        scheduler
                );

        return result.thenApply(res -> ResponseEntity.ok(res.isOn)).toCompletableFuture().join();
    }


    // Blinds endpoints

    @PostMapping("/blinds/position")
    public ResponseEntity<String> setBlindsPosition(@RequestParam boolean open) {
        blinds.tell(new Blinds.ManualOverride(open));
        return ResponseEntity.ok("Blinds " + (open ? "OPENED" : "CLOSED"));
    }

    @PostMapping("/blinds/auto")
    public ResponseEntity<String> setBlindsAuto() {
        weatherSensor.tell(new WeatherSensor.ReadWeather(EnvironmentSimulator.WeatherCondition.CLOUDY));
        return ResponseEntity.ok("Blinds reset to automatic mode");
    }

    @GetMapping("/blinds/status")
    public ResponseEntity<Boolean> getBlindsStatus() {
        CompletionStage<Blinds.StatusResponse> result =
                AskPattern.ask(
                        blinds,
                        Blinds.GetStatus::new,
                        Duration.ofSeconds(2),
                        scheduler
                );

        return result.thenApply(res -> ResponseEntity.ok(res.isOpen)).toCompletableFuture().join();
    }


    // Media Station endpoints

    @PostMapping("/media/play")
    public ResponseEntity<Map<String, Object>> playMovie(@RequestParam String title) {
        CompletionStage<MediaStation.PlayMovieResponse> response =
                AskPattern.ask(
                        mediaStation,
                        replyTo -> new MediaStation.PlayMovie(title, replyTo),
                        Duration.ofSeconds(5),
                        scheduler
                );

        return response.thenApply(result -> {
            if (result.isSuccess()) {
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("message", result.getMessage());
                return ResponseEntity.ok(responseMap);
            } else {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("error", result.getMessage());
                return ResponseEntity.badRequest().body(errorMap);
            }
        }).toCompletableFuture().join();
    }

    @PostMapping("/media/stop")
    public ResponseEntity<Map<String, Object>> stopMovie() {
        CompletionStage<MediaStation.StopMovieResponse> response =
                AskPattern.ask(
                        mediaStation,
                        MediaStation.StopMovie::new,
                        Duration.ofSeconds(5),
                        scheduler
                );

        return response.thenApply(result -> {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("message", result.wasPlaying() ?
                    result.getMessage() : "No movie was playing");
            return ResponseEntity.ok(responseMap);
        }).toCompletableFuture().join();
    }

    @GetMapping("/media/status")
    public ResponseEntity<Map<String, Object>> getMediaStatus() {
        CompletionStage<MediaStation.CurrentMovieResponse> response =
                AskPattern.ask(
                        mediaStation,
                        MediaStation.GetCurrentMovie::new,
                        Duration.ofSeconds(5),
                        scheduler
                );

        return response.thenApply(result -> {
            Map<String, Object> responseMap = new HashMap<>();
            if (result.getMovieTitle().isPresent()) {
                responseMap.put("playing", true);
                responseMap.put("title", result.getMovieTitle().get());
            } else {
                responseMap.put("playing", false);
            }
            return ResponseEntity.ok(responseMap);
        }).toCompletableFuture().join();
    }

    // Fridge endpoints

    @GetMapping("/fridge/contents")
    public ResponseEntity<Map<String, Object>> getFridgeContents() {
        CompletionStage<Fridge.ContentsResponse> response =
                AskPattern.ask(
                        fridge,
                        Fridge.GetContents::new,
                        Duration.ofSeconds(5),
                        scheduler
                );

        return response.thenApply(result -> {
            Map<String, Object> contents = new HashMap<>();
            contents.put("currentItemCount", result.getCurrentItemCount());
            contents.put("maxItemCount", result.getMaxItemCount());
            contents.put("currentWeight", result.getCurrentWeight());
            contents.put("maxWeight", result.getMaxWeight());

            List<Map<String, Object>> products = new ArrayList<>();
            result.getContents().forEach((product, quantity) -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", product.getId());
                item.put("name", product.getName());
                item.put("price", product.getPrice());
                item.put("weight", product.getWeight());
                item.put("quantity", quantity);
                products.add(item);
            });

            contents.put("products", products);
            return ResponseEntity.ok(contents);
        }).toCompletableFuture().join();
    }

    @PostMapping("/fridge/consume")
    public ResponseEntity<Map<String, Object>> consumeProduct(@RequestParam String id, @RequestParam int quantity) {
        CompletionStage<Fridge.ConsumeResponse> response =
                AskPattern.ask(
                        fridge,
                        replyTo -> new Fridge.ConsumeProduct(id, quantity, replyTo),
                        Duration.ofSeconds(5),
                        scheduler
                );

        return response.thenApply(result -> {
            Map<String, Object> responseMap = new HashMap<>();
            if (result.isSuccess()) {
                responseMap.put("message", result.getMessage());
                return ResponseEntity.ok(responseMap);
            } else {
                responseMap.put("error", result.getMessage());
                return ResponseEntity.badRequest().body(responseMap);
            }
        }).toCompletableFuture().join();
    }

    @PostMapping("/fridge/order")
    public ResponseEntity<Map<String, Object>> orderProduct(
            @RequestParam String name,
            @RequestParam BigDecimal price,
            @RequestParam double weight,
            @RequestParam int quantity) {

        Product product = new Product(name, price, weight);
        List<OrderItem> items = Collections.singletonList(new OrderItem(product, quantity));

        CompletionStage<Fridge.OrderResponse> response =
                AskPattern.ask(
                        fridge,
                        replyTo -> new Fridge.OrderProducts(items, replyTo),
                        Duration.ofSeconds(10),
                        scheduler
                );

        return response.thenApply(result -> {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", result.isSuccess());
            responseMap.put("message", result.getMessage());

            if (result.isSuccess() && result.getReceipt().isPresent()) {
                Receipt receipt = result.getReceipt().get();

                Map<String, Object> receiptMap = new HashMap<>();
                receiptMap.put("orderId", receipt.getOrderId());
                receiptMap.put("timestamp", receipt.getTimestamp().toString());
                receiptMap.put("totalPrice", receipt.getTotalPrice());

                List<Map<String, Object>> itemsList = new ArrayList<>();
                receipt.getItems().forEach(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("name", item.getProduct().getName());
                    itemMap.put("price", item.getProduct().getPrice());
                    itemMap.put("quantity", item.getQuantity());
                    itemsList.add(itemMap);
                });

                receiptMap.put("items", itemsList);
                responseMap.put("receipt", receiptMap);
            }

            if (result.isSuccess()) {
                return ResponseEntity.ok(responseMap);
            } else {
                return ResponseEntity.badRequest().body(responseMap);
            }
        }).toCompletableFuture().join();
    }

    @GetMapping("/fridge/history")
    public ResponseEntity<Map<String, Object>> getOrderHistory() {
        CompletionStage<Fridge.OrderHistoryResponse> response =
                AskPattern.ask(
                        fridge,
                        Fridge.GetOrderHistory::new,
                        Duration.ofSeconds(5),
                        scheduler
                );

        return response.thenApply(result -> {
            Map<String, Object> history = new HashMap<>();

            List<Map<String, Object>> orders = new ArrayList<>();
            result.getOrders().forEach(order -> {
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("id", order.getId());
                orderMap.put("timestamp", order.getTimestamp().toString());
                orderMap.put("totalPrice", order.getTotalPrice());

                List<Map<String, Object>> items = new ArrayList<>();
                order.getItems().forEach(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("name", item.getProduct().getName());
                    itemMap.put("price", item.getProduct().getPrice());
                    itemMap.put("quantity", item.getQuantity());
                    items.add(itemMap);
                });

                orderMap.put("items", items);
                orders.add(orderMap);
            });

            List<Map<String, Object>> receipts = new ArrayList<>();
            result.getReceipts().forEach(receipt -> {
                Map<String, Object> receiptMap = new HashMap<>();
                receiptMap.put("orderId", receipt.getOrderId());
                receiptMap.put("timestamp", receipt.getTimestamp().toString());
                receiptMap.put("totalPrice", receipt.getTotalPrice());
                receipts.add(receiptMap);
            });

            history.put("orders", orders);
            history.put("receipts", receipts);

            return ResponseEntity.ok(history);
        }).toCompletableFuture().join();
    }
}