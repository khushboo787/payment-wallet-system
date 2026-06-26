package com.paymentwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableCaching
@EnableKafka
public class PaymentWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentWalletApplication.class, args);
    }
}
