package io.github.hadixlin.http

@FunctionalInterface
interface ResponseHandler<T, R> {
	@Throws(Exception::class)
	fun apply(resp: T): R

	fun handleError(error: T): R {
		try {
			return apply(error)
		} catch (e: Exception) {
			var runtimeException = IllegalStateException("处理错误发生异常", e)
			if (error is Throwable) {
				runtimeException.addSuppressed(error as Throwable)
			} else {
				runtimeException = IllegalStateException("处理错误失败:" + error.toString(), e)
			}
			throw runtimeException
		}

	}
}
