package com.example.user_service.service;

import com.example.user_service.dto.GroupCreateRequest;
import com.example.user_service.dto.GroupMemberAddRequest;
import com.example.user_service.dto.GroupRuleUpdateRequest;
import com.example.user_service.model.Group;
import com.example.user_service.model.GroupMember;
import com.example.user_service.model.GroupMemberId;
import com.example.user_service.model.GroupRule;
import com.example.user_service.model.InvitationCode;
import com.example.user_service.repository.GroupMemberRepository;
import com.example.user_service.repository.GroupRepository;
import com.example.user_service.repository.GroupRuleRepository;
import com.example.user_service.repository.InvitationCodeRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRuleRepository groupRuleRepository;
    private final InvitationCodeRepository invitationCodeRepository;

    @Transactional
    public UUID createGroup(UUID ownerId, GroupCreateRequest request) {
        validateCreate(request);
        Group group = Group.create(request.groupName(), ownerId, request.goalAmount());
        Group saved = groupRepository.save(group);
        GroupMember leader = GroupMember.join(saved.getGroupId(), ownerId);
        groupMemberRepository.save(leader);
        return saved.getGroupId();
    }

    @Transactional
    public void addMember(UUID groupId, GroupMemberAddRequest request, UUID operatorId) {
        assertOperatorIsOwner(groupId, operatorId);
        assertNotDuplicate(groupId, request.userId());
        GroupMember member = GroupMember.join(groupId, request.userId());
        groupMemberRepository.save(member);
    }

    @Transactional
    public void updateRule(UUID groupId, GroupRuleUpdateRequest request, UUID operatorId) {
        assertOperatorIsOwner(groupId, operatorId);
        validateRule(request);
        GroupRule rule = GroupRule.of(groupId, request.voteQuorum(), request.voteDurationHours());
        groupRuleRepository.save(rule);
    }

    @Transactional
    public UUID issueInvitation(UUID groupId, UUID operatorId) {
        assertOperatorIsOwner(groupId, operatorId);
        InvitationCode invitation = InvitationCode.issue(groupId);
        InvitationCode saved = invitationCodeRepository.save(invitation);
        return saved.getCode();
    }

    private void validateCreate(GroupCreateRequest request) {
        if (request == null) {
            throw  new IllegalArgumentException("요청이 비었습니다.");
        }
        if (request.groupName() == null || request.groupName().isBlank()) {
            throw new IllegalArgumentException("그룹명을 입력하세요.");
        }
        if (request.goalAmount() == null || request.goalAmount() <= 0) {
            throw new IllegalArgumentException("목표 금액은 0원 이상이여야 합니다.");
        }
        if (request.initialAmount() != null && request.initialAmount() < 0) {
            throw new IllegalArgumentException("초기 금액이 올바르지 않습니다.");
        }
    }

    private void validateRule(GroupRuleUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청이 비었습니다.");
        }
        if (request.voteQuorum() == null || request.voteQuorum() <= 0) {
            throw new IllegalArgumentException("투표 찬성 인원수는 0 이상이여야 합니다.");
        }
        if (request.voteDurationHours() == null || request.voteDurationHours() <= 0) {
            throw new IllegalArgumentException("투표 가능 시간은 0 이상이여야 합니다.");
        }
    }

    private void assertNotDuplicate(UUID groupId, UUID userId) {
        GroupMemberId id = new GroupMemberId(userId, groupId);
        boolean exists = groupMemberRepository.existsById(id);
        if (exists) {
            throw new IllegalArgumentException("이미 그룹에 존재하는 멤버입니다.");
        }
    }

    private void assertOperatorIsOwner(UUID groupId, UUID operatorId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        boolean isOwner = group.getOwnerId().equals(operatorId);
        if (!isOwner) {
            throw new IllegalArgumentException("그룹장만 수행할 수 있습니다.");
        }
    }
}
