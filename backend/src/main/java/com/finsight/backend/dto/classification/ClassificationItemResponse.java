package com.finsight.backend.dto.classification;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClassificationItemResponse {

    private Long id;

    private String description;

    private String category;

    private BigDecimal confidence;

    private String mappedCategory;

    private String source;

    private Boolean updated;
}
