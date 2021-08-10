/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

open class BinaryOption<T : Any>(
        val name: String,
        val valueParser: ValueParser<T>,
        val compilerConfigurationKey: CompilerConfigurationKey<T> = CompilerConfigurationKey.create(name)
) {
    interface ValueParser<T : Any> {
        fun parse(value: String): T?
        val validValuesHint: String?
    }
}

object BinaryOptions {
    private val registeredOptionsByName = mutableMapOf<String, BinaryOption<*>>()

    private fun register(option: BinaryOption<*>) {
        val previousOption = registeredOptionsByName[option.name]
        if (previousOption != null) {
            error("option '${option.name}' is registered twice: $previousOption and $option") // FIXME: makes no sense without toString
        }
        registeredOptionsByName[option.name] = option
    }

    fun getByName(name: String): BinaryOption<*>? = registeredOptionsByName[name]

    private inline fun <reified T : Enum<T>> option(): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<T>>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, EnumValueParser(enumValues<T>().toList()))
                register(option)
                ReadOnlyProperty { _, _ ->
                    option.compilerConfigurationKey
                }
            }

    private fun booleanOption(): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CompilerConfigurationKey<Boolean>>> =
            PropertyDelegateProvider { _, property ->
                val option = BinaryOption(property.name, BooleanValueParser)
                register(option)
                ReadOnlyProperty { _, _ ->
                    option.compilerConfigurationKey
                }
            }
}

private object BooleanValueParser : BinaryOption.ValueParser<Boolean> {
    override fun parse(value: String): Boolean? = value.toBooleanStrictOrNull()

    override val validValuesHint: String?
        get() = "true|false"
}

private class EnumValueParser<T : Enum<T>>(val values: List<T>) : BinaryOption.ValueParser<T> {
    // TODO: should we really ignore case here?
    override fun parse(value: String): T? = values.firstOrNull { it.name.equals(value, ignoreCase = true) }

    override val validValuesHint: String?
        get() = values.joinToString("|")
}