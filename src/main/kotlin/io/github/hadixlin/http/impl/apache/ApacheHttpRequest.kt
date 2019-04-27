package io.github.hadixlin.http.impl.apache

import com.fasterxml.jackson.core.type.TypeReference
import io.github.hadixlin.http.AbstractHttpRequest
import io.github.hadixlin.http.HttpException
import io.github.hadixlin.http.Json
import io.github.hadixlin.http.Method.*
import io.github.hadixlin.http.ResponseHandler
import org.apache.http.HttpEntity
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType.create
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.*

/**
 * 基于Apache的实现
 *
 *
 * Created by hadix on 21/11/2016.
 */
internal class ApacheHttpRequest(url: String, http: ApacheHttp) : AbstractHttpRequest(url, http) {
    private fun httpEntity(): HttpEntity {
        return if (MULTIPART_FORM_DATA == mimeType) {
            createMultipartEntity()
        } else {
            createTextEntity()
        }
    }

    private fun nameValuePairs(): List<NameValuePair> {
        val nvps = ArrayList<NameValuePair>(params.size)
        for (entry in params.entries) {
            for (value in entry.value) {
                nvps.add(BasicNameValuePair(entry.key, value.toString()))
            }
        }
        return nvps
    }

    private fun requestUri(): URI {
        return URIBuilder(url).addParameters(nameValuePairs()).build()
    }


    @Throws(HttpException::class)
    private fun submit(): CloseableHttpResponse {
        val requestBuilder = createRequestBuilder()
        val req = requestBuilder.build()
        val resp: CloseableHttpResponse
        try {
            resp = (http as ApacheHttp).hc.execute(req)
            assertSuccess(resp)
        } catch (e: IOException) {
            return wrapThenRethrow(req, e)
        }
        return resp
    }

    /**
     * 创建一个RequestBuilder,该Builder会根据依先前的调用进行配置
     *
     * @return 经过配置的RequestBuilder
     */
    private fun createRequestBuilder(): RequestBuilder {
        val requestBuilder = RequestBuilder.create(method.toString()).setUri(url)
        when (method) {
            POST, PUT, PATCH -> {
                val entity = httpEntity()
                logIfDebug(entity)
                requestBuilder.entity = entity
            }
            GET, DELETE, HEAD -> {
                val uri = requestUri()
                requestBuilder.uri = uri
                log.debug("uri = {}", uri)
            }
        }
        addHeaders(requestBuilder)
        return requestBuilder
    }

    private fun logIfDebug(entity: HttpEntity) {
        if (!log.isDebugEnabled) return
        try {
            log.debug(
                "uri = {}, body = {}", url,
                if (entity.isRepeatable) EntityUtils.toString(entity, charset) else "unrepeatable"
            )
        } catch (e: IOException) {
            log.error(e.message, e)
        }
    }

    private fun createMultipartEntity(): HttpEntity {
        val entityBuilder = MultipartEntityBuilder.create()
        params.forEach { (k, v) ->
            v.forEach { value ->
                when (value) {
                    is File -> entityBuilder.addBinaryBody(k, value)
                    is InputStream -> entityBuilder.addBinaryBody(k, value)
                    is ByteArray -> entityBuilder.addBinaryBody(k, value)
                    else -> entityBuilder.addTextBody(k, value.toString())
                }
            }
        }
        return entityBuilder.build()
    }

    private fun createTextEntity(): HttpEntity {
        val entityBuilder = EntityBuilder.create()
        if (body != null) {
            if (body is ByteArray) {
                entityBuilder.binary = body as ByteArray
            } else if (body is File) {
                entityBuilder.file = body as File
            }
            if (mimeType != null) {
                entityBuilder.contentType = create(mimeType, charset)
            }
        } else {
            entityBuilder.parameters = nameValuePairs()
            entityBuilder.contentType = create(APPLICATION_X_WWW_FORM_URLENCODED, charset)
        }
        return entityBuilder.build()
    }

    private fun addHeaders(requestBuilder: RequestBuilder) {
        for ((name, value1) in headers) {
            for (value in value1) {
                requestBuilder.addHeader(name, value)
            }
        }
    }

    private fun assertSuccess(resp: CloseableHttpResponse) {
        val statusLine = resp.statusLine
        val entity = resp.entity
        if (statusLine.statusCode >= HTTP_STATUS_300) {
            val errorResp = toErrorResp(entity)
            throw HttpException(
                statusLine.statusCode, url, "$params=>resp:$errorResp"
            )
        }
    }

    private fun toErrorResp(entity: HttpEntity): String? {
        var errorResp: String? = null
        try {
            errorResp = EntityUtils.toString(entity)
        } catch (e: Exception) {
            if (log.isWarnEnabled) {
                log.warn("响应体无法转换为文本:" + e.message)
            } else {
                e.printStackTrace()
            }
        }

        return errorResp
    }

    private fun <T> submit(handler: (HttpResponse) -> T): T {
        try {
            submit().use { resp ->
                return handler(resp)
            }
        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            throw HttpException(url, paramsText(), e)
        }

    }

    override fun <T> submitForStream(
        inputHandler: ResponseHandler<InputStream, T>,
        errorHandler: ResponseHandler<HttpException, T>
    ): T {
        try {
            submit().use { resp ->
                return inputHandler.apply(resp.entity.content)
            }
        } catch (e: Exception) {
            return errorHandler.handleError(HttpException(url, paramsText(), e))
        }
    }

    @Throws(HttpException::class)
    override fun submitForText(): String {
        try {
            submit().use { resp ->
                return EntityUtils.toString(resp.entity, "utf8")
            }
        } catch (e: IOException) {
            throw HttpException(url, paramsText(), e)
        }

    }

    @Throws(HttpException::class)
    override fun submitForFile(): File {
        return submit { resp: HttpResponse ->
            val entity = resp.entity
            val inputStream = entity.content
            val tmpFile = createTempFile()
            val outputStream = tmpFile.outputStream()
            inputStream.use { input ->
                outputStream.use { out ->
                    input.copyTo(out)
                }
            }
            inputStream.copyTo(outputStream)
            tmpFile
        }
    }

    override fun <T> submitForObject(clazz: Class<T>): T {
        return submit { resp -> Json.toBean(resp.entity.content, clazz) }
    }

    override fun <T> submitForObject(typeReference: TypeReference<T>): T {
        return submit { resp -> Json.toBean(resp.entity.content, typeReference) }
    }

    companion object {

        private val log = LoggerFactory.getLogger(ApacheHttpRequest::class.java)
        private const val APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"
        private const val MULTIPART_FORM_DATA = "multipart/form-data"
        private const val HTTP_STATUS_300 = 300

        private fun wrapThenRethrow(req: HttpUriRequest, e: IOException): CloseableHttpResponse {
            val uri = req.method + " : " + req.uri.toString()
            val params = getParamsText(req)
            throw HttpException(uri, params, e)
        }

        private fun getParamsText(req: HttpUriRequest): String {
            val params: String
            params = if (req is HttpEntityEnclosingRequest) {
                val reqEntity = req.entity
                if (reqEntity == null) {
                    ""
                } else if (reqEntity.isRepeatable && !reqEntity.isStreaming) {
                    toString(reqEntity)
                } else {
                    "请求内容不能转换为文本,请求体长度为" + reqEntity.contentLength
                }
            } else {
                ""
            }
            return params
        }

        private fun toString(reqEntity: HttpEntity): String {
            return try {
                EntityUtils.toString(reqEntity)
            } catch (e: IOException) {
                log.warn("请求体转换为文本失败", e)
                e.message ?: ""
            }
        }
    }
}
