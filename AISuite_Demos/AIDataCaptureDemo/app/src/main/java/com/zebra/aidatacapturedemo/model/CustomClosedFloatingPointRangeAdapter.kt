package com.zebra.aidatacapturedemo.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.zebra.aidatacapturedemo.data.DefaultValues
import java.lang.reflect.Type

// Gson lib cannot directly typecase kotlin.ranges.ClosedFloatingPointRange and store under file,
// hence use the include the following Adapter Class explicitly for Gson
class CustomClosedFloatingPointRangeAdapter : JsonSerializer<ClosedFloatingPointRange<*>>,
    JsonDeserializer<ClosedFloatingPointRange<*>> {
    override fun serialize(
        src: ClosedFloatingPointRange<*>,
        typeOfSrc: Type?,
        context: JsonSerializationContext
    ): JsonElement {
        // Serialize the range to a simple JSON object, e.g., {"start": 0.0, "endInclusive": 10.0}
        val obj = JsonObject()
        obj.addProperty("start", src.start as Number)
        obj.addProperty("endInclusive", src.endInclusive as Number)
        return obj
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): ClosedFloatingPointRange<*> {
        val obj = json.asJsonObject
        val start = obj.get("start")?.asFloat ?: DefaultValues.STRING_LENGTH_RANGE_MIN_VALUE
        val endInclusive =
            obj.get("endInclusive")?.asFloat ?: DefaultValues.STRING_LENGTH_RANGE_MAX_VALUE
        return start..endInclusive
    }
}
