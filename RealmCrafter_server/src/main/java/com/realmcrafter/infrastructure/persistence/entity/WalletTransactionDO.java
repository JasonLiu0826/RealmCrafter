package com.realmcrafter.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水对账表：充值、购买资产、创作者收益、违规扣罚。
 */
@Getter
@Setter
@Entity
@Table(name = "wallet_transaction")
public class WalletTransactionDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserDO user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Type type;

    @Column(length = 255)
    private String description;

    /** 支付渠道订单号，回调幂等防重复入账 */
    @Column(name = "external_order_id", length = 128, unique = true)
    private String externalOrderId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
    }

    public enum Type {
        RECHARGE,
        BUY_ASSET,
        CREATOR_REVENUE,
        PENALTY_DEDUCT
    }
}
