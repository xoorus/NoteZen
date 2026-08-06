package com.bchev.notezen.domain.model;

public enum PricingPlan {
    STARTER(19.90, 1, "price_1TywUBJCNseoIpqDAFOOvnhb"),
    PROFESSIONAL(24.90, 999, "price_1U1NyAJCNseoIpqDOR3rLQC5");

    private final double monthlyPrice;
    private final int maxLocations;
    private final String stripePriceId;

    PricingPlan(double monthlyPrice, int maxLocations, String stripePriceId) {
        this.monthlyPrice = monthlyPrice;
        this.maxLocations = maxLocations;
        this.stripePriceId = stripePriceId;
    }

    public double getMonthlyPrice() {
        return monthlyPrice;
    }

    public int getMaxLocations() {
        return maxLocations;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }
}
