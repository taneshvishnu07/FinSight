package com.finsight.backend.dto.classification;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClassificationResponse {

    private String status;

    private String message;

    private Integer totalTransactions;

    private Integer classifiedTransactions;

    private List<ClassificationItemResponse> classifications = new ArrayList<>();
}
