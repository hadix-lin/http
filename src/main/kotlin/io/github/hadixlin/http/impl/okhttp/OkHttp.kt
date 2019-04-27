package io.github.hadixlin.http.impl.okhttp

import io.github.hadixlin.http.Http
import io.github.hadixlin.http.HttpRequest
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class OkHttp private constructor(internal val okHttpClient: OkHttpClient) : Http {

    override fun newReq(url: String): HttpRequest {
        return OkHttpRequest(url, this)
    }

    override fun close() {
        // do nothing;
    }

    class Builder : Http.Builder<OkHttpClient.Builder>() {
        override val internalBuilder: OkHttpClient.Builder = OkHttpClient.Builder()

        override fun build(): Http {
            val cacheDir = cacheDir()
            if (cacheDir != null) {
                internalBuilder.cache(Cache(cacheDir, 1000))
            }
            if (connTimeout() > 0) {
                internalBuilder.connectTimeout(connTimeout().toLong(), TimeUnit.SECONDS)
            }
            if (soTimeout() > 0) {
                internalBuilder.readTimeout(soTimeout().toLong(), TimeUnit.SECONDS)
                internalBuilder.writeTimeout(soTimeout().toLong(), TimeUnit.SECONDS)
            }
            internalBuilder.hostnameVerifier { _, _ -> true }
            internalBuilder.retryOnConnectionFailure(false)
            if (log.isDebugEnabled) {
                val logging =
                    HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { log.debug(it) })
                logging.level = Level.BODY
                internalBuilder.addInterceptor(logging)
            }
            return OkHttp(internalBuilder.build())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OkHttp::class.java)

        fun internalBuilder(): Builder {
            return Builder()
        }
    }
}
