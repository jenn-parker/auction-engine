package com.jennparker.auction.domain;

/** Optional physical venue information for an auction. */
public final class PhysicalLocation {
    private final String venueName;
    private final String line1;
    private final String line2;
    private final String city;
    private final String stateOrRegion;
    private final String postalCode;
    private final String countryCode;

    public PhysicalLocation(String venueName, String line1, String line2, String city,
                            String stateOrRegion, String postalCode, String countryCode) {
        this.venueName = venueName; this.line1 = line1; this.line2 = line2; this.city = city;
        this.stateOrRegion = stateOrRegion; this.postalCode = postalCode; this.countryCode = countryCode;
    }
    public String getVenueName() { return venueName; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getStateOrRegion() { return stateOrRegion; }
    public String getPostalCode() { return postalCode; }
    public String getCountryCode() { return countryCode; }
}
