package com.example.trading_service.event;

import com.example.trading_service.domain.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * 거래 체결 이벤트
 * - 개인 거래나 그룹 거래 체결 시 발행되는 이벤트
 */
@Getter
public class TradeExecutedEvent extends ApplicationEvent {
    
    private final Order order;
    private final float executionPrice;
    private final UUID groupId;
    
    public TradeExecutedEvent(Object source, Order order, float executionPrice) {
        super(source);
        this.order = order;
        this.executionPrice = executionPrice;
        this.groupId = order.getGroupId();
    }
}
