package com.example.user_service.controller;

import com.example.user_service.dto.GroupCreateRequest;
import com.example.user_service.dto.GroupIdResponse;
import com.example.user_service.dto.GroupMemberAddRequest;
import com.example.user_service.dto.GroupMemberSimple;
import com.example.user_service.dto.GroupRuleResponse;
import com.example.user_service.dto.GroupRuleUpdateRequest;
import com.example.user_service.dto.GroupSummaryResponse;
import com.example.user_service.dto.InvitationCodeResponse;
import com.example.user_service.domain.Group;
import com.example.user_service.domain.GroupMember;
import com.example.user_service.domain.GroupRule;
import com.example.user_service.service.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupSummaryResponse> getGroup(@PathVariable UUID groupId,
                                                         Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        Group group = groupService.getDetail(groupId, userId);
        GroupSummaryResponse body = new GroupSummaryResponse(group.getGroupId(), group.getGroupName());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<GroupSummaryResponse>> getMyGroups(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<Group> groups = groupService.findMyGroups(userId);
        List<GroupSummaryResponse> body = groups.stream()
                .map(g -> new GroupSummaryResponse(g.getGroupId(), g.getGroupName())).toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberSimple>> getMembers(@PathVariable UUID groupId,
                                                              Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<GroupMember> members = groupService.members(groupId, userId);
        UUID ownerId = groupService.getOwnerId(groupId);
        List<GroupMemberSimple> body = members.stream()
                .map(m -> new GroupMemberSimple(m.getUserId(), resolveRole(m.getUserId(), ownerId))).toList();
        return ResponseEntity.ok(body);
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

    @GetMapping("/{groupId}/rules")
    public ResponseEntity<GroupRuleResponse> getRule(@PathVariable UUID groupId,
                                                     Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        GroupRule rule = groupService.getRule(groupId, userId);
        GroupRuleResponse body = new GroupRuleResponse(rule.getVoteQuorum(), rule.getVoteDurationHours());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/invites/{code}/accept")
    public ResponseEntity<Void> acceptInvite(@PathVariable UUID code,
                                             Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        groupService.acceptInvite(code, userId);
        return ResponseEntity.noContent().build();
    }

    private String resolveRole(UUID memberUserId, UUID ownerUserId) {
        if (memberUserId.equals(ownerUserId)) {
            return "OWNER";
        }
        return "MEMBER";
    }
}
