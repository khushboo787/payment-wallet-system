package com.paymentwallet.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${app.kafka.topic.payment-notification}")
    private String paymentNotificationTopic;

    @Value("${app.kafka.topic.payment-completed}")
    private String paymentCompletedTopic;

    public void sendPaymentNotification(PaymentEvent event) {
        log.info("Sending payment notification for referenceId: {}", event.getReferenceId());
        CompletableFuture<SendResult<String, PaymentEvent>> future =
                kafkaTemplate.send(paymentNotificationTopic, event.getReferenceId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send payment notification for referenceId: {}", event.getReferenceId(), ex);
            } else {
                log.info("Payment notification sent successfully for referenceId: {} | partition: {} | offset: {}",
                        event.getReferenceId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    public void sendPaymentCompleted(PaymentEvent event) {
        log.info("Sending payment completed event for referenceId: {}", event.getReferenceId());
        kafkaTemplate.send(paymentCompletedTopic, event.getReferenceId(), event);
    }
}
