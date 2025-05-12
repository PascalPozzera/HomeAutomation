package at.fhv.sysarch.lab2.homeautomation.devices.fridge.grpc;

import akka.actor.typed.ActorSystem;
import at.fhv.sysarch.lab2.homeautomation.devices.fridge.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OrderProcessorClient {

    private final ManagedChannel channel;
    private final OrderProcessorGrpc.OrderProcessorStub asyncStub;
    private final ActorSystem<?> system;

    public OrderProcessorClient(String serverHost, ActorSystem<?> system) {
        this.system = system;

        this.channel = ManagedChannelBuilder.forAddress(serverHost, 8101)
                .usePlaintext()
                .build();

        this.asyncStub = OrderProcessorGrpc.newStub(channel);
    }

    public CompletionStage<Receipt> processOrder(Order order) {
        OrderRequest request = convertToOrderRequest(order);

        CompletableFuture<Receipt> receiptFuture = new CompletableFuture<>();

        asyncStub.processOrder(request, new io.grpc.stub.StreamObserver<ReceiptResponse>() {
            @Override
            public void onNext(ReceiptResponse response) {
                Receipt receipt = convertToReceipt(response);
                receiptFuture.complete(receipt);
            }

            @Override
            public void onError(Throwable t) {
                receiptFuture.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
            }
        });

        return receiptFuture;
    }

    private OrderRequest convertToOrderRequest(Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        List<OrderItemProto> itemProtos = order.getItems().stream()
                .map(this::convertToOrderItemProto)
                .collect(Collectors.toList());

        return OrderRequest.newBuilder()
                .setOrderId(order.getId())
                .setTimestamp(order.getTimestamp().format(formatter))
                .addAllItems(itemProtos)
                .build();
    }

    private OrderItemProto convertToOrderItemProto(OrderItem item) {
        Product product = item.getProduct();

        ProductProto productProto = ProductProto.newBuilder()
                .setId(product.getId())
                .setName(product.getName())
                .setPrice(product.getPrice().toString())
                .setWeight(product.getWeight())
                .build();

        return OrderItemProto.newBuilder()
                .setProduct(productProto)
                .setQuantity(item.getQuantity())
                .build();
    }

    private Receipt convertToReceipt(ReceiptResponse response) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime timestamp = LocalDateTime.parse(response.getTimestamp(), formatter);

        List<OrderItem> items = new ArrayList<>();

        for (OrderItemProto itemProto : response.getItemsList()) {
            ProductProto productProto = itemProto.getProduct();

            Product product = new Product(
                    productProto.getId(),
                    productProto.getName(),
                    new BigDecimal(productProto.getPrice()),
                    productProto.getWeight()
            );

            items.add(new OrderItem(product, itemProto.getQuantity()));
        }

        return new Receipt(
                response.getOrderId(),
                timestamp,
                items,
                new BigDecimal(response.getTotalPrice())
        );
    }
}