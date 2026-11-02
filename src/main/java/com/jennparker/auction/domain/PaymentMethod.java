package com.jennparker.auction.domain;

/** Payment instrument metadata only; raw PAN/CVV data must never enter this model. */
public final class PaymentMethod {
    private final String id;
    private final String provider;
    private final Type type;
    private final Integer expirationMonth;
    private final Integer expirationYear;
    private final boolean authorized;
    private final boolean isDefault;

    public PaymentMethod(String id, Type type, boolean authorized) {
        this(id, "external", type, authorized, false);
    }

    public PaymentMethod(String id, Type type, boolean authorized, boolean isDefault) {
        this(id, "external", type, authorized, isDefault);
    }

    public PaymentMethod(String id, String provider, Type type, boolean authorized, boolean isDefault) {
        this(id, provider, type, authorized, isDefault, null, null);
    }

    public PaymentMethod(String id, String provider, Type type, boolean authorized, boolean isDefault,
                         Integer expirationMonth, Integer expirationYear) {
        if (id == null || type == null) throw new IllegalArgumentException("Payment method id and type are required");
        if (expirationMonth != null && (expirationMonth < 1 || expirationMonth > 12)) throw new IllegalArgumentException("Expiration month must be 1-12");
        if (expirationYear != null && expirationYear < 2000) throw new IllegalArgumentException("Expiration year is invalid");
        this.id = id;
        this.provider = provider == null || provider.trim().isEmpty() ? "external" : provider;
        this.type = type;
        this.authorized = authorized;
        this.isDefault = isDefault;
        this.expirationMonth = expirationMonth;
        this.expirationYear = expirationYear;
    }

    public String getId() { return id; }
    public String getProvider() { return provider; }
    public Type getType() { return type; }
    public Integer getExpirationMonth() { return expirationMonth; }
    public Integer getExpirationYear() { return expirationYear; }
    public boolean isAuthorized() { return authorized; }
    public boolean isDefault() { return isDefault; }

    public enum Type { CARD, BANK_ACCOUNT, WALLET }
}
