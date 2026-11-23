package com.finsight.backend.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.finsight.backend.dto.response.UploadResponse;
import com.finsight.backend.dto.transaction.TransactionResponse;

public interface UploadService {

    UploadResponse uploadTransactions(
            MultipartFile file
    );

    List<UploadResponse> getMyUploads();

    UploadResponse getUploadById(
            Long uploadId
    );

    List<TransactionResponse> getTransactionsByUpload(
            Long uploadId
    );
}