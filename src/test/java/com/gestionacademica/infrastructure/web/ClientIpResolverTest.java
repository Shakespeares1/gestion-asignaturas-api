package com.gestionacademica.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {
    @Test
    void enProdIgnoraHeadersCuandoElPeerNoEsUnProxyConfiable() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("app.proxy.trusted-cidrs", "10.0.0.0/8");
        ClientIpResolver resolver = new ClientIpResolver(environment);
        MockHttpServletRequest request = request("198.51.100.10");
        request.addHeader("X-Forwarded-For", "203.0.113.99");
        request.addHeader("CF-Connecting-IP", "203.0.113.8");

        assertEquals("198.51.100.10", resolver.resolve(request));
    }

    @Test
    void enProdCaminaLaCadenaDesdeElProxyYDescartaPrefijosFalsificados() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("app.proxy.trusted-cidrs", "10.0.0.0/8,203.0.113.0/24");
        ClientIpResolver resolver = new ClientIpResolver(environment);
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader("X-Forwarded-For", "198.51.100.99, 192.0.2.77, 203.0.113.9");
        request.addHeader("CF-Connecting-IP", "198.51.100.88");

        assertEquals("192.0.2.77", resolver.resolve(request));
    }

    @Test
    void cadenaMalformadaUsaElPeerConfiableComoFallbackSeguro() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("app.proxy.trusted-cidrs", "10.0.0.0/8");
        ClientIpResolver resolver = new ClientIpResolver(environment);
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.8, host-malicioso");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    @Test
    void enTestIgnoraHeadersProxyAunqueHayaCidrsConfigurados() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        environment.setProperty("app.proxy.trusted-cidrs", "10.0.0.0/8");
        ClientIpResolver resolver = new ClientIpResolver(environment);
        MockHttpServletRequest request = request("10.0.0.5");
        request.addHeader("CF-Connecting-IP", "203.0.113.8");
        request.addHeader("X-Forwarded-For", "198.51.100.99");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    @Test
    void fallaRapidoAnteUnaListaCidrInvalida() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("app.proxy.trusted-cidrs", "10.0.0.0/99");

        assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver(environment));
    }

    private static MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment;
    }

    private static MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
