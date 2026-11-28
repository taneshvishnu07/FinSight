package com.finsight.backend.dto.alert;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One alert as returned to the frontend. NOTE: AlertResponse declares its own nested
 * AlertItemResponse, which is the one the services actually use; this top-level class
 * appears to be unused.
 */
@Getter
@Setter
@NoArgsConstructor
public class AlertItemResponse {

    private Long id;

    private String type;

    private String severity;

    private String title;

    private String message;

    private String generatedBy;

    private Boolean isRead;

    private LocalDateTime createdAt;
}