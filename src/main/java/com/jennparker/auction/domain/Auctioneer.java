package com.jennparker.auction.domain;

public final class Auctioneer {
    private final String id, cognitoSubject, name, email;
    private final boolean active;
    public Auctioneer(String id, String cognitoSubject, String name, String email, boolean active) {
        if (id == null || cognitoSubject == null) throw new IllegalArgumentException("Auctioneer id and Cognito subject are required");
        this.id = id; this.cognitoSubject = cognitoSubject; this.name = name; this.email = email; this.active = active;
    }
    public String getId() { return id; } public String getCognitoSubject() { return cognitoSubject; }
    public String getName() { return name; } public String getEmail() { return email; } public boolean isActive() { return active; }
}
