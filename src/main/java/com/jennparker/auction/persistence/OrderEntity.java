package com.jennparker.auction.persistence;

import com.jennparker.auction.domain.Order;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id private UUID id;
    @Column(name = "lot_id", nullable = false, unique = true) private String lotId;
    @Column(name = "customer_id", nullable = false) private String customerId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected OrderEntity() { }

    public OrderEntity(Order order) {
        this.id = UUID.fromString(order.getId());
        this.lotId = order.getLotId(); this.customerId = order.getCustomerId();
        this.amount = order.getAmount(); this.status = order.getStatus().name(); this.createdAt = order.getCreatedAt();
    }

    public UUID getId() { return id; }
    public String getLotId() { return lotId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public void updateStatus(Order.Status next) { this.status = next.name(); }
    public Order toDomain() { return new Order(id.toString(), lotId, customerId, amount, createdAt, Order.Status.valueOf(status)); }
}
