package dev.bartuzen.qbitcontroller.network

import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class EnumConverterFactory : Converter.Factory() {
    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<Enum<*>, String>? = if (type is Class<*> && type.isEnum) {
        Converter<Enum<*>, String> { value ->
            value.javaClass.getField(value.name).getAnnotation(JsonProperty::class.java)?.value
                ?: value.name
        }
    } else {
        null
    }
}
