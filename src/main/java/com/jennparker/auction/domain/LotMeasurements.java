package com.jennparker.auction.domain;

import java.math.BigDecimal;

public final class LotMeasurements {
    private final BigDecimal height, width, length, depth, weight;
    private final String dimensionUnit, weightUnit;
    public LotMeasurements(BigDecimal height, BigDecimal width, BigDecimal length, BigDecimal depth,
                           BigDecimal weight, String dimensionUnit, String weightUnit) {
        this.height = height; this.width = width; this.length = length; this.depth = depth; this.weight = weight;
        this.dimensionUnit = dimensionUnit; this.weightUnit = weightUnit;
    }
    public BigDecimal getHeight() { return height; } public BigDecimal getWidth() { return width; }
    public BigDecimal getLength() { return length; } public BigDecimal getDepth() { return depth; }
    public BigDecimal getWeight() { return weight; } public String getDimensionUnit() { return dimensionUnit; }
    public String getWeightUnit() { return weightUnit; }
}
