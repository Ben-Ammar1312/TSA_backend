package org.example.tas_backend.dtos;

import lombok.Data;

@Data
public class InviteDTO {
    private String targetUserId;
    private String targetRole;
    private String targetName;
    private String scheduledTime;
    private String note;
}
