package com.example;

import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.TimeUnit;

public class OrderProcessor {
    // Used in Vertx handler
    public static Single<String> processOrder(Order order){
        return simulateKafkaPublish(order)
                .flatMap(result -> simulateRedisUpdate(order));
    }

    // Kafka Publishing
    public static Single<String> simulateKafkaPublish(Order order){
        return Single.just("Kafka Published for " + order.id)
                .delay(300, TimeUnit.MILLISECONDS);
    }

    // Redis Update
    public static Single<String> simulateRedisUpdate(Order order){
        return Single.just("Redis Updated for " + order.id)
                .delay(200, TimeUnit.MILLISECONDS);
    }
}
