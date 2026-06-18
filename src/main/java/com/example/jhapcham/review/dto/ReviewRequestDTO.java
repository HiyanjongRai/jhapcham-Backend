package com.example.jhapcham.review.dto;


import com.example.jhapcham.review.application.*;
import com.example.jhapcham.review.domain.*;
import com.example.jhapcham.review.dto.*;
import com.example.jhapcham.review.persistence.*;
import lombok.Data;

@Data
public class ReviewRequestDTO {
    private Long productId;
    private Integer rating;
    private String comment;
}
