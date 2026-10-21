package org.oddlama.vane.annotation.processor

import javax.annotation.processing.SupportedAnnotationTypes

/** Validates that `@VaneEnchantment` sits on a class extending vane-core's `CustomEnchantment`. */
@SupportedAnnotationTypes("org.oddlama.vane.annotation.enchantment.VaneEnchantment")
class VaneEnchantmentProcessor :
    ClassPlacementProcessor("org.oddlama.vane.core.enchantments.CustomEnchantment")
