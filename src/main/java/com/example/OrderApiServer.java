package com.example;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.*;

public class OrderApiServer extends AbstractVerticle {
    private final Map<String, Order> orderStore = new HashMap<>();

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new OrderApiServer());
    }

    @Override
    public void start(){
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.post("/orders").handler(this::handleCreateOrder);
        router.get("/orders/:id").handler(this::handleGetOrder);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()){
                        System.out.println("HTTP server running on port 8080");
                    }else{
                        System.out.println("System failed to start: " + http.cause());
                    }
                });
    }

    private void handleCreateOrder(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        if (body == null || !body.containsKey("customerID") || !body.containsKey("items")) {
            ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("error", "Missing Fields").encode());
            return;
        }

        String id = UUID.randomUUID().toString();
        int customerID = body.getInteger("customerID");
        List<String> items = body.getJsonArray("items").getList();

        Order order = new Order(id, customerID, items);
        orderStore.put(id, order);

        System.out.println("📦 New Order Received: " + order);

        OrderProcessor.processOrder(order)
                .subscribe(
                        result -> {
                            System.out.println("Order processed: " + result);
                            ctx.response()
                                    .putHeader("Content-Type", "application/json")
                                    .end(new JsonObject().put("orderId", id).encode());
                        },
                        error -> {
                            System.err.println("Error processing order: " + error.getMessage());
                            ctx.response()
                                    .setStatusCode(500)
                                    .putHeader("Content-Type", "application/json")
                                    .end(new JsonObject().put("error", "Order processing failed").encode());
                        }
                );
    }


    private void handleGetOrder(RoutingContext ctx){
        String id = ctx.pathParam("id");
        Order order = orderStore.get(id);

        if (order == null){
            ctx.response().setStatusCode(404).setStatusMessage("Order Not Found!");
            return;
        }

        JsonObject json = new JsonObject()
                .put("id", order.id)
                .put("customerID", order.customerID)
                .put("items", order.items);

        ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(json.encode());
    }
}
