package com.example.jhapcham.product.persistence;


import com.example.jhapcham.product.domain.*;
public interface ProductViewCountProjection {
    Long getProductId();
    long getViewCount();

    String getProductName();
}