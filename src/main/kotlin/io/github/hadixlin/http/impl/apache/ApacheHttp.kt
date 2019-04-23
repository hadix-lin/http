package io.github.hadixlin.http.impl.apache

import io.github.hadixlin.http.Http
import io.github.hadixlin.http.HttpRequest
import org.apache.http.client.config.RequestConfig
import org.apache.http.config.SocketConfig
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import java.io.IOException

/** Created by hadix on 22/11/2016.  */
class ApacheHttp private constructor(internal val hc: CloseableHttpClient) : Http {

	override fun newReq(url: String): HttpRequest {
		return ApacheHttpRequest(url, this)
	}

	override fun close() {
		if (hc != null) {
			try {
				hc.close()
			} catch (e: IOException) {
				log.error("关闭HttpClient发生错误 : " + e.message, e)
			}

		}
	}

	class Builder : Http.Builder() {

		private var internalBuilder: HttpClientBuilder? = null

		override fun build(): Http {
			internalBuilder = HttpClients.custom()

			defaultConfigs(internalBuilder!!)

			if (connTimeout() > 0) {
				internalBuilder!!.setDefaultRequestConfig(
					RequestConfig.custom()
						.setConnectTimeout(connTimeout())
						.setConnectionRequestTimeout(100)
						.build())
			}
			if (soTimeout() > 0) {
				internalBuilder!!.setDefaultSocketConfig(
					SocketConfig.custom().setSoTimeout(soTimeout()).build())
			}
			if (maxConnTotal() > 0) {
				internalBuilder!!.setMaxConnTotal(maxConnTotal()).setMaxConnPerRoute(maxConnTotal())
			}

			return ApacheHttp(internalBuilder!!.build())
		}

		private fun defaultConfigs(builder: HttpClientBuilder) {
			builder
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.disableAutomaticRetries()
				.setMaxConnTotal(1000)
				.setMaxConnPerRoute(1000)
				.setDefaultRequestConfig(
					RequestConfig.custom()
						.setConnectTimeout(500)
						.setConnectionRequestTimeout(200)
						.build())
				.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(30000).build())
				.useSystemProperties()
		}
	}

	companion object {

		private val log = LoggerFactory.getLogger(ApacheHttp::class.java)

		fun builder(): Builder {
			return Builder()
		}

		private fun newDefault(): ApacheHttp {
			val instance = Builder().build() as ApacheHttp
			Runtime.getRuntime().addShutdownHook(Thread(Runnable { instance.close() }))
			return instance
		}
	}
}
