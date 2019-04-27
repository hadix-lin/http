package io.github.hadixlin.http.impl.okhttp

import com.fasterxml.jackson.core.type.TypeReference
import io.github.hadixlin.http.AbstractHttpRequest
import io.github.hadixlin.http.HttpException
import io.github.hadixlin.http.Json
import io.github.hadixlin.http.Method.*
import io.github.hadixlin.http.ResponseHandler
import okhttp3.*
import java.io.File
import java.io.IOException
import java.io.InputStream

/** @author hadix
 */
class OkHttpRequest internal constructor(url: String, http: OkHttp) :
    AbstractHttpRequest(url, http) {

    private val client: OkHttpClient = http.okHttpClient

    private fun httpUrl(): HttpUrl {
        val urlBuilder = HttpUrl.Builder()
        val parsedUrl = HttpUrl.parse(url) ?: throw IllegalArgumentException("url[$url]不合法")
        urlBuilder.scheme(parsedUrl.scheme())
        urlBuilder.host(parsedUrl.host())
        urlBuilder.port(parsedUrl.port())
        urlBuilder.encodedPath(parsedUrl.encodedPath())
        urlBuilder.encodedQuery(parsedUrl.encodedQuery())
        params.forEach { (name, values) ->
            values.forEach { v ->
                urlBuilder.addQueryParameter(
                    name,
                    v.toString()
                )
            }
        }
        return urlBuilder.build()
    }

    private fun requestBody(): RequestBody? =
        if (MultipartBody.FORM == mimeType()) {
            createMultiRequestBody()
        } else {
            createTextRequestBody()
        }

    private fun mimeType(): MediaType? {
        val mimeType = this.mimeType
        return if (mimeType != null)
            MediaType.parse(mimeType)
        else
            MediaType.parse(DEFAULT_MIME_TYPE)
    }

    override fun <T> submitForStream(
        inputHandler: ResponseHandler<InputStream, T>,
        errorHandler: ResponseHandler<HttpException, T>
    ): T {
        try {
            submit().use { body ->
                return inputHandler.apply(body.byteStream())
            }
        } catch (e: HttpException) {
            return errorHandler.handleError(e)
        } catch (e: Exception) {
            val error = wrapThenRethrow(e)
            return errorHandler.handleError(error)
        }
    }

    private fun submit(): ResponseBody {
        val req = createRequest()
        val call = client.newCall(req)
        try {
            val resp = call.execute()
            if (resp.code() >= HTTP_STATUS_300) {
                resp.close()
                throw HttpException(resp.code(), url, params.toString())
            }
            return resp.body() ?: throw HttpException(url, paramsText(), "没有响应体")
        } catch (e: IOException) {
            throw wrapThenRethrow(e)
        }
    }

    private fun createRequest(): Request {
        val builder = Request.Builder()
        headers.forEach { (name, values) -> values.forEach { v -> builder.addHeader(name, v) } }
        when (method) {
            POST, PUT, PATCH -> {
                builder.method(method.name, requestBody())
                builder.url(url)
            }
            GET, DELETE, HEAD -> {
                builder.url(httpUrl())
                builder.method(method.name, null)
            }
        }
        return builder.build()
    }

    private fun createMultiRequestBody(): RequestBody {
        val builder = MultipartBody.Builder()
        params.forEach { (name, values) ->
            values.forEach { value ->
                when (value) {
                    is File -> builder.addFormDataPart(
                        name,
                        value.name,
                        MultipartBody.create(null, value)
                    )
                    is ByteArray -> builder.addFormDataPart(
                        name,
                        null,
                        MultipartBody.create(null, value)
                    )
                    else -> builder.addFormDataPart(name, value.toString())
                }
            }
        }
        builder.setType(MultipartBody.FORM)
        return builder.build()
    }

    private fun createTextRequestBody(): RequestBody? {
        val mediaType = mimeType()
        return when {
            body != null -> createTextRequestBodyUseBody(mediaType)
            params.isNotEmpty() -> createTextRequestBodyUseParams()
            else -> RequestBody.create(mediaType, ByteArray(0))
        }
    }

    private fun createTextRequestBodyUseBody(mediaType: MediaType?): RequestBody? {
        if (body != null) {
            if (body is ByteArray) {
                return RequestBody.create(mediaType, body as ByteArray)
            } else if (body is File) {
                return RequestBody.create(mediaType, body as File)
            }
        }
        return null
    }

    private fun createTextRequestBodyUseParams(): RequestBody {
        val bodyBuilder = FormBody.Builder(charset)
        params.forEach { (name, values) ->
            values.forEach { v ->
                bodyBuilder.add(
                    name,
                    v.toString()
                )
            }
        }
        return bodyBuilder.build()
    }

    private fun <T> submit(responseHandler: (ResponseBody) -> T): T {
        try {
            submit().use { body -> return responseHandler(body) }
        } catch (e: Exception) {
            throw wrapThenRethrow(e)
        }
    }

    @Throws(HttpException::class)
    override fun submitForText(): String {
        return submit { it.string() }
    }

    @Throws(HttpException::class)
    override fun submitForFile(): File {
        return submit { body ->
            val tmpFile = createTempFile()
            body.byteStream().use { input ->
                tmpFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tmpFile
        }
    }

    @Throws(HttpException::class)
    override fun <T> submitForObject(clazz: Class<T>): T {
        return submit { body -> Json.toBean(body.byteStream(), clazz) }
    }

    @Throws(HttpException::class)
    override fun <T> submitForObject(typeReference: TypeReference<T>): T {
        return submit { body -> Json.toBean(body.byteStream(), typeReference) }
    }

    private fun wrapThenRethrow(e: Exception): HttpException {
        return if (e is HttpException) e else HttpException(url, params.toString(), e)
    }

    companion object {
        private const val HTTP_STATUS_300 = 300
        private const val DEFAULT_MIME_TYPE = "text/plain"
    }
}
