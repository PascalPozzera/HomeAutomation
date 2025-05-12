package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Receipt {
    private final String orderId;
    private final LocalDateTime timestamp;
    private final List<OrderItem> items;
    private final BigDecimal totalPrice;

    public Receipt(String orderId, LocalDateTime timestamp, List<OrderItem> items, BigDecimal totalPrice) {
        this.orderId = orderId;
        this.timestamp = timestamp;
        this.items = new ArrayList<>(items);
        this.totalPrice = totalPrice;
    }

    public Receipt(Order order) {
        this(order.getId(), order.getTimestamp(), order.getItems(), order.getTotalPrice());
    }

    public String getOrderId() {
        return orderId;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== RECEIPT =====\n");
        sb.append("Order ID: ").append(orderId).append("\n");
        sb.append("Date: ").append(timestamp).append("\n");
        sb.append("Items:\n");

        for (OrderItem item : items) {
            Product product = item.getProduct();
            sb.append("   ").append(item.getQuantity())
                    .append("x ").append(product.getName())
                    .append(" @ ").append(product.getPrice())
                    .append("€ = ").append(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .append("€\n");
        }

        sb.append("Total: ").append(totalPrice).append("@\n");
        sb.append("===================\n");
        return sb.toString();
    }
}
