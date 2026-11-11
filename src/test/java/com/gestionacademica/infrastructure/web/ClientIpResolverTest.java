package com.gestionacademica.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {
    @Test
    void enProdSoloUsaElHeaderCloudflareYNuncaXForwardedFor() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setActiveProfiles("prod");
        ClientIpResolver resolver = new ClientIpResolver(environment);
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "198.51.100.99");
        request.addHeader("CF-Connecting-IP", "203.0.113.8");

        assertEquals("203.0.113.8", resolver.resolve(request));
    }

    @Test
    void direccionCloudflareInvalidaUsaLaConexionYNoUnHeaderAlternativo() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ClientIpResolver resolver = new ClientIpResolver(environment);
        MockHttpServletRequest request = request();
        request.addHeader("CF-Connecting-IP", "203.0.113.1, 198.51.100.2");
        request.addHeader("X-Forwarded-For", "198.51.100.99");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    @Test
    void enPerfilesNoProductivosIgnoraHeadersProxy() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        ClientIpResolver resolver = new ClientIpResolver(environment);
        MockHttpServletRequest request = request();
        request.addHeader("CF-Connecting-IP", "203.0.113.8");
        request.addHeader("X-Forwarded-For", "198.51.100.99");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        return request;
    }
}
