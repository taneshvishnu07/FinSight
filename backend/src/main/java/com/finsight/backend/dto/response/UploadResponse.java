/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    private Long uploadId;

    private String fileName;

    private Integer totalRecords;

    private Integer importedRecords;

    private String status;

    private LocalDateTime uploadedAt;
}