package com.example.jhapcham.loyalty.domain;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
public enum LoyaltyTransactionType {
    EARN,
    REDEEM,
    REFUND_REVERSAL,
    EXPIRE,
    BONUS,
    PENDING_REWARD,
    REDEMPTION_RESTORE,
    MANUAL_ADJUSTMENT,
    FRAUD_LOCK
}
