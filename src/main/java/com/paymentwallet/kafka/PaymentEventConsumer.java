package com.paymentwallet.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentEventConsumer {

    @KafkaListener(
            topics = "${app.kafka.topic.payment-notification}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentNotification(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received payment notification | topic: {} | partition: {} | offset: {} | referenceId: {}",
                topic, partition, offset, event.getReferenceId());

        processPaymentNotification(event);
    }

    @KafkaListener(
            topics = "${app.kafka.topic.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentCompleted(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Payment completed event received | topic: {} | referenceId: {} | status: {}",
                topic, event.getReferenceId(), event.getStatus());

        processPaymentCompleted(event);
    }

    private void processPaymentNotification(PaymentEvent event) {
        log.info("Processing payment notification: {} | amount: {} | from: {} to: {}",
                event.getReferenceId(), event.getAmount(),
                event.getSourceWalletNumber(), event.getDestinationWalletNumber());
        // Email/SMS notification logic would go here
    }

    private void processPaymentCompleted(PaymentEvent event) {
        log.info("Processing completed payment: {} | status: {}",
                event.getReferenceId(), event.getStatus());
        // Post-processing logic (loyalty points, analytics) would go here
    }
}
