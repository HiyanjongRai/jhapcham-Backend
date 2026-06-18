package com.example.jhapcham.product.dto;


import com.example.jhapcham.product.domain.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AttributeDTO {
    private Long id;
    private String name;
    private List<AttributeValueDTO> values;

    @Data
    @Builder
    public static class AttributeValueDTO {
        private Long id;
        private String value;
    }
}
