package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.grpc.OrderProcessorClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletionStage;

public class Fridge extends AbstractBehavior<Fridge.FridgeCommand> {

    public interface FridgeCommand {}

    public static final class GetContents implements FridgeCommand {
        final ActorRef<ContentsResponse> replyTo;

        public GetContents(ActorRef<ContentsResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class ConsumeProduct implements FridgeCommand {
        final String productId;
        final int quantity;
        final ActorRef<ConsumeResponse> replyTo;

        public ConsumeProduct(String productId, int quantity, ActorRef<ConsumeResponse> replyTo) {
            this.productId = productId;
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }

    public static final class OrderProducts implements FridgeCommand {
        final List<OrderItem> items;
        final ActorRef<OrderResponse> replyTo;

        public OrderProducts(List<OrderItem> items, ActorRef<OrderResponse> replyTo) {
            this.items = new ArrayList<>(items);
            this.replyTo = replyTo;
        }
    }

    public static final class GetOrderHistory implements FridgeCommand {
        final ActorRef<OrderHistoryResponse> replyTo;

        public GetOrderHistory(ActorRef<OrderHistoryResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    private static final class AutoReorder implements FridgeCommand {
        final Product product;
        final int quantityToOrder;

        private AutoReorder(Product product, int quantityToOrder) {
            this.product = product;
            this.quantityToOrder = quantityToOrder;
        }
    }

    public static final class OrderCompleted implements FridgeCommand {
        final Receipt receipt;
        final ActorRef<OrderResponse> originalReplyTo;

        private OrderCompleted(Receipt receipt, ActorRef<OrderResponse> originalReplyTo) {
            this.receipt = receipt;
            this.originalReplyTo = originalReplyTo;
        }
    }

    public static class ContentsResponse {
        private final Map<Product, Integer> contents;
        private final double currentWeight;
        private final int currentItemCount;
        private final double maxWeight;
        private final int maxItemCount;

        public ContentsResponse(Map<Product, Integer> contents, double currentWeight, int currentItemCount, double maxWeight, int maxItemCount) {
            this.contents = new HashMap<>(contents);
            this.currentWeight = currentWeight;
            this.currentItemCount = currentItemCount;
            this.maxWeight = maxWeight;
            this.maxItemCount = maxItemCount;
        }

        public Map<Product, Integer> getContents() {
            return Collections.unmodifiableMap(contents);
        }

        public double getCurrentWeight() {
            return currentWeight;
        }

        public int getCurrentItemCount() {
            return currentItemCount;
        }

        public double getMaxWeight() {
            return maxWeight;
        }

        public int getMaxItemCount() {
            return maxItemCount;
        }
    }

    public static class ConsumeResponse {
        private final boolean success;
        private final String message;

        public ConsumeResponse(boolean success, String message) {
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

    public static class OrderResponse {
        private final boolean success;
        private final String message;
        private final Optional<Receipt> receipt;

        public OrderResponse(boolean success, String message, Optional<Receipt> receipt) {
            this.success = success;
            this.message = message;
            this.receipt = receipt;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Optional<Receipt> getReceipt() {
            return receipt;
        }
    }

    public static class OrderHistoryResponse {
        private final List<Order> orders;
        private final List<Receipt> receipts;

        public OrderHistoryResponse(List<Order> orders, List<Receipt> receipts) {
            this.orders = new ArrayList<>(orders);
            this.receipts = new ArrayList<>(receipts);
        }

        public List<Order> getOrders() {
            return Collections.unmodifiableList(orders);
        }

        public List<Receipt> getReceipts() {
            return Collections.unmodifiableList(receipts);
        }
    }

    private final String identifier;
    private final double maxWeight;
    private final int maxItemCount;
    private final Map<Product, Integer> contents = new HashMap<>();
    private final List<Order> orderHistory = new ArrayList<>();
    private final List<Receipt> receiptHistory = new ArrayList<>();
    private final OrderProcessorClient orderProcessorClient;

    private static final int AUTO_REORDER_THRESHOLD = 1;
    private static final int AUTO_REORDER_QUANTITY = 3;

    public static Behavior<FridgeCommand> create(String identifier, double maxWeight, int maxItemCount, String orderProcessorAddress) {
        return Behaviors.setup(context -> new Fridge(context, identifier, maxWeight, maxItemCount, orderProcessorAddress));
    }

    private Fridge(ActorContext<FridgeCommand> context, String identifier, double maxWeight, int maxItemCount, String orderProcessorAddress) {
        super(context);
        this.identifier = identifier;
        this.maxWeight = maxWeight;
        this.maxItemCount = maxItemCount;
        this.orderProcessorClient = new OrderProcessorClient(orderProcessorAddress, context.getSystem());

        getContext().getLog().info("Fridge {} started with capacity: {} items, {} kg", identifier, maxItemCount, maxWeight);

        initializeDefaultContents();
    }

    private void initializeDefaultContents() {
        addToContents(new Product("Milk", new BigDecimal("1.99"), 1.0), 2);
        addToContents(new Product("Cheese", new BigDecimal("3.49"), 0.5), 1);
        addToContents(new Product("Eggs", new BigDecimal("2.29"), 0.4), 10);
        addToContents(new Product("Yogurt", new BigDecimal("0.99"), 0.2), 4);
        addToContents(new Product("Orange Juice", new BigDecimal("2.49"), 1.0), 1);
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetContents.class, this::onGetContents)
                .onMessage(ConsumeProduct.class, this::onConsumeProduct)
                .onMessage(OrderProducts.class, this::onOrderProducts)
                .onMessage(GetOrderHistory.class, this::onGetOrderHistory)
                .onMessage(AutoReorder.class, this::onAutoReorder)
                .onMessage(OrderCompleted.class, this::onOrderCompleted)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<FridgeCommand> onGetContents(GetContents msg) {
        double currentWeight = calculateCurrentWeight();
        int currentItemCount = calculateCurrentItemCount();

        msg.replyTo.tell(new ContentsResponse(contents, currentWeight, currentItemCount, maxWeight, maxItemCount));

        return this;
    }

    private Behavior<FridgeCommand> onConsumeProduct(ConsumeProduct msg) {
        Optional<Product> product = contents.keySet().stream()
                .filter(p -> p.getId().equals(msg.productId))
                .findFirst();

        if (product.isEmpty()) {
            msg.replyTo.tell(new ConsumeResponse(false, "Product not found in fridge"));
            return this;
        }

        Product p = product.get();
        int currentQuantity = contents.get(p);

        if (currentQuantity < msg.quantity) {
            msg.replyTo.tell(new ConsumeResponse(false, "Not enough " + p.getName() + " in fridge. Available: " + currentQuantity));
            return this;
        }

        int newQuantity = currentQuantity - msg.quantity;

        if (newQuantity == 0) {
            contents.remove(p);
            getContext().getLog().info("Consumed last {} x {}", msg.quantity, p.getName());

            getContext().getSelf().tell(new AutoReorder(p, AUTO_REORDER_QUANTITY));
        } else {
            contents.put(p, newQuantity);
            getContext().getLog().info("Consumed {} x {}. Remaining: {}", msg.quantity, p.getName(), newQuantity);

            if (newQuantity <= AUTO_REORDER_THRESHOLD) {
                getContext().getSelf().tell(new AutoReorder(p, AUTO_REORDER_QUANTITY));
            }
        }

        msg.replyTo.tell(new ConsumeResponse(true, "Consumed " + msg.quantity + " x " + p.getName()));

        return this;
    }

    private Behavior<FridgeCommand> onOrderProducts(OrderProducts msg) {
        getContext().getLog().info("Processing order request for {} items", msg.items);

        double orderWeight = msg.items.stream()
                .mapToDouble(i -> i.getProduct().getWeight() * i.getQuantity())
                .sum();

        int orderItemCount = msg.items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        double currentWeight = calculateCurrentWeight();
        int currentItemCount = calculateCurrentItemCount();

        if (currentWeight + orderWeight > maxWeight) {
            msg.replyTo.tell(new OrderResponse(false,
                    "Order too heavy. Current " + currentWeight + "kg, Order:" + orderWeight +
                            "kg, Max: " + maxWeight + "kg", Optional.empty()));
            return this;
        }

        if (currentItemCount + orderItemCount > maxItemCount) {
            msg.replyTo.tell(new OrderResponse(false,
                    "Not enough space. Current " + currentItemCount + "items, Order:" + orderItemCount +
                            "items, Max: " + maxItemCount + "items", Optional.empty()));
            return this;
        }

        Order order = new Order(msg.items);
        orderHistory.add(order);

        getContext().getLog().info("Order validated, sending to external processor: {}", order.getId());

        CompletionStage<Receipt> receiptFuture = orderProcessorClient.processOrder(order);

        receiptFuture.thenAccept(receipt -> {
            getContext().getSelf().tell(new OrderCompleted(receipt, msg.replyTo));
                }).exceptionally(ex -> {
                    getContext().getLog().error("Error processing failed: {}", ex.getMessage());
                    msg.replyTo.tell(new OrderResponse(false,
                            "Order processing failed: " + ex.getMessage(), Optional.empty()));
                    return null;
        });

        return this;
    }

    private Behavior<FridgeCommand> onOrderCompleted(OrderCompleted msg) {
        Receipt receipt = msg.receipt;
        getContext().getLog().info("Order completed, receipt received: {}", receipt.getOrderId());

        receiptHistory.add(receipt);

        for (OrderItem item : receipt.getItems()) {
            addToContents(item.getProduct(), item.getQuantity());
        }

        msg.originalReplyTo.tell(new OrderResponse(true, "Order processed successfully", Optional.of(receipt)));

        return this;
    }

    private Behavior<FridgeCommand> onGetOrderHistory(GetOrderHistory msg) {
        msg.replyTo.tell(new OrderHistoryResponse(orderHistory, receiptHistory));
        return this;
    }

    private Behavior<FridgeCommand> onAutoReorder(AutoReorder msg) {
        getContext().getLog().info("Auto-reordering {} x {}", msg.quantityToOrder, msg.product.getName());

        List<OrderItem> items = Collections.singletonList(new OrderItem(msg.product, msg.quantityToOrder));

        ActorRef<OrderResponse> autoReorderAdapter = getContext().messageAdapter(
                OrderResponse.class,
                response -> {
                    if (response.isSuccess()) {
                        getContext().getLog().info("Auto-reordering successful for {}", msg.product.getName());
                    } else {
                        getContext().getLog().warn("Auto-reordering failed for {}: {}", msg.product.getName(), response.getMessage());
                    }

                    return new FridgeCommand() {};
                });

        getContext().getSelf().tell(new OrderProducts(items, autoReorderAdapter));

        return this;
    }

    private void addToContents(Product product, int quantity) {
        contents.merge(product, quantity, Integer::sum);
    }

    private double calculateCurrentWeight() {
        return contents.entrySet().stream()
                .mapToDouble(e -> e.getKey().getWeight() * e.getValue())
                .sum();
    }

    private int calculateCurrentItemCount() {
        return contents.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    private Fridge onPostStop() {
        getContext().getLog().info("Fridge {} actor stopped", identifier);
        return this;
    }
}
