package io.github.hadixlin.http

import io.github.hadixlin.http.impl.apache.ApacheHttp
import io.github.hadixlin.http.impl.okhttp.OkHttp
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/** @author hadix
 */
interface Http : AutoCloseable {

	fun newReq(url: String): HttpRequest

	abstract class Builder {

		private var connTimeout = -1
		private var soTimeout = -1
		private var maxConnTotal = -1
		private var cacheDir: File? = null

		abstract fun build(): Http

		fun connTimeout(): Int {
			return connTimeout
		}

		fun connTimeout(connTimeout: Int): Builder {
			this.connTimeout = connTimeout
			return this
		}

		fun soTimeout(): Int {
			return soTimeout
		}

		fun soTimeout(soTimeout: Int): Builder {
			this.soTimeout = soTimeout
			return this
		}

		fun maxConnTotal(): Int {
			return maxConnTotal
		}

		fun maxConnTotal(maxConnTotal: Int): Builder {
			this.maxConnTotal = maxConnTotal
			return this
		}

		fun cacheDir(): File? {
			return cacheDir
		}

		/** 缓存支持仅在使用okhttp实现时有效  */
		fun cacheDir(cacheDir: File): Builder {
			this.cacheDir = cacheDir
			return this
		}
	}

	companion object {
		private val log: Logger = LoggerFactory.getLogger(Http::class.java)

		private val INSTANCE = createInstance()

		fun createInstance(): Http {
			return newBuilder().build()
		}

		fun newBuilder(): Builder {
			var builder: Builder
			try {
				Class.forName("okhttp3.OkHttpClient")
				builder = OkHttp.builder()
			} catch (e: ClassNotFoundException) {
				try {
					Class.forName("org.apache.http.client.HttpClient")
					builder = ApacheHttp.builder()
				} catch (e1: ClassNotFoundException) {
					log.error("没有找到Http实现,需要Apache HttpClient或OkHttp")
					throw UnsupportedOperationException("没有找到Http实现,需要Apache HttpClient或OkHttp")
				}

			}
			return builder
		}

		fun req(url: String): HttpRequest {
			return INSTANCE.newReq(url)
		}
	}
}
