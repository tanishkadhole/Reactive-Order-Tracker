package com.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.kafka.client.producer.KafkaProducer;

import java.util.*;

public class OrderApiServer extends AbstractVerticle {
    private final Map<String, Order> orderStore = new HashMap<>();
    private KafkaProducer<String, String> kafkaProducer;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new OrderApiServer());
    }

    @Override
    public void start() {
        kafkaProducer = KafkaProducer.create(vertx, getKafkaConfig());

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.post("/orders").handler(this::handleCreateOrder);
        router.get("/orders/:id").handler(this::handleGetOrder);
        router.post("/orders/:id/status/:status").handler(this::handleStatusUpdate);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        System.out.println("🚀 HTTP server running on port 8080");
                    } else {
                        System.err.println("❌ Failed to start: " + http.cause());
                    }
                });
    }

    private void handleCreateOrder(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        if (body == null || !body.containsKey("customerID") || !body.containsKey("items")) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", "Missing Fields").encode());
            return;
        }

        String id = UUID.randomUUID().toString();
        int customerID = body.getInteger("customerID");
        List<String> items = body.getJsonArray("items").getList();

        Order order = new Order(id, customerID, items);
        orderStore.put(id, order);
        System.out.println("📦 New Order Created: " + order);

        OrderProcessor.emitOrderEvent(order, "created", kafkaProducer)
                .subscribe(
                        result -> {
                            System.out.println(result);
                            ctx.response()
                                    .putHeader("Content-Type", "application/json")
                                    .end(new JsonObject().put("orderId", id).encode());
                        },
                        error -> {
                            error.printStackTrace();
                            ctx.response().setStatusCode(500)
                                    .end(new JsonObject().put("error", "Kafka error").encode());
                        }
                );
    }

    private void handleGetOrder(RoutingContext ctx) {
        String id = ctx.pathParam("id");
        Order order = orderStore.get(id);

        if (order == null) {
            ctx.response().setStatusCode(404).end(new JsonObject().put("error", "Order not found").encode());
            return;
        }

        JsonObject json = new JsonObject()
                .put("id", order.id)
                .put("customerID", order.customerID)
                .put("items", order.items);

        ctx.response().putHeader("Content-Type", "application/json").end(json.encode());
    }

    private void handleStatusUpdate(RoutingContext ctx) {
        String id = ctx.pathParam("id");
        String status = ctx.pathParam("status");

        Order order = orderStore.get(id);
        if (order == null) {
            ctx.response().setStatusCode(404).end(new JsonObject().put("error", "Order not found").encode());
            return;
        }

        List<String> valid = List.of("paid", "shipped", "delivered");
        if (!valid.contains(status)) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid status").encode());
            return;
        }

        OrderProcessor.emitOrderEvent(order, status, kafkaProducer)
                .subscribe(
                        result -> {
                            System.out.println(result);
                            ctx.response().end(new JsonObject().put("status", status).encode());
                        },
                        error -> {
                            error.printStackTrace();
                            ctx.response().setStatusCode(500)
                                    .end(new JsonObject().put("error", "Kafka error").encode());
                        }
                );
    }

    private Map<String, String> getKafkaConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", "localhost:9092");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", "1");
        return config;
    }
}
