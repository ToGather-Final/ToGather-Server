package com.example.pay_service.client;

import com.example.module_common.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${app.services.user-service.url:http://localhost:8082}")
public interface UserServiceClient {

    @GetMapping("/internal/users/{userId}")
    UserInfo getUserInfo(@PathVariable("userId") UUID userId);

    @GetMapping("/internal/groups/{groupId}/members/{userId}")
    Boolean isGroupMember(@PathVariable("groupId") UUID groupId, @PathVariable("userId") UUID userId);
}
