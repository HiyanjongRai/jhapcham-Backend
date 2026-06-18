package com.example.jhapcham.inventory.domain;


import com.example.jhapcham.inventory.application.*;
import com.example.jhapcham.inventory.domain.*;
import com.example.jhapcham.inventory.dto.*;
import com.example.jhapcham.inventory.persistence.*;
public enum InventoryAlertType {
    LOW_STOCK("Product stock is running low"),
    OUT_OF_STOCK("Product is out of stock"),
    OVERSTOCK("Product has excessive stock"),
    RESTOCK_REMINDER("Time to reorder product");

    private final String description;

    InventoryAlertType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
