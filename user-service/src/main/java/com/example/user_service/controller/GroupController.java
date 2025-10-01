package com.example.user_service.controller;

import com.example.user_service.dto.GroupCreateRequest;
import com.example.user_service.dto.GroupIdResponse;
import com.example.user_service.dto.GroupMemberAddRequest;
import com.example.user_service.dto.GroupRuleUpdateRequest;
import com.example.user_service.dto.InvitationCodeResponse;
import com.example.user_service.service.GroupService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/group")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupIdResponse> create(@Valid @RequestBody GroupCreateRequest request,
                                                  Authentication authentication) {
        UUID ownerId = (UUID) authentication.getPrincipal();
        UUID groupId = groupService.createGroup(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GroupIdResponse(groupId));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<Void> addMember(@PathVariable UUID groupId,
                                          @Valid @RequestBody GroupMemberAddRequest request,
                                          Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        groupService.addMember(groupId, request, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{groupId}/rules")
    public ResponseEntity<Void> updateRule(@PathVariable UUID groupId,
                                           @Valid @RequestBody GroupRuleUpdateRequest request,
                                           Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        groupService.updateRule(groupId, request, operatorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/invites")
    public ResponseEntity<InvitationCodeResponse> issueInvite(@PathVariable UUID groupId,
                                                              Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        UUID code = groupService.issueInvitation(groupId, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new InvitationCodeResponse(code));
    }
}
