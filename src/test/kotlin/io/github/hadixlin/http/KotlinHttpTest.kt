package io.github.hadixlin.http

import io.github.hadixlin.http.Method.PUT
import org.junit.Test
import kotlin.test.assertNotNull

class KotlinHttpTest {

    @Test
    fun testSubmitForText() {
        val resp = Http.req("http://www.baidu.com").method(PUT).submitForText()
        println(resp)
        assertNotNull(resp)
    }
}
