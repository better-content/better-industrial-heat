package com.bettercontent.betterindustrialheat.api.impossible;

/** Finite energy and retained heat released by an impossible-matter source. */
public record ImpossibleUnbindingResult(long units, double ae, float heat) {
    public static final ImpossibleUnbindingResult EMPTY = new ImpossibleUnbindingResult(0, 0.0, 0.0f);

    public ImpossibleUnbindingResult {
        if (units < 0 || !Double.isFinite(ae) || ae < 0 || !Float.isFinite(heat) || heat < 0) {
            throw new IllegalArgumentException("Impossible unbinding results must be finite and non-negative");
        }
        if (units == 0 && (ae != 0.0 || heat != 0.0f)) {
            throw new IllegalArgumentException("Zero unbinding units cannot release energy or heat");
        }
    }
}
