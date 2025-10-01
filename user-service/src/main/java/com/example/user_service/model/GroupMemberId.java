package com.example.user_service.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
@Embeddable
public class GroupMemberId implements Serializable {

    private UUID userId;
    private UUID groupId;

    public GroupMemberId(){}

    public GroupMemberId(UUID userId, UUID groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroupMemberId that = (GroupMemberId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, groupId);
    }

}
