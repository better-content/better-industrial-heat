package com.bettercontent.betterindustrialheat.food

import java.util.function.Predicate

/** Pure boundary for rejecting recipes that accept any item in a forbidden input class. */
object RecipeInputGate {
    @JvmStatic
    fun <T> rejects(inputs: Iterable<T>, isRejected: Predicate<T>): Boolean =
        inputs.any(isRejected::test)
}
