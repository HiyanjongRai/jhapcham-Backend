package com.example.jhapcham.loyalty.domain;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
public enum LoyaltyTransactionStatus {
    PENDING,
    AVAILABLE,
    REVERSED,
    EXPIRED,
    CANCELLED,
    FLAGGED
}
