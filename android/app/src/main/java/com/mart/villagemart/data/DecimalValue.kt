package com.mart.villagemart.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonUnquotedLiteral
import java.math.BigDecimal

@kotlinx.serialization.Serializable(with = DecimalValueSerializer::class)
data class DecimalValue(val value: BigDecimal) {
    constructor(value: String) : this(value.toBigDecimal())
    constructor(value: Long) : this(BigDecimal.valueOf(value))

    override fun toString(): String = value.toPlainString()

    companion object {
        val ZERO = DecimalValue(BigDecimal.ZERO)
        fun of(value: String?): DecimalValue = value?.toBigDecimalOrNull()?.let(::DecimalValue) ?: ZERO
    }
}

object DecimalValueSerializer : KSerializer<DecimalValue> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("VillageMartDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DecimalValue {
        val text = if (decoder is JsonDecoder) {
            (decoder.decodeJsonElement() as? JsonPrimitive)?.contentOrNull
                ?: error("Expected a decimal JSON value")
        } else {
            decoder.decodeString()
        }
        return DecimalValue(text)
    }

    override fun serialize(encoder: Encoder, value: DecimalValue) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonUnquotedLiteral(value.toString()))
        } else {
            encoder.encodeString(value.toString())
        }
    }
}
