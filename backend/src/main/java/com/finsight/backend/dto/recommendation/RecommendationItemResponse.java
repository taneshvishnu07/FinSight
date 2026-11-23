package com.finsight.backend.dto.recommendation;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecommendationItemResponse {

    private Long id;

    private String type;

    private String priority;

    private String title;

    private String message;

    private BigDecimal relatedAmount;

    private String relatedCategory;

    private String relatedMerchant;

    private String generatedBy;
}