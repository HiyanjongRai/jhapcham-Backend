package com.example.jhapcham.banner.dto;


import com.example.jhapcham.banner.application.*;
import com.example.jhapcham.banner.domain.*;
import com.example.jhapcham.banner.dto.*;
import com.example.jhapcham.banner.persistence.*;
import lombok.Data;

@Data
public class BannerProductRequestDTO {
    private Long productId;
    private Integer displayOrder;
}
