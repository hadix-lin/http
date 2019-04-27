package io.github.hadixlin.http

import com.fasterxml.jackson.core.type.TypeReference
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.charset.Charset

/** Created by hadix on 21/11/2016.  */
interface HttpRequest {

    /**
     * 添加参数列表
     *
     * @param name 参数名
     * @param value 参数值,可以有多个
     * @return 请求对象本身, 便于链式调用
     */
    fun param(name: String, vararg value: Any): HttpRequest

    /**
     * 添加参数列表
     *
     * @param params 参数名值对,可以重复出现,表示多值参数
     * @return 请求对象本身, 便于链式调用
     */
    fun params(vararg params: Any): HttpRequest

    /**
     * 添加参数列表
     *
     * @param params 参数名值映射表,如果值有多个可以使用Collection的子类
     * @return 请求本身, 便于链式调用
     */
    fun params(params: Map<String, *>): HttpRequest

    fun params(): Map<String, List<*>>

    fun paramsAsDecodingQueryString(): String

    fun header(name: String, value: String): HttpRequest

    fun addHeader(name: String, value: String): HttpRequest

    fun headers(vararg headers: String): HttpRequest

    fun headers(headers: Map<String, String>): HttpRequest

    fun removeHeader(vararg names: String): HttpRequest

    fun body(binaryBody: ByteArray): HttpRequest

    @Throws(FileNotFoundException::class)
    fun body(fileBody: File): HttpRequest

    fun body(stringBody: String): HttpRequest

    fun body(bean: Any): HttpRequest

    fun method(method: Method): HttpRequest

    fun mimeType(contentType: String): HttpRequest

    fun charset(charset: String): HttpRequest

    fun <T> submitForStream(
        inputHandler: ResponseHandler<InputStream, T>,
        errorHandler: ResponseHandler<HttpException, T>
    ): T

    @Throws(HttpException::class)
    fun submitForText(): String

    @Throws(HttpException::class)
    fun submitForFile(): File

    @Throws(HttpException::class)
    fun <T> submitForObject(clazz: Class<T>): T

    @Throws(HttpException::class)
    fun <T> submitForObject(typeReference: TypeReference<T>): T

    fun charset(charset: Charset): HttpRequest
}
