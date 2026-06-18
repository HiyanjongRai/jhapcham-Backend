package com.example.jhapcham.banner.dto;


import com.example.jhapcham.banner.application.*;
import com.example.jhapcham.banner.domain.*;
import com.example.jhapcham.banner.dto.*;
import com.example.jhapcham.banner.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BannerProductResponseDTO {
    private Long id;
    private Long productId;
    private String name;
    private String thumbnail;
    private Integer stock;
    private BigDecimal price;
    private BigDecimal discountPercentage;
    private BigDecimal discountedPrice;
    private Integer displayOrder;
}
