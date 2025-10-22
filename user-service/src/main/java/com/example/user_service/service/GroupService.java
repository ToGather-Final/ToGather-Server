package com.example.user_service.service;

import com.example.user_service.domain.*;
import com.example.user_service.dto.*;
import com.example.user_service.repository.GroupMemberRepository;
import com.example.user_service.repository.GroupRepository;
import com.example.user_service.repository.InvitationCodeRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.example.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.user_service.domain.InvitationCode.generateCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final InvitationCodeRepository invitationCodeRepository;
    private final com.example.user_service.client.TradingServiceClient tradingServiceClient;
    private final UserRepository userRepository;

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

        // ❌ 그룹 생성 시에는 예수금 지급 안 함!
        // ✅ 모든 멤버가 모였을 때 (ACTIVE 상태) 일괄 지급

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
        assertMember(groupId, operatorId);

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

        String code;
        int maxAttempts = 10;
        int attempts = 0;

        do {
            code = generateCode();
            attempts++;

            if (attempts > maxAttempts) {
                throw new RuntimeException("초대 코드 생성에 실패했습니다. 다시 시도해주세요.");
            }
        } while (invitationCodeRepository.existsByCode(code));

        InvitationCode invitation = InvitationCode.issue(groupId, code);
        InvitationCode saved = invitationCodeRepository.save(invitation);
        return saved.getCode();
    }

    @Transactional
    public InviteAcceptResponse acceptInvite(String code, UUID userId) {
        InvitationCode invitationCode = invitationCodeRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("초대 코드를 찾을 수 없습니다."));

        validateInvitationAcceptable(invitationCode);

        Group group = groupRepository.findById(invitationCode.getGroupId())
                .orElseThrow(() -> new NoSuchElementException("그룹을 찾을 수 없습니다."));

        if (group.getStatus() != GroupStatus.WAITING) {
            throw new IllegalArgumentException("참여할 수 없는 그룹입니다.");
        }

        GroupMemberId groupMemberId = new GroupMemberId(userId, invitationCode.getGroupId());
        boolean isAlreadyMember = groupMemberRepository.existsById(groupMemberId);
        if (!isAlreadyMember) {
            // 이전 상태 저장
            GroupStatus previousStatus = group.getStatus();
            
            // 멤버 추가 (이 시점에 WAITING → ACTIVE 변경 가능)
            group.addMember();
            groupRepository.save(group);

            groupMemberRepository.save(GroupMember.join(invitationCode.getGroupId(), userId));

            // ✅ 그룹이 ACTIVE 상태가 되었는지 확인 (모든 멤버가 모임!)
            if (previousStatus == GroupStatus.WAITING && group.getStatus() == GroupStatus.ACTIVE) {
                log.info("🎉 그룹 완성! 모든 멤버에게 초기 예수금 일괄 지급 - groupId: {}, initialAmount: {}원", 
                        group.getGroupId(), group.getInitialAmount());
                
                // 모든 그룹 멤버에게 예수금 일괄 지급
                if (group.getInitialAmount() != null && group.getInitialAmount() > 0) {
                    depositInitialFundsToAllMembers(group.getGroupId(), group.getInitialAmount());
                }
            }
        }

        if (group.isFull()) {
            invitationCode.expire();
            invitationCodeRepository.save(invitationCode);
        }

        return new InviteAcceptResponse(group.getGroupId(), group.getGroupName());
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

        if (group.getStatus() != GroupStatus.WAITING) {
            throw new IllegalArgumentException("그룹이 이미 활성화되어 더 이상 참여할 수 없습니다.");
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

        // 실제 GroupMember 테이블에서 멤버 수 계산
        long actualMemberCount = groupMemberRepository.countByIdGroupId(groupId);

        return new GroupStatusResponse(
                group.getStatus(),
                (int) actualMemberCount,  // 실제 멤버 수 사용
                group.getMaxMembers(),
                actualMemberCount >= group.getMaxMembers()  // 실제 멤버 수로 계산
        );
    }

    @Transactional(readOnly = true)
    public List<GroupStatusResponse> getMyGroupsStatus(UUID userId) {
        List<Group> groups = groupRepository.findAllByMember(userId);
        return groups.stream()
                .map(g -> {
                    // 실제 GroupMember 테이블에서 멤버 수 계산
                    long actualMemberCount = groupMemberRepository.countByIdGroupId(g.getGroupId());
                    return new GroupStatusResponse(
                            g.getStatus(),
                            (int) actualMemberCount,  // 실제 멤버 수 사용
                            g.getMaxMembers(),
                            actualMemberCount >= g.getMaxMembers()  // 실제 멤버 수로 계산
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoginResponse.UserGroupInfo> getUserGroupsForLogin(UUID userId) {
        List<Group> groups = groupRepository.findAllByMember(userId);
        return groups.stream()
                .map(g -> {
                    long actualMemberCount = groupMemberRepository.countByIdGroupId(g.getGroupId());

                    boolean isOwner = g.getOwnerId().equals(userId);

                    String groupCode = invitationCodeRepository.findByGroupIdAndIsExpiredFalse(g.getGroupId())
                            .map(InvitationCode::getCode)
                            .orElse(null);

                    return new LoginResponse.UserGroupInfo(
                            g.getGroupId(),
                            g.getGroupName(),
                            groupCode,
                            g.getStatus(),
                            (int) actualMemberCount,
                            g.getMaxMembers(),
                            actualMemberCount >= g.getMaxMembers(),
                            isOwner
                    );
                })
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
        log.info("🔍 그룹 투표 정족수 조회 시작 - groupId: {}", groupId);
        
        try {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> {
                        log.error("❌ 그룹을 찾을 수 없습니다 - groupId: {}", groupId);
                        return new NoSuchElementException("그룹을 찾을 수 없습니다.");
                    });

            Integer voteQuorum = group.getVoteQuorum();
            log.info("✅ 그룹 투표 정족수 조회 완료 - groupId: {}, voteQuorum: {}, groupName: {}", 
                    groupId, voteQuorum, group.getGroupName());
            
            return voteQuorum;
        } catch (NoSuchElementException e) {
            log.error("❌ 그룹 조회 실패 - groupId: {}, error: {}", groupId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ 예상치 못한 오류 발생 - groupId: {}, error: {}", groupId, e.getMessage(), e);
            throw new RuntimeException("투표 정족수 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public String getCurrentInvitationCode(UUID groupId, UUID userId) {
        assertMember(groupId, userId);
        return invitationCodeRepository.findByGroupIdAndIsExpiredFalse(groupId)
                .map(InvitationCode::getCode)
                .orElse(null);
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

    /**
     * 그룹이 완성되었을 때 모든 멤버에게 초기 예수금 일괄 지급
     * - 그룹 상태가 WAITING → ACTIVE로 변경될 때 호출
     */
    private void depositInitialFundsToAllMembers(UUID groupId, Integer initialAmount) {
        try {
            log.info("🎉 그룹 완성! 모든 멤버에게 예수금 일괄 지급 시작 - groupId: {}, amount: {}원", groupId, initialAmount);
            
            // 그룹의 모든 멤버 조회
            List<GroupMember> allMembers = groupMemberRepository.findByIdGroupId(groupId);
            
            if (allMembers.isEmpty()) {
                log.warn("⚠️ 그룹 멤버가 없습니다 - groupId: {}", groupId);
                return;
            }
            
            log.info("👥 그룹 멤버 수: {}명", allMembers.size());
            
            int successCount = 0;
            int failCount = 0;
            
            // 각 멤버에게 예수금 지급
            for (GroupMember member : allMembers) {
                try {
                    UUID userId = member.getId().getUserId();
                    
                    // 1. 투자 계좌 생성 (이미 있으면 기존 계좌 반환)
                    try {
                        tradingServiceClient.createInvestmentAccount(userId);
                        log.debug("✅ 투자 계좌 확인/생성 완료 - userId: {}", userId);
                    } catch (Exception e) {
                        log.debug("⚠️ 투자 계좌 생성 중 오류 (이미 존재할 수 있음) - userId: {}, error: {}", userId, e.getMessage());
                    }
                    
                    // 2. 예수금 충전
                    java.math.BigDecimal amount = java.math.BigDecimal.valueOf(initialAmount);
                    com.example.user_service.dto.InternalDepositRequest depositRequest = 
                            new com.example.user_service.dto.InternalDepositRequest(
                                    userId,
                                    amount,
                                    groupId,
                                    "그룹 시작 - 초기 예수금 지급"
                            );
                    
                    tradingServiceClient.depositFunds(depositRequest);
                    successCount++;
                    
                    log.info("✅ 멤버 예수금 지급 완료 - userId: {}, amount: {}원", userId, initialAmount);
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ 멤버 예수금 지급 실패 - userId: {}, amount: {}원, error: {}", 
                            member.getId().getUserId(), initialAmount, e.getMessage());
                }
            }
            
            log.info("🎊 예수금 일괄 지급 완료! - 성공: {}명, 실패: {}명, 총 멤버: {}명", 
                    successCount, failCount, allMembers.size());
            
        } catch (Exception e) {
            log.error("❌ 예수금 일괄 지급 중 오류 발생 - groupId: {}, error: {}", 
                    groupId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    @Transactional(readOnly = true)
    public boolean isGroupMember(UUID groupId, UUID userId) {
        return groupMemberRepository.existsByIdGroupIdAndIdUserId(groupId, userId);
    }

    /**
     * 사용자가 속한 그룹 목록 조회 (내부 시스템용)
     */
    @Transactional(readOnly = true)
    public List<UUID> getUserGroupsInternal(UUID userId) {
        try {
            log.info("사용자 그룹 목록 조회 - 사용자ID: {}", userId);
            
            List<GroupMember> userMemberships = groupMemberRepository.findByIdUserId(userId);
            List<UUID> userGroups = userMemberships.stream()
                    .map(member -> member.getId().getGroupId())
                    .toList();
            
            log.info("사용자 그룹 목록 조회 완료 - 사용자ID: {}, 그룹 수: {}", userId, userGroups.size());
            return userGroups;
            
        } catch (Exception e) {
            log.error("사용자 그룹 목록 조회 실패 - 사용자ID: {} - {}", userId, e.getMessage());
            return List.of(); // 빈 리스트 반환
        }
    }
}
