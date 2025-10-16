package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.domain.Group;
import com.example.user_service.domain.GroupMember;
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

        List<GroupMember> members = groupService.members(groupId, userId);
        int currentMembers = members.size();

        GroupSummaryResponse body = new GroupSummaryResponse(
                group.getGroupId(),
                group.getGroupName(),
                group.getMaxMembers(),
                currentMembers,
                group.getGoalAmount(),
                group.getInitialAmount()
        );

        return ResponseEntity.ok(body);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<GroupSimpleResponse>> getMyGroups(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<Group> groups = groupService.findMyGroups(userId);
        List<GroupSimpleResponse> body = groups.stream()
                .map(g -> new GroupSimpleResponse(g.getGroupId(), g.getGroupName())).toList();
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
    public ResponseEntity<GroupCreateResponse> create(@Valid @RequestBody GroupCreateRequest request,
                                                  Authentication authentication) {
        UUID ownerId = (UUID) authentication.getPrincipal();
        UUID groupId = groupService.createGroup(ownerId, request);

        String invitationCode = groupService.issueInvitation(groupId, ownerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(new GroupCreateResponse(groupId, invitationCode));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<Void> addMember(@PathVariable UUID groupId,
                                          @Valid @RequestBody GroupMemberAddRequest request,
                                          Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        groupService.addMember(groupId, request, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{groupId}/invites")
    public ResponseEntity<InvitationCodeResponse> issueInvite(@PathVariable UUID groupId,
                                                              Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        String code = groupService.issueInvitation(groupId, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new InvitationCodeResponse(code));
    }

    @PutMapping("/{groupId}/goal-amount")
    public ResponseEntity<Void> updateGoalAmount(@PathVariable UUID groupId,
                                                 @Valid @RequestBody GoalAmountUpdateRequest request,
                                                 Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        groupService.updateGoalAmount(groupId, request.goalAmount(),operatorId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{groupId}/quorum")
    public ResponseEntity<Void> updateQuorum(@PathVariable UUID groupId,
             @Valid @RequestBody QuorumUpdateRequest request,
                                             Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        groupService.updateQuorumSettings(groupId, request.voteQuorum(), request.dissolutionQuorum(), operatorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invites/{code}/accept")
    public ResponseEntity<Void> acceptInvite(@PathVariable String code,
                                             Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        groupService.acceptInvite(code, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/status")
    public ResponseEntity<GroupStatusResponse> getGroupStatus(@PathVariable UUID groupId, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        GroupStatusResponse response = groupService.getGroupStatus(groupId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine/status")
    public ResponseEntity<List<GroupStatusResponse>> getMyGroupStatus(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<GroupStatusResponse> response = groupService.getMyGroupsStatus(userId);
        return ResponseEntity.ok(response);
    }

    // 시스템용 조회 기능
    @GetMapping("/internal/{groupId}/settings")
    public ResponseEntity<GroupSettingsResponse> getGroupSettingsInternal(@PathVariable UUID groupId) {
        GroupSettingsResponse response = groupService.getGroupSettingsInternal(groupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/internal/{groupId}/vote-quorum")
    public ResponseEntity<Integer> getVoteQuorumInternal(@PathVariable UUID groupId) {
        Integer voteQuorum = groupService.getVoteQuorumInternal(groupId);
        return ResponseEntity.ok(voteQuorum);
    }

    private String resolveRole(UUID memberUserId, UUID ownerUserId) {
        if (memberUserId.equals(ownerUserId)) {
            return "OWNER";
        }
        return "MEMBER";
    }
}
