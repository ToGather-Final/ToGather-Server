package com.example.pay_service.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
public class PaymentSession implements Serializable {
    private String id;
    private UUID merchantId;
    private UUID merchantAccountId;
    private Long suggestedAmount;
    private boolean used;
}
