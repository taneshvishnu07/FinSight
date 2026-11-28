package com.finsight.backend.dto.alert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Alerts of one upload plus counts (total / unread) and a summary. Also used as the
 * response model for the Python /api/alerts call, so field names must match the AI
 * service JSON.
 */
@Getter
@Setter
@NoArgsConstructor
public class AlertResponse {

    private Long analysisId;
    private Long uploadHistoryId;
    private int alertCount;
    private int unreadCount;
    private LocalDateTime generatedAt;
    private List<AlertItemResponse> alerts = new ArrayList<>();
    private String summary;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AlertItemResponse {

        private Long id;
        private String type;
        private String severity;
        private String title;
        private String message;
        private Boolean isRead;
        private String generatedBy;
    }
}
