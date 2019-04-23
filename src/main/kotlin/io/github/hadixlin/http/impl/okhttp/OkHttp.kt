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

	class Builder : Http.Builder() {
		private val builder = OkHttpClient.Builder()

		override fun build(): Http {
			val cacheDir = cacheDir()
			if (cacheDir != null) {
				builder.cache(Cache(cacheDir, 1000))
			}
			if (connTimeout() > 0) {
				builder.connectTimeout(connTimeout().toLong(), TimeUnit.SECONDS)
			}
			if (soTimeout() > 0) {
				builder.readTimeout(soTimeout().toLong(), TimeUnit.SECONDS)
				builder.writeTimeout(soTimeout().toLong(), TimeUnit.SECONDS)
			}
			builder.hostnameVerifier { _, _ -> true }
			builder.retryOnConnectionFailure(false)
			if (log.isDebugEnabled) {
				val logging = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { log.debug(it) })
				logging.level = Level.BODY
				builder.addInterceptor(logging)
			}
			return OkHttp(builder.build())
		}
	}

	companion object {
		private val log = LoggerFactory.getLogger(OkHttp::class.java)

		fun builder(): Builder {
			return Builder()
		}
	}
}
