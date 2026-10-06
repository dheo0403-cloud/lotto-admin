package com.example.lotto.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AdminTokenInterceptorTest {

    private boolean call(String configured, String method, String header, MockHttpServletResponse res) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, "/api/v1/lotto/recommend");
        if (header != null) req.addHeader("X-Admin-Token", header);
        return new AdminTokenInterceptor(configured).preHandle(req, res, new Object());
    }

    @Test
    void 조회는_토큰_없이_허용() throws Exception {
        assertTrue(call("secret", "GET", null, new MockHttpServletResponse()));
    }

    @Test
    void 올바른_토큰이면_쓰기_허용() throws Exception {
        assertTrue(call("secret", "POST", "secret", new MockHttpServletResponse()));
    }

    @Test
    void 토큰_누락_또는_불일치면_401() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(call("secret", "POST", null, res));
        assertEquals(401, res.getStatus());
        assertFalse(call("secret", "POST", "wrong", new MockHttpServletResponse()));
    }

    @Test
    void 서버_토큰_미설정이면_쓰기_전부_차단() throws Exception {
        assertFalse(call("", "POST", "", new MockHttpServletResponse()));
        assertFalse(call("", "POST", "anything", new MockHttpServletResponse()));
    }
}
