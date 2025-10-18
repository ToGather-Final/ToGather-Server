package com.example.user_service.controller;

import com.example.user_service.dto.*;
import com.example.user_service.domain.Group;
import com.example.user_service.domain.GroupMember;
import com.example.user_service.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
@RestController
@RequestMapping("/groups")
@Tag(name = "그룹 관리", description = "그룹 생성, 조회, 수정, 삭제 관련 API")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @Operation(summary = "그룹 상세 정보 조회", description = "특정 그룹의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupSummaryResponse> getGroup(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        Group group = groupService.getDetail(groupId, userId);

        List<GroupMember> members = groupService.members(groupId, userId);
        int currentMembers = members.size();

        String invitationCode = groupService.getCurrentInvitationCode(groupId, userId);

        GroupSummaryResponse body = new GroupSummaryResponse(
                group.getGroupId(),
                group.getGroupName(),
                group.getMaxMembers(),
                currentMembers,
                group.getGoalAmount(),
                group.getInitialAmount(),
                invitationCode
        );

        return ResponseEntity.ok(body);
    }

    @Operation(summary = "내 그룹 목록 조회", description = "현재 사용자가 속한 모든 그룹의 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/mine")
    public ResponseEntity<List<GroupSimpleResponse>> getMyGroups(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<Group> groups = groupService.findMyGroups(userId);
        List<GroupSimpleResponse> body = groups.stream()
                .map(g -> new GroupSimpleResponse(g.getGroupId(), g.getGroupName())).toList();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "그룹 멤버 목록 조회", description = "특정 그룹의 모든 멤버 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "멤버 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberSimple>> getMembers(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<GroupMember> members = groupService.members(groupId, userId);
        UUID ownerId = groupService.getOwnerId(groupId);
        List<GroupMemberSimple> body = members.stream()
                .map(m -> new GroupMemberSimple(m.getUserId(), resolveRole(m.getUserId(), ownerId))).toList();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "그룹 생성", description = "새로운 투자 그룹을 생성합니다. 그룹 생성 시 초대 코드도 함께 발급됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "그룹 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<GroupCreateResponse> create(
            @Parameter(description = "그룹 생성 요청 데이터", required = true) @Valid @RequestBody GroupCreateRequest request,
            Authentication authentication) {
        UUID ownerId = (UUID) authentication.getPrincipal();
        UUID groupId = groupService.createGroup(ownerId, request);

        String invitationCode = groupService.issueInvitation(groupId, ownerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(new GroupCreateResponse(groupId, invitationCode));
    }

    @Operation(summary = "그룹 멤버 추가", description = "그룹에 새로운 멤버를 추가합니다. (그룹장만 가능)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "멤버 추가 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 그룹이 이미 활성화됨"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PostMapping("/{groupId}/members")
    public ResponseEntity<Void> addMember(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "멤버 추가 요청 데이터", required = true) @Valid @RequestBody GroupMemberAddRequest request,
            Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        groupService.addMember(groupId, request, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "초대 코드 발급", description = "그룹 초대를 위한 초대 코드를 새로 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "초대 코드 발급 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PostMapping("/{groupId}/invites")
    public ResponseEntity<InvitationCodeResponse> issueInvite(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        String code = groupService.issueInvitation(groupId, operatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new InvitationCodeResponse(code));
    }

    @Operation(summary = "목표 금액 수정", description = "그룹의 목표 금액을 수정합니다. (그룹장만 가능)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "목표 금액 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PutMapping("/{groupId}/goal-amount")
    public ResponseEntity<Void> updateGoalAmount(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "목표 금액 수정 요청 데이터", required = true) @Valid @RequestBody GoalAmountUpdateRequest request,
            Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        groupService.updateGoalAmount(groupId, request.goalAmount(),operatorId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "투표 정족수 수정", description = "그룹의 투표 정족수와 해체 정족수를 수정합니다. (그룹장만 가능)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "정족수 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PutMapping("/{groupId}/quorum")
    public ResponseEntity<Void> updateQuorum(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId,
            @Parameter(description = "정족수 수정 요청 데이터", required = true) @Valid @RequestBody QuorumUpdateRequest request,
            Authentication authentication) {
        UUID operatorId = (UUID) authentication.getPrincipal();
        groupService.updateQuorumSettings(groupId, request.voteQuorum(), request.dissolutionQuorum(), operatorId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "초대 수락", description = "초대 코드를 사용하여 그룹에 참여합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "초대 수락 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 초대 코드 또는 이미 만료됨"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "초대 코드를 찾을 수 없음")
    })
    @PostMapping("/invites/{code}/accept")
    public ResponseEntity<InviteAcceptResponse> acceptInvite(
            @Parameter(description = "초대 코드", required = true) @PathVariable String code,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        InviteAcceptResponse response = groupService.acceptInvite(code, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "그룹 상태 확인", description = "그룹의 현재 상태(대기중/활성화), 현재 멤버 수, 최대 멤버 수를 확인합니다. 클라이언트에서 대기중 화면 표시 여부를 결정할 때 사용합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 상태 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "그룹 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupId}/status")
    public ResponseEntity<GroupStatusResponse> getGroupStatus(
            @Parameter(description = "그룹 ID", required = true) @PathVariable UUID groupId, 
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        GroupStatusResponse response = groupService.getGroupStatus(groupId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 그룹들 상태 확인", description = "현재 사용자가 속한 모든 그룹의 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "그룹 상태 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
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

    private String resolveRole(UUID memberUserId, UUID ownerUserId) {
        if (memberUserId.equals(ownerUserId)) {
            return "OWNER";
        }
        return "MEMBER";
    }
}
