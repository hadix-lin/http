package io.github.hadixlin.http;

import org.junit.Assert;
import org.junit.Test;

public class JavaHttpTest {

    @Test
    public void testSubmitForText() {
        String resp = Http.req("http://www.baidu.com").submitForText();
        System.out.println(resp);
        Assert.assertNotNull(resp);
    }
}
