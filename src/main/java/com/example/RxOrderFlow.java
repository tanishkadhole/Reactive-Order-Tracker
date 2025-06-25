package com.example;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Observable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RxOrderFlow {

    public static void main(String[] args) {

        // Incoming order stream
        Observable<Order> incomingOrders = Observable.fromIterable(List.of(
                new Order(UUID.randomUUID().toString(), 101, List.of("apple", "banana")),
                new Order(UUID.randomUUID().toString(), 102, List.of("grape", "mango"))
        ));

        // Publish to Kafka → Update Redis
        incomingOrders
                .flatMapSingle(order ->
                        simulateKafkaPublish(order)
                                .flatMap(result -> simulateRedisUpdate(order))
                )
                .blockingSubscribe(
                        result -> System.out.println("Flow complete: " + result),
                        error -> System.err.println("Error: " + error.getMessage())
                );
    }

    static Single<String> simulateKafkaPublish(Order order) {
        return Single.just("Kafka published for " + order.id)
                .delay(300, TimeUnit.MILLISECONDS);
    }

    static Single<String> simulateRedisUpdate(Order order) {
        return Single.just("Redis updated for " + order.id)
                .delay(200, TimeUnit.MILLISECONDS);
    }
}
