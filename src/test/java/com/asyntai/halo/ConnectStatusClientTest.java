package com.asyntai.halo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConnectStatusClientTest {

    private final ConnectStatusClient client = new ConnectStatusClient("https://asyntai.com");

    @Test
    void stateFormat() {
        assertTrue(ConnectStatusClient.isValidState("halo_0123456789abcdef"));
        assertFalse(ConnectStatusClient.isValidState("halo_0123456789ABCDEF"));
        assertFalse(ConnectStatusClient.isValidState("neos_0123456789abcdef"));
        assertFalse(ConnectStatusClient.isValidState("halo_0123"));
        assertFalse(ConnectStatusClient.isValidState("halo_0123456789abcdef/../x"));
        assertFalse(ConnectStatusClient.isValidState(null));
    }

    @Test
    void notConnectedYet() {
        assertFalse(client.parse("asyntaiHalo(null)").connected());
        assertFalse(client.parse("asyntaiHalo(null);").connected());
        assertFalse(client.parse("").connected());
        assertFalse(client.parse(null).connected());
    }

    @Test
    void connectedReplyIsRead() {
        var s = client.parse("asyntaiHalo({\"site_id\": \"asyntai_abc123\", "
            + "\"script_url\": \"https://widget.asyntai.com/static/js/chat-widget.js\", "
            + "\"account_email\": \"a@example.com\"})");
        assertTrue(s.connected());
        assertEquals("asyntai_abc123", s.siteId());
        assertEquals("https://widget.asyntai.com/static/js/chat-widget.js", s.scriptUrl());
        assertEquals("a@example.com", s.accountEmail());
    }

    @Test
    void anythingUnexpectedIsRejected() {
        // Another callback name, code instead of data, or a bad widget ID.
        assertFalse(client.parse("evil({\"site_id\":\"asyntai_abc\"})").connected());
        assertFalse(client.parse("asyntaiHalo(alert(1))").connected());
        assertFalse(client.parse("asyntaiHalo({\"site_id\":\"x<script>\"})").connected());
        assertFalse(client.parse("asyntaiHalo([1,2])").connected());
        assertFalse(client.parse("asyntaiHalo({\"site_id\":\"asyntai_a\"}); alert(1)").connected());
        assertFalse(client.parse("asyntaiHalo(" + "x".repeat(20000) + ")").connected());
    }

    @Test
    void untrustedValuesAreDropped() {
        var s = client.parse("asyntaiHalo({\"site_id\":\"asyntai_abc\","
            + "\"script_url\":\"https://evil.example/x.js\",\"account_email\":\"<b>x</b>\"})");
        assertTrue(s.connected());
        assertEquals("", s.scriptUrl());
        assertEquals("", s.accountEmail());
    }

    @Test
    void testOriginOnlyLocal() {
        System.setProperty("asyntai.origin", "http://127.0.0.1:8097");
        assertEquals("http://127.0.0.1:8097", ConnectStatusClient.originFromSystem());
        System.setProperty("asyntai.origin", "https://evil.example");
        assertEquals("https://asyntai.com", ConnectStatusClient.originFromSystem());
        System.clearProperty("asyntai.origin");
        assertEquals("https://asyntai.com", ConnectStatusClient.originFromSystem());
    }
}
