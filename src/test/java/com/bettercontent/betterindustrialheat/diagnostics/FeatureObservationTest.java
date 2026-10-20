package com.bettercontent.betterindustrialheat.diagnostics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public final class FeatureObservationTest {
    @Test public void nativeScalarsHaveExplicitStage() { var fields = FeatureObservation.fields("minecraft:water", "chemlib:steam", 1000, 900f, 500f); assertTrue(fields.contains("amount_mb=1000")); assertTrue(fields.contains("before_heat=900.0")); assertTrue(fields.contains("after_heat=500.0")); }
    @Test public void uncertaintyAndPrivacyStayExplicit() { var fields = FeatureObservation.fields("private text\nsecret", "not a registry id", 1, 0f, 0f); assertFalse(fields.contains("secret")); assertFalse(fields.contains("private")); assertTrue(fields.contains("source=UNKNOWN")); }
}
