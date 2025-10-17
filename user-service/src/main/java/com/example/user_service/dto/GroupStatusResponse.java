package com.example.user_service.dto;

import com.example.user_service.domain.GroupStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그룹 상태 응답")
public record GroupStatusResponse(
        @Schema(description = "그룹 상태", example = "WAITING", allowableValues = {"WAITING", "ACTIVE", "DISSOLVED"})
        GroupStatus status,
        
        @Schema(description = "현재 그룹원 수", example = "4")
        Integer currentMembers,
        
        @Schema(description = "최대 그룹원 수", example = "5")
        Integer maxMembers,
        
        @Schema(description = "그룹이 가득 찼는지 여부", example = "false")
        Boolean isFull
) {
}
