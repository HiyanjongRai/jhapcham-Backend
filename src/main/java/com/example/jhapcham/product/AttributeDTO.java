package com.example.jhapcham.product;

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
