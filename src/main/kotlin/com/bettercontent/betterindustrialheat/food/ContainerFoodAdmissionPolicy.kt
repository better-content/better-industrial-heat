package com.bettercontent.betterindustrialheat.food

/** Prevents untouched generated loot from opting in while admitting food-bearing machine containers. */
object ContainerFoodAdmissionPolicy {
    fun shouldActivate(hasTrackedFood: Boolean, hasUnopenedLoot: Boolean): Boolean =
        hasTrackedFood && !hasUnopenedLoot
}
