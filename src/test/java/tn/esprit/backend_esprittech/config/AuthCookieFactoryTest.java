package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieFactoryTest {

    private AuthCookieFactory factory;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setExpirationMs(900_000L);
        properties.setRefreshExpirationMs(604_800_000L);
        factory = new AuthCookieFactory(properties, true, "Strict", "/api", "/api/auth/refresh");
    }

    @Test
    void accessToken_shouldBuildHttpOnlySecureCookieForApiPath() {
        ResponseCookie cookie = factory.accessToken("access.jwt");

        assertEquals(AuthCookieFactory.ACCESS_TOKEN_COOKIE, cookie.getName());
        assertEquals("access.jwt", cookie.getValue());
        assertEquals("/api", cookie.getPath());
        assertEquals(900, cookie.getMaxAge().getSeconds());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
    }

    @Test
    void refreshToken_shouldBeRestrictedToRefreshEndpoint() {
        ResponseCookie cookie = factory.refreshToken("refresh.jwt");

        assertEquals(AuthCookieFactory.REFRESH_TOKEN_COOKIE, cookie.getName());
        assertEquals("refresh.jwt", cookie.getValue());
        assertEquals("/api/auth/refresh", cookie.getPath());
        assertEquals(604_800, cookie.getMaxAge().getSeconds());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
    }

    @Test
    void clearCookies_shouldUseZeroMaxAgeAndSameSecurityAttributes() {
        ResponseCookie access = factory.clearAccessToken();
        ResponseCookie refresh = factory.clearRefreshToken();

        assertEquals(AuthCookieFactory.ACCESS_TOKEN_COOKIE, access.getName());
        assertEquals("", access.getValue());
        assertEquals("/api", access.getPath());
        assertEquals(0, access.getMaxAge().getSeconds());
        assertTrue(access.isHttpOnly());
        assertTrue(access.isSecure());

        assertEquals(AuthCookieFactory.REFRESH_TOKEN_COOKIE, refresh.getName());
        assertEquals("", refresh.getValue());
        assertEquals("/api/auth/refresh", refresh.getPath());
        assertEquals(0, refresh.getMaxAge().getSeconds());
        assertTrue(refresh.isHttpOnly());
        assertTrue(refresh.isSecure());
    }
}
