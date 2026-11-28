package com.finsight.backend.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.finsight.backend.dto.response.UploadResponse;
import com.finsight.backend.dto.transaction.TransactionResponse;

/**
 * Importing transaction files and reading upload history. Implementation:
 * UploadServiceImpl.
 */
public interface UploadService {

    /**
     * Parse and validate an uploaded CSV/Excel file, then save its transactions and an
     * upload history record.
     */
    UploadResponse uploadTransactions(
            MultipartFile file
    );

    /**
     * All uploads of the logged-in user, newest first.
     */
    List<UploadResponse> getMyUploads();

    /**
     * One upload record (must belong to the user).
     */
    UploadResponse getUploadById(
            Long uploadId
    );

    /**
     * The transactions imported by one upload.
     */
    List<TransactionResponse> getTransactionsByUpload(
            Long uploadId
    );
}