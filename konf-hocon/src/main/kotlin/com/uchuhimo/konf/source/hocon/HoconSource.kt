/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uchuhimo.konf.source.hocon

import com.typesafe.config.Config
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import com.typesafe.config.ConfigValueFactory
import com.typesafe.config.ConfigValueType
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.name
import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo
import com.uchuhimo.konf.source.WrongTypeException
import com.uchuhimo.konf.source.toDescription

/**
 * Source from a HOCON value.
 */
class HoconSource(
    val value: ConfigValue,
    context: Map<String, String> = mapOf()
) : Source, SourceInfo by SourceInfo.with(context) {
    init {
        addInfo("type", "HOCON")
    }

    private val type = value.valueType()

    @Suppress("UNCHECKED_CAST")
    private fun <T> ConfigValue.cast(): T = unwrapped() as T

    private fun checkType(actual: ConfigValueType, expected: ConfigValueType) {
        if (actual != expected) {
            throw WrongTypeException(this, "HOCON(${actual.name})", "HOCON(${expected.name})")
        }
    }

    enum class NumType {
        Int, Long, Double
    }

    private fun checkNumType(expected: NumType) {
        val unwrappedValue = value.unwrapped()
        val type = when (unwrappedValue) {
            is Int -> NumType.Int
            is Long -> NumType.Long
            is Double -> NumType.Double
            else -> throw ParseException(
                "value $unwrappedValue with type ${unwrappedValue::class.java.simpleName}" +
                    " is not a valid number(Int/Long/Double)")
        }
        if (type != expected) {
            throw WrongTypeException(this, "HOCON(${type.name})", "HOCON(${expected.name})")
        }
    }

    val config: Config by lazy {
        checkType(type, ConfigValueType.OBJECT)
        (value as ConfigObject).toConfig()
    }

    override fun contains(path: Path): Boolean = config.hasPathOrNull(path.name)

    override fun getOrNull(path: Path): Source? {
        val name = path.name
        return if (config.hasPathOrNull(name)) {
            if (config.getIsNull(name)) {
                HoconSource(
                        ConfigValueFactory.fromAnyRef(null, config.origin().description()),
                        context)
            } else {
                HoconSource(config.getValue(name), context)
            }
        } else {
            null
        }
    }

    override fun isNull(): Boolean = type == ConfigValueType.NULL

    override fun isList(): Boolean = type == ConfigValueType.LIST

    override fun toList(): List<Source> {
        checkType(type, ConfigValueType.LIST)
        return mutableListOf<Source>().apply {
            for (value in (value as ConfigList)) {
                add(HoconSource(value, context).apply {
                    addInfo("inList", this@HoconSource.info.toDescription())
                })
            }
        }
    }

    override fun isMap(): Boolean = type == ConfigValueType.OBJECT

    override fun toMap(): Map<String, Source> {
        checkType(type, ConfigValueType.OBJECT)
        return mutableMapOf<String, Source>().apply {
            for ((key, value) in (value as ConfigObject)) {
                put(key, HoconSource(value, context).apply {
                    addInfo("inMap", this@HoconSource.info.toDescription())
                })
            }
        }
    }

    override fun isText(): Boolean = type == ConfigValueType.STRING

    override fun toText(): String {
        checkType(type, ConfigValueType.STRING)
        return value.cast()
    }

    override fun isBoolean(): Boolean = type == ConfigValueType.BOOLEAN

    override fun toBoolean(): Boolean {
        checkType(type, ConfigValueType.BOOLEAN)
        return value.cast()
    }

    override fun isDouble(): Boolean = type == ConfigValueType.NUMBER && value.unwrapped() is Double

    override fun toDouble(): Double {
        return try {
            checkType(type, ConfigValueType.NUMBER)
            checkNumType(NumType.Double)
            value.cast()
        } catch (e: WrongTypeException) {
            try {
                checkNumType(NumType.Long)
                (value.cast<Long>()).toDouble()
            } catch (e: WrongTypeException) {
                checkNumType(NumType.Int)
                (value.cast<Int>()).toDouble()
            }
        }
    }

    override fun isLong(): Boolean = type == ConfigValueType.NUMBER && value.unwrapped() is Long

    override fun toLong(): Long {
        return try {
            checkType(type, ConfigValueType.NUMBER)
            checkNumType(NumType.Long)
            value.cast()
        } catch (e: WrongTypeException) {
            checkNumType(NumType.Int)
            (value.cast<Int>()).toLong()
        }
    }

    override fun isInt(): Boolean = type == ConfigValueType.NUMBER && value.unwrapped() is Int

    override fun toInt(): Int {
        checkType(type, ConfigValueType.NUMBER)
        checkNumType(NumType.Int)
        return value.cast()
    }
}
