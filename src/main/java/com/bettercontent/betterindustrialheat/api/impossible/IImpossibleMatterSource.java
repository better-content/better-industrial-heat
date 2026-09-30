package com.bettercontent.betterindustrialheat.api.impossible;

/**
 * Optional boundary implemented by a finite impossible-matter vessel.
 *
 * <p>The source owns conservation: an actual unbind must reduce its finite
 * inventory, retain or emit daughter products, and retain every other side
 * effect. HeatSync only controls containment and transports the AE and heat
 * explicitly reported by the result.</p>
 */
public interface IImpossibleMatterSource {
    /**
     * Preview or perform at most {@code maxUnits} finite unbinding units.
     * Simulation must be side-effect free. An actual call owns daughter-product
     * accounting and must never return more than the preceding preview.
     */
    ImpossibleUnbindingResult unbind(long maxUnits, boolean simulate);
}
