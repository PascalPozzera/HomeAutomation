package at.fhv.sysarch.lab2.homeautomation.orderprocessor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import at.fhv.sysarch.lab2.homeautomation.devices.fridge.grpc.*;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OrderProcessorServer {

    private Server server;
    private final int port = 8101;

    public static void main(String[] args) throws Exception {
        ActorSystem<OrderProcessorActor.Command> system = ActorSystem.create(
                OrderProcessorActor.create(),
                "OrderProcessorServer");

        final OrderProcessorServer server = new OrderProcessorServer(system);
        server.start();

        System.out.println("Order processor server started on port " + server.port + ". Press ENTER to exit...");
        System.in.read();

        server.stop();
        system.terminate();
    }

    private OrderProcessorServer(ActorSystem<OrderProcessorActor.Command> system) {
        ActorRef<OrderProcessorActor.Command> orderProcessorActor = system;

        server = ServerBuilder.forPort(port)
                .addService(new OrderProcessorServiceImpl(orderProcessorActor, system))
                .build();
    }

    private void start() throws IOException {
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                OrderProcessorServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
        }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }
}

class OrderProcessorServiceImpl extends OrderProcessorGrpc.OrderProcessorImplBase {
    private final ActorRef<OrderProcessorActor.Command> orderProcessor;
    private final ActorSystem<?> system;

    public OrderProcessorServiceImpl(ActorRef<OrderProcessorActor.Command> orderProcessor, ActorSystem<?> system) {
        this.orderProcessor = orderProcessor;
        this.system = system;
    }

    @Override
    public void processOrder(OrderRequest request, StreamObserver<ReceiptResponse> responseObserver) {
        CompletableFuture<ReceiptResponse> future = new CompletableFuture<>();

        orderProcessor.tell(new OrderProcessorActor.ProcessOrderCommand(request, future));

        future.thenAccept(response -> {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }).exceptionally(ex -> {
            responseObserver.onError(ex);
            return null;
        });
    }
}

class OrderProcessorActor extends AbstractBehavior<OrderProcessorActor.Command> {

    public interface Command {}

    public static final class ProcessOrderCommand implements Command {
        final OrderRequest request;
        final CompletableFuture<ReceiptResponse> replyTo;

        public ProcessOrderCommand(OrderRequest request, CompletableFuture<ReceiptResponse> replyTo) {
            this.request = request;
            this.replyTo = replyTo;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(OrderProcessorActor::new);
    }

    private OrderProcessorActor(ActorContext<Command> context) {
        super(context);
        getContext().getLog().info("Order Processor Actor started");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ProcessOrderCommand.class, this::onProcessOrder)
                .build();
    }

    private Behavior<Command> onProcessOrder(ProcessOrderCommand cmd) {
        OrderRequest request = cmd.request;
        getContext().getLog().info("Processing order: {}", request.getOrderId());

        try {
            BigDecimal totalPrice = BigDecimal.ZERO;
            for (OrderItemProto item : request.getItemsList()) {
                BigDecimal itemPrice = new BigDecimal(item.getProduct().getPrice());
                BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
                totalPrice = totalPrice.add(itemPrice.multiply(quantity));
            }

            // Processing fee
            totalPrice = totalPrice.add(new BigDecimal("1.99"));

            ReceiptResponse response = ReceiptResponse.newBuilder()
                    .setOrderId(request.getOrderId())
                    .setTimestamp(request.getTimestamp())
                    .addAllItems(request.getItemsList())
                    .setTotalPrice(totalPrice.toString())
                    .build();

            getContext().getLog().info("Order {} processed successfully. Total: {}",
                    request.getOrderId(), totalPrice);

            cmd.replyTo.complete(response);

        } catch (Exception e) {
            getContext().getLog().error("Failed to process order: {}", e.getMessage());
            cmd.replyTo.completeExceptionally(e);
        }

        return this;
    }
}