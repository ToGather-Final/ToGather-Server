package com.example.user_service.service;

import com.example.user_service.domain.*;
import com.example.user_service.dto.*;
import com.example.user_service.repository.GroupMemberRepository;
import com.example.user_service.repository.GroupRepository;
import com.example.user_service.repository.InvitationCodeRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final InvitationCodeRepository invitationCodeRepository;

    @Transactional
    public UUID createGroup(UUID ownerId, GroupCreateRequest request) {
        validateCreate(request);

        Group group = Group.create(
                request.groupName(),
                ownerId,
                request.goalAmount(),
                request.initialAmount(),
                request.maxMembers(),
                request.voteQuorum(),
                request.dissolutionQuorum()
        );
        Group saved = groupRepository.save(group);

        GroupMember leader = GroupMember.join(saved.getGroupId(), ownerId);
        groupMemberRepository.save(leader);

        return saved.getGroupId();
    }

    @Transactional
    public void updateGroupSettings(UUID groupId, GroupSettingsUpdateRequest request, UUID operatorId) {
        assertOperatorIsOwner(groupId, operatorId);

        Group group = groupRepository.findById(groupId).orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다."));

        boolean hasVoteQuorum = request.voteQuorum().isPresent();
        boolean hasDissolutionQuorum = request.dissolutionQuorum().isPresent();
        boolean hasGoalAmount = request.goalAmount().isPresent();

        if (!hasVoteQuorum && !hasDissolutionQuorum && !hasGoalAmount) {
            throw new IllegalArgumentException("수정할 사항이 없습니다.");
        }

        validateGroupSettings(groupId, request);

        group.updateSettings(request.voteQuorum(), request.dissolutionQuorum(), request.goalAmount());
        groupRepository.save(group);
    }

    @Transactional
    public void updateGoalAmount(UUID groupId, Integer goalAmount, UUID operatorId) {
        assertOperatorIsOwner(groupId, operatorId);

        if (goalAmount == null || goalAmount <= 0) {
            throw new IllegalArgumentException("목표 금액은 0원보다 커야 합니다.");
        }

        Group group = groupRepository.findById(groupId).orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다."));

        group.updateGoalAmount(goalAmount);
        groupRepository.save(group);
    }

    @Transactional
    public void updateQuorumSettings(UUID groupId, Integer voteQuorum, Integer dissolutionQuorum, UUID operatorId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다."));

        if (voteQuorum == null || voteQuorum <= 0) {
            throw new IllegalArgumentException("투표 찬성 인원수는 1명 이상이어야 합니다.");
        }
        if (dissolutionQuorum == null || dissolutionQuorum <= 0) {
            throw new IllegalArgumentException("그룹 해체 인원수는 1명 이상이어야 합니다.");
        }
        if (voteQuorum > group.getMaxMembers()) {
            throw new IllegalArgumentException("투표 찬성 인원수는 그룹 최대 인원을 초과할 수 없습니다.");
        }
        if (dissolutionQuorum > group.getMaxMembers()) {
            throw new IllegalArgumentException("그룹 해체 인원수는 그룹 최대 인원을 초과할 수 없습니다.");
        }

        group.updateQuorumSetting(voteQuorum, dissolutionQuorum);
        groupRepository.save(group);
    }

    @Transactional
    public String issueInvitation(UUID groupId, UUID operatorId) {
        assertOperatorIsOwner(groupId, operatorId);
        InvitationCode invitation = InvitationCode.issue(groupId);
        InvitationCode saved = invitationCodeRepository.save(invitation);
        return saved.getCode();
    }

    @Transactional
    public void acceptInvite(String code, UUID userId) {
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

    @Transactional(readOnly = true)
    public Group getDetail(UUID groupId, UUID userId) {
        assertMember(groupId, userId);
        return groupRepository.findById(groupId).orElseThrow(() -> new NoSuchElementException("그룹이 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<Group> findMyGroups(UUID userId) {
        return groupRepository.findAllByMember(userId);
    }

    @Transactional(readOnly = true)
    public List<GroupMember> members(UUID groupId, UUID userId) {
        assertMember(groupId, userId);
        return groupMemberRepository.findByIdGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public UUID getOwnerId(UUID groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다."));
        return group.getOwnerId();
    }

    @Transactional
    public void addMember(UUID groupId, GroupMemberAddRequest request, UUID operatorId) {
        assertOperatorIsOwner(groupId, operatorId);
        assertNotDuplicate(groupId, request.userId());

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        if (group.getStatus() == GroupStatus.ACTIVE) {
            throw new IllegalArgumentException("그룹이 이미 활성화되어 더 이상 멤버를 추가할 수 없습니다.");
        }

        GroupMember member = GroupMember.join(groupId, request.userId());
        groupMemberRepository.save(member);

        group.addMember();
        groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public GroupStatusResponse getGroupStatus(UUID groupId, UUID userId) {
        assertMember(groupId, userId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        return new GroupStatusResponse(
                group.getStatus(),
                group.getCurrentMembers(),
                group.getMaxMembers(),
                group.isFull()
        );
    }

    @Transactional(readOnly = true)
    public List<GroupStatusResponse> getMyGroupsStatus(UUID userId) {
        List<Group> groups = groupRepository.findAllByMember(userId);
        return groups.stream()
                .map(g -> new GroupStatusResponse(
                        g.getStatus(),
                        g.getCurrentMembers(),
                        g.getMaxMembers(),
                        g.isFull()
                ))
                .toList();
    }

    // 시스템용 조회 기능
    @Transactional(readOnly = true)
    public GroupSettingsResponse getGroupSettingsInternal(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다."));

        return new GroupSettingsResponse(
                group.getVoteQuorum(),
                group.getDissolutionQuorum(),
                group.getGoalAmount(),
                group.getMaxMembers()
        );
    }

    @Transactional(readOnly = true)
    public Integer getVoteQuorumInternal(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다."));

        return group.getVoteQuorum();
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
        if(request.maxMembers() == null || request.maxMembers() <= 0) {
            throw new IllegalArgumentException("그룹 인원은 최대 1명 이상이어야 합니다.");
        }
        if (request.voteQuorum() == null || request.voteQuorum() <= 0) {
            throw new IllegalArgumentException("투표 가결 인원수는 1명 이상이어야 합니다.");
        }
        if (request.dissolutionQuorum() == null || request.dissolutionQuorum() <= 0) {
            throw new IllegalArgumentException("그룹 해체 동의 인원수는 1명 이상이어야 합니다.");
        }

        if (request.voteQuorum() > request.maxMembers()) {
            throw new IllegalArgumentException("투표 가결 인원수는 그룹 최대 인원을 초과할 수 없습니다.");
        }
        if (request.dissolutionQuorum() > request.maxMembers()) {
            throw new IllegalArgumentException("그룹 해체 동의 인원수는 그룹 최대 인원을 초과할 수 없습니다.");
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

    private void validateGroupSettings(UUID groupId, GroupSettingsUpdateRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다."));

        // 투표 찬성 인원수 검증
        request.voteQuorum().ifPresent(quorum -> {
            if (quorum > group.getMaxMembers()) {
                throw new IllegalArgumentException("투표 찬성 인원수는 그룹 최대 인원을 초과할 수 없습니다.");
            }
        });

        // 그룹 해체 인원수 검증
        request.dissolutionQuorum().ifPresent(quorum -> {
            if (quorum > group.getMaxMembers()) {
                throw new IllegalArgumentException("그룹 해체 인원수는 그룹 최대 인원을 초과할 수 없습니다.");
            }
        });
    }
}
