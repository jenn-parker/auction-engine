package com.jennparker.auction.persistence;

import com.jennparker.auction.domain.PaymentMethod;
import javax.persistence.*;

@Entity
@Table(name = "payment_methods")
public class PaymentMethodEntity {
    @Id private String id;
    @Column(nullable = false) private String provider;
    @Column(name = "expiration_month") private Integer expirationMonth;
    @Column(name = "expiration_year") private Integer expirationYear;
    @Column(name = "customer_id", nullable = false) private String customerId;
    @Column(name = "method_type", nullable = false) private String methodType;
    @Column(nullable = false) private boolean authorized;
    @Column(name = "is_default", nullable = false) private boolean isDefault;

    protected PaymentMethodEntity() { }
    public PaymentMethodEntity(String customerId, PaymentMethod method) {
        this.id = method.getId(); this.customerId = customerId; this.provider = method.getProvider();
        this.expirationMonth = method.getExpirationMonth(); this.expirationYear = method.getExpirationYear();
        this.methodType = method.getType().name(); this.authorized = method.isAuthorized();
        this.isDefault = method.isDefault();
    }
    public PaymentMethod toDomain() { return new PaymentMethod(id, provider, PaymentMethod.Type.valueOf(methodType), authorized, isDefault, expirationMonth, expirationYear); }
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public PaymentMethod.Type getType() { return PaymentMethod.Type.valueOf(methodType); }
    public boolean isAuthorized() { return authorized; }
    public boolean isDefault() { return isDefault; }
    public void update(PaymentMethod method) { this.provider = method.getProvider(); this.methodType = method.getType().name(); this.authorized = method.isAuthorized(); this.isDefault = method.isDefault(); this.expirationMonth = method.getExpirationMonth(); this.expirationYear = method.getExpirationYear(); }
    public void clearDefault() { this.isDefault = false; }
}
