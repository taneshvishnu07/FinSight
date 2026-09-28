/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.alert;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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