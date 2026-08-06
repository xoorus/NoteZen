package com.bchev.notezen.domain.model;

public enum PricingPlan {
    STARTER(19.90, 1, "starter"),
    PROFESSIONAL(24.90, 999, "professional");

    private final double monthlyPrice;
    private final int maxLocations;
    private final String stripePriceId; // sera défini ultérieurement

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
