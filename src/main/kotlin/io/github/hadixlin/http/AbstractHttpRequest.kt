package io.github.hadixlin.http

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * @author hadix
 * @date 21/11/2016
 */
abstract class AbstractHttpRequest(protected val url: String, protected val http: Http) :
    HttpRequest {
    protected val params: MutableMap<String, List<*>> = LinkedHashMap(10)
    protected val headers: MutableMap<String, List<String>> = LinkedHashMap(10)
    /**
     * 请求体,实际类型为byte[] 或 File.<br></br>
     * 对于File类型直接保存,对于文本使用iso-8809-1转换为byte[]保存<br></br>
     * 对于Object,使用Jackson的ObjectMapper.writeValueAsBytes转换为二进制形式(byte[])保存<br></br>
     */
    protected var body: Any? = null
    protected var method = Method.GET
    protected var mimeType: String? = null
    protected var charset: Charset = StandardCharsets.UTF_8

    override fun param(name: String, vararg value: Any): HttpRequest {
        if (name.isBlank()) {
            throw IllegalArgumentException("参数不能为空")
        }
        params[name] = listOf(value)
        return this
    }

    override fun params(vararg params: Any): HttpRequest {
        this.params.putAll(toMap(params))
        return this
    }

    private fun toMap(pairs: Array<out Any>): Map<String, MutableList<String>> {
        assertPaired(pairs)
        val end = pairs.size - 1
        val paramMap = LinkedHashMap<String, MutableList<String>>(end / 2 + 1)
        var i = 0
        while (i < end) {
            val name = pairs[i].toString()
            val value = pairs[i + 1].toString()
            val valueList = paramMap.computeIfAbsent(name) { mutableListOf() }
            valueList.add(value)
            i += PAIR_LENGTH
        }
        return paramMap
    }

    override fun params(params: Map<String, *>): HttpRequest {
        for ((k, v) in params) {
            v ?: continue
            this.params[k] = if (v is Collection<*>) v.toList() else listOf(v)
        }
        return this
    }

    protected fun paramsText(): String {
        return when {
            body != null && body is ByteArray ->
                String(body as ByteArray, StandardCharsets.UTF_8)
            else -> params.toString()
        }
    }

    override fun params(): Map<String, List<*>> {
        return params.toSortedMap()
    }

    override fun paramsAsDecodingQueryString(): String {
        val query = StringBuilder()
        for ((key, value1) in params) {
            for (value in value1) {
                query.append(key).append('=').append(value).append('&')
            }
        }
        query.deleteCharAt(query.length - 1)
        return query.toString()
    }

    override fun header(name: String, value: String): HttpRequest {
        if (name.isBlank() || value.isBlank()) {
            LOGGER.warn("请求header参数不能为空,name={},value={}", name, value)
            return this
        }
        headers[name] = toHeaderValue(value)
        return this
    }

    private fun toHeaderValue(value: String): List<String> {
        return value.split(';')
    }

    override fun addHeader(name: String, value: String): HttpRequest {
        val valueList: MutableList<String> = headers[name]?.toMutableList() ?: mutableListOf()
        valueList.add(value)
        headers[name] = valueList
        return this
    }

    override fun headers(vararg headers: String): HttpRequest {
        assertPaired(headers)
        this.headers.putAll(toMap(headers))
        return this
    }

    override fun headers(headers: Map<String, String>): HttpRequest {
        for ((key, value) in headers) {
            header(key, value)
        }
        return this
    }

    override fun removeHeader(vararg names: String): HttpRequest {
        for (name in names) {
            this.headers.remove(name)
        }
        return this
    }

    override fun body(binaryBody: ByteArray): HttpRequest {
        this.body = binaryBody
        return this
    }

    override fun body(fileBody: File): HttpRequest {
        this.body = fileBody
        return this
    }

    override fun body(stringBody: String): HttpRequest {
        return body(stringBody.toByteArray(charset))
    }

    override fun body(bean: Any): HttpRequest {
        this.body = Json.toBytes(bean)

        mimeType(MIME_TYPE_APPLICATION_JSON)
        return this
    }

    override fun mimeType(contentType: String): HttpRequest {
        this.mimeType = contentType
        return this
    }

    override fun method(method: Method): HttpRequest {
        this.method = method
        return this
    }

    override fun charset(charset: String): HttpRequest {
        this.charset = Charset.forName(charset)
        return this
    }

    override fun charset(charset: Charset): HttpRequest {
        this.charset = charset
        return this
    }

    private fun assertPaired(pairs: Array<out Any>?) {
        if (pairs == null || pairs.size % PAIR_LENGTH != 0) {
            throw IllegalArgumentException("参数必须成对出现")
        }
    }

    companion object {

        private const val MIME_TYPE_APPLICATION_JSON = "application/json"
        private const val PAIR_LENGTH = 2
        private val LOGGER = LoggerFactory.getLogger(AbstractHttpRequest::class.java)
    }
}
