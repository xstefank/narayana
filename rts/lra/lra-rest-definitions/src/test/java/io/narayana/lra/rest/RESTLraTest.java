package io.narayana.lra.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.MalformedURLException;
import java.net.URL;

public class RESTLraTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testJsonWithCallback() throws Exception {
        RESTLra lra = RESTLraBuilder.lra()
                .name("testLRA")
                .withAction(RESTAction.post(new URL("http://stub.com")).build())
                .data(42)
                .callback("http://testLocal.org")
                .build();

        String expected = "{" +
                "\"name\":\"testLRA\"," +
                "\"actions\":[{\"target\":\"http://stub.com\",\"callbackUrl\":\"http://stub.com\"}]," +
                "\"data\":42," +
                "\"parentLRA\":null," +
                "\"clientId\":\"\"," +
                "\"timelimit\":0," +
                "\"callbackURL\":\"http://testLocal.org\"," +
                "\"nestedLRAs\":[]" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        Assert.assertEquals(expected, mapper.writeValueAsString(lra));
    }

    @Test
    public void testJsonWithActionCallback() throws Exception {
        RESTLra lra = RESTLraBuilder.lra()
                .name("testLRA")
                .withAction(RESTAction
                        .post(new URL("http://example.com/request"))
                        .callbackUrl(new URL("http://example.com/callback"))
                        .build())
                .data(42)
                .callback("http://testLocal.org")
                .build();

        String expected = "{" +
                "\"name\":\"testLRA\"," +
                "\"actions\":[{\"target\":\"http://example.com/request\",\"callbackUrl\":\"http://example.com/callback\"}]," +
                "\"data\":42," +
                "\"parentLRA\":null," +
                "\"clientId\":\"\"," +
                "\"timelimit\":0," +
                "\"callbackURL\":\"http://testLocal.org\"," +
                "\"nestedLRAs\":[]" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        Assert.assertEquals(expected, mapper.writeValueAsString(lra));
    }

    @Test
    public void testJsonWithNullCallback() throws MalformedURLException {
        expectedException.expect(IllegalArgumentException.class);

        RESTLra lra = RESTLraBuilder.lra()
                .name("testLRA")
                .withAction(RESTAction.post(new URL("http://stub.com")).build())
                .build();
    }
}
