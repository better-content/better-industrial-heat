package com.bettercontent.betterindustrialheat.diagnostics;

/** Pure projection of values already observed by the owning native decision. */
public final class FeatureObservation {
    private FeatureObservation() {}
    public static String fields(String source, String target, int amount, float before, float after) { return PlaytestLog.fields("source", registry(source), "target", registry(target), "amount_mb", amount, "before_heat", before, "after_heat", after); }
    private static String registry(String value) { return value != null && value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") ? value : "UNKNOWN"; }
}
