package io.github.hadixlin.http

/**
 * @author hadix
 * @date 25/10/2016
 */
class HttpException : RuntimeException {

    private val reqUri: String
    private val params: String
    private val code: Int

    constructor(code: Int, reqUri: String, params: String)
        : super("$code $reqUri : $params") {
        this.reqUri = reqUri
        this.params = params
        this.code = code
    }

    constructor(reqUri: String, params: String, cause: Throwable)
        : this(0, reqUri, params, cause)

    constructor(code: Int, reqUri: String, params: String, cause: Throwable)
        : super("$code $reqUri : $params", cause) {
        this.reqUri = reqUri
        this.params = params
        this.code = code
    }

    constructor(reqUri: String, params: String, message: String)
        : super("$reqUri : $params $message") {
        this.reqUri = reqUri
        this.params = params
        this.code = 0
    }
}
