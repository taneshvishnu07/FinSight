package com.finsight.backend.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An upload record as shown to the user: file name, rows found vs imported, status and
 * time.
 */
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