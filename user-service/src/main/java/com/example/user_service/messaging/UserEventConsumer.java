package com.example.user_service.messaging;

import com.example.module_common.dto.TradingEvent;
import com.example.module_common.dto.PaymentEvent;
import com.example.module_common.dto.VoteEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {

    @RabbitListener(queues = "trading.user.queue")
    public void handleTradingEvent(TradingEvent event) {
        System.out.println("User Service - 거래 이벤트 수신: " + event);
        // 거래 이벤트에 따른 사용자 관련 처리 로직
    }

    @RabbitListener(queues = "payment.user.queue")
    public void handlePaymentEvent(PaymentEvent event) {
        System.out.println("User Service - 결제 이벤트 수신: " + event);
        // 결제 이벤트에 따른 사용자 관련 처리 로직
    }

    @RabbitListener(queues = "vote.user.queue")
    public void handleVoteEvent(VoteEvent event) {
        System.out.println("User Service - 투표 이벤트 수신: " + event);
        // 투표 이벤트에 따른 사용자 관련 처리 로직
    }
}
