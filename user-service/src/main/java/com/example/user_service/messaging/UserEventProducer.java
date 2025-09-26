package com.example.user_service.messaging;

import com.example.user_service.dto.UserDto;
import com.example.module_common.dto.UserEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserEventProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String USER_EXCHANGE = "user.exchange";
    private static final String USER_CREATED_ROUTING_KEY = "user.created";
    private static final String USER_UPDATED_ROUTING_KEY = "user.updated";
    private static final String USER_DELETED_ROUTING_KEY = "user.deleted";

    public void sendUserCreatedEvent(UserDto userDto) {
        UserEvent event = new UserEvent("USER_CREATED", userDto.getId(), userDto.getUsername(), userDto.getEmail());
        rabbitTemplate.convertAndSend(USER_EXCHANGE, USER_CREATED_ROUTING_KEY, event);
        System.out.println("사용자 생성 이벤트 발행: " + event);
    }

    public void sendUserUpdatedEvent(UserDto userDto) {
        UserEvent event = new UserEvent("USER_UPDATED", userDto.getId(), userDto.getUsername(), userDto.getEmail());
        rabbitTemplate.convertAndSend(USER_EXCHANGE, USER_UPDATED_ROUTING_KEY, event);
        System.out.println("사용자 업데이트 이벤트 발행: " + event);
    }

    public void sendUserDeletedEvent(UserDto userDto) {
        UserEvent event = new UserEvent("USER_DELETED", userDto.getId(), userDto.getUsername(), userDto.getEmail());
        rabbitTemplate.convertAndSend(USER_EXCHANGE, USER_DELETED_ROUTING_KEY, event);
        System.out.println("사용자 삭제 이벤트 발행: " + event);
    }
}
