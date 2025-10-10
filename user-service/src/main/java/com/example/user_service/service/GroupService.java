package com.example.user_service.service;

import com.example.user_service.dto.GroupCreateRequest;
import com.example.user_service.dto.GroupMemberAddRequest;
import com.example.user_service.dto.GroupRuleUpdateRequest;
import com.example.user_service.domain.Group;
import com.example.user_service.domain.GroupMember;
import com.example.user_service.domain.GroupMemberId;
import com.example.user_service.domain.GroupRule;
import com.example.user_service.domain.InvitationCode;
import com.example.user_service.repository.GroupMemberRepository;
import com.example.user_service.repository.GroupRepository;
import com.example.user_service.repository.GroupRuleRepository;
import com.example.user_service.repository.InvitationCodeRepository;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

    @Transactional
    public void acceptInvite(UUID code, UUID userId) {
        InvitationCode invitationCode = invitationCodeRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("초대 코드를 찾을 수 없습니다."));

        validateInvitationAcceptable(invitationCode);

        GroupMemberId groupMemberId = new GroupMemberId(userId, invitationCode.getGroupId());
        boolean isAlreadyMember = groupMemberRepository.existsById(groupMemberId);
        if (!isAlreadyMember) {
            groupMemberRepository.save(GroupMember.join(invitationCode.getGroupId(), userId));
        }

        invitationCode.expire();
        invitationCodeRepository.save(invitationCode);
    }

    @Transactional(TxType.SUPPORTS)
    public Group getDetail(UUID groupId, UUID userId) {
        assertMember(groupId, userId);
        return groupRepository.findById(groupId).orElseThrow(() -> new NoSuchElementException("그룹이 없습니다."));
    }

    @Transactional(TxType.SUPPORTS)
    public List<Group> findMyGroups(UUID userId) {
        return groupRepository.findAllByMember(userId);
    }

    @Transactional(TxType.SUPPORTS)
    public List<GroupMember> members(UUID groupId, UUID userId) {
        assertMember(groupId, userId);
        return groupMemberRepository.findByIdGroupId(groupId);
    }

    @Transactional(TxType.SUPPORTS)
    public UUID getOwnerId(UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다."));
        return group.getOwnerId();
    }

    @Transactional(TxType.SUPPORTS)
    public GroupRule getRule(UUID groupId, UUID requesterId) {
        assertMember(groupId, requesterId);
        return groupRuleRepository.findByGroupId(groupId).orElseThrow(() -> new NoSuchElementException("그룹 규칙이 없습니다."));
    }

    private void assertMember(UUID groupId, UUID userId) {
        boolean ok = groupMemberRepository.existsByIdGroupIdAndIdUserId(groupId, userId);
        if (ok) {
            return;
        }
        throw new AccessDeniedException("그룹 멤버가 아닙니다.");
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

    private void validateInvitationAcceptable(InvitationCode invitationCode) {
        if (invitationCode.isExpired()) {
            throw new IllegalArgumentException("이미 만료된 초대 코드입니다.");
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
