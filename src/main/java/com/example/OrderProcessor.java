package com.example;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.time.Instant;

public class OrderProcessor {

    public static Single<String> emitOrderEvent(Order order, String status, KafkaProducer<String, String> producer) {
        JsonObject event = new JsonObject()
                .put("orderId", order.id)
                .put("customerID", order.customerID)
                .put("items", order.items)
                .put("status", status)
                .put("timestamp", Instant.now().toString());

        KafkaProducerRecord<String, String> record =
                KafkaProducerRecord.create("orders", order.id, event.encode());

        return Single.create(emitter ->
                producer.send(record, ar -> {
                    if (ar.succeeded()) {
                        emitter.onSuccess("Kafka event sent: " + event.encode());
                    } else {
                        emitter.onError(ar.cause());
                    }
                })
        );
    }
}
