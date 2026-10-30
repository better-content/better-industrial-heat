package com.bettercontent.betterindustrialheat.api;

/** Test-source-only same-package registry isolation; never shipped. */
public final class CarrierTestScope {
    private CarrierTestScope() {}
    public static void reset() {
        CookingAgeApi.CARRIERS.clear();
        CookingAgeApi.frozen = false;
    }
}
