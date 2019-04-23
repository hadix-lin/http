package io.github.hadixlin.http

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.TypeReference

import java.io.IOException
import java.io.InputStream

/** Created by hadix on 24/11/2016.  */
object Json {

	fun toBytes(bean: Any): ByteArray {
		return JSON.toJSONBytes(bean)
	}

	fun <T> toBean(input: InputStream, clazz: Class<T>): T {
		try {
			return JSON.parseObject(input, clazz)
		} catch (e: IOException) {
			throw RuntimeException(e)
		}

	}

	fun <T> toBean(input: InputStream, typeReference: TypeReference<T>): T {
		try {
			return JSON.parseObject(input, typeReference.type)
		} catch (e: IOException) {
			throw RuntimeException(e)
		}

	}
}
