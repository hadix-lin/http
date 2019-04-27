package io.github.hadixlin.http

import org.junit.Test
import kotlin.test.assertNotNull

class KotlinHttpTest {

    @Test
    fun testSubmitForText() {
        val resp = Http.req("http://www.baidu.com").submitForText()
        println(resp)
        assertNotNull(resp)
    }
}
