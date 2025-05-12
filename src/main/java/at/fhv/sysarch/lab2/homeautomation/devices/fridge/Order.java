package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order {
    private final String id;
    private final LocalDateTime timestamp;
    private final List<OrderItem> items;
    private BigDecimal totalPrice;

    public Order(String id, LocalDateTime timestamp, List<OrderItem> items) {
        this.id = id;
        this.timestamp = timestamp;
        this.items = items;
        this.totalPrice = calculateTotalPrice();
    }

    public Order(List<OrderItem> items) {
        this(UUID.randomUUID().toString(), LocalDateTime.now(), items);
    }

    private BigDecimal calculateTotalPrice() {
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public double getTotalWeight() {
        return items.stream()
                .mapToDouble(item -> item.getProduct().getWeight() * item.getQuantity())
                .sum();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", items=" + items +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
