package io.github.hadixlin.http

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.InputStream

/** Created by hadix on 24/11/2016.  */
internal object Json {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    fun toBytes(bean: Any): ByteArray {
        return objectMapper.writeValueAsBytes(bean)
    }

    fun <T> toBean(input: InputStream, clazz: Class<T>): T {
        return objectMapper.readValue(input, clazz)
    }

    fun <T> toBean(input: InputStream, typeReference: TypeReference<T>): T {
        return objectMapper.readValue(input, typeReference)
    }
}
