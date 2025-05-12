package at.fhv.sysarch.lab2.homeautomation.devices.fridge;

import java.math.BigDecimal;
import java.util.UUID;

public class Product {
    private final String id;
    private final String name;
    private final BigDecimal price;
    private final double weight;

    public Product(String id, String name, BigDecimal price, double weight) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.weight = weight;
    }

    public Product(String name, BigDecimal price, double weight) {
        this(UUID.randomUUID().toString(), name, price, weight);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name + " (" + price + "€, " + weight + " kg";
    }
}
