package com.daccaauto.pos.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseInvoiceAiServiceTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final PurchaseInvoiceAiService service = new PurchaseInvoiceAiService("demo", "", "gemini-3.1-flash-lite");
    private static final String LINE = """
            {"name":"Filter","partNumber":null,"sku":null,"brand":null,"category":null,
             "supplierProductCode":null,"quantity":2,"unitPrice":12.34,"taxPercent":5}
            """;
    private static String invoice(String lines) {
        return """
                {"supplierName":"Supplier","invoiceId":"INV-1","purchaseDate":"2026-10-24",
                 "supplierContactPerson":null,"supplierPhone":null,"supplierEmail":null,
                 "supplierAddress":null,"supplierTrnNumber":null,
                 "currency":"AED","total":25.914,"lines":[%s],"warnings":[]}
                """.formatted(lines);
    }
    private byte[] envelope(String json, String finish) {
        return mapper.writeValueAsBytes(Map.of("candidates", new Object[] {Map.of("finishReason", finish,
                "content", Map.of("parts", new Object[] {Map.of("text", json)}))}));
    }
    private static MockMultipartFile image(String format) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, out);
        return new MockMultipartFile("file", "untrusted.txt", "text/plain", out.toByteArray());
    }
    private static void status(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode());
    }

    @Test void preservesSupplierNameAndContactDetailsForBothProviders() {
        String json = invoice(LINE).replace("\"supplierName\":\"Supplier\"", "\"supplierName\":\"GWD L.L.C\"")
                .replace("\"supplierContactPerson\":null", "\"supplierContactPerson\":\"Ali\"")
                .replace("\"supplierPhone\":null", "\"supplierPhone\":\"+971501234567\"")
                .replace("\"supplierEmail\":null", "\"supplierEmail\":\"sales@example.com\"")
                .replace("\"supplierAddress\":null", "\"supplierAddress\":\"Dubai\"")
                .replace("\"supplierTrnNumber\":null", "\"supplierTrnNumber\":\"123456789\"");
        var result = service.parseResponse(envelope(json, "STOP"));
        assertEquals("GWD L.L.C", result.supplierName());
        assertEquals("Ali", result.supplierContactPerson());
        assertEquals("+971501234567", result.supplierPhone());
        assertEquals("sales@example.com", result.supplierEmail());
        assertEquals("Dubai", result.supplierAddress());
        assertEquals("123456789", result.supplierTrnNumber());
        assertEquals(result, service.parseAnthropicResponse(anthropicEnvelope(json, "end_turn")));
    }

    @Test void demoIsExplicitAndNeverUsedForExtraction() {
        assertTrue(service.demo().warnings().getFirst().contains("SAMPLE ONLY"));
        status(HttpStatus.SERVICE_UNAVAILABLE, () -> service.extract(image("png")));
        status(HttpStatus.SERVICE_UNAVAILABLE,
                () -> new PurchaseInvoiceAiService("gemini", "", "gemini-3.1-flash-lite").extract(image("png")));
        assertThrows(UnsupportedOperationException.class, () -> service.demo().lines().clear());
    }

    @Test void validatesRealImageBytesNotFilenameOrContentType() throws Exception {
        service.validateImage(image("png"));
        service.validateImage(image("jpeg"));
        status(HttpStatus.BAD_REQUEST, () -> service.validateImage(null));
        status(HttpStatus.BAD_REQUEST, () -> service.validateImage(new MockMultipartFile("file", new byte[0])));
        status(HttpStatus.BAD_REQUEST, () -> service.validateImage(new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes())));
        status(HttpStatus.BAD_REQUEST, () -> service.validateImage(new MockMultipartFile("file", new byte[] {(byte)255, (byte)216, (byte)255})));
        status(HttpStatus.PAYLOAD_TOO_LARGE, () -> service.validateImage(new MockMultipartFile("file", new byte[3 * 1024 * 1024 + 1])));
    }

    @Test void rejectsHugeDimensionsBeforeDecoding() throws Exception {
        byte[] png = image("png").getBytes();
        java.nio.ByteBuffer.wrap(png, 16, 8).putInt(100_000).putInt(100_000);
        CRC32 crc = new CRC32(); crc.update(png, 12, 17);
        java.nio.ByteBuffer.wrap(png, 29, 4).putInt((int) crc.getValue());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.validateImage(new MockMultipartFile("file", png)));
        assertTrue(error.getReason().contains("25 million"));
    }

    @Test void parsesDecimalsAndMarksUnknownFields() {
        var result = service.parseResponse(envelope(invoice(LINE), "STOP"));
        assertEquals(new BigDecimal("25.914"), result.total());
        assertEquals(new BigDecimal("12.34"), result.lines().getFirst().unitPrice());
        assertNull(result.lines().getFirst().sku());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("sku is unknown")));
    }

    @Test void invalidNumbersBecomeNullWithWarningsNeverInvented() {
        var result = service.parseResponse(envelope(invoice(LINE.replace("\"quantity\":2", "\"quantity\":1.5")
                .replace("\"unitPrice\":12.34", "\"unitPrice\":-1").replace("\"taxPercent\":5", "\"taxPercent\":101"))
                .replace("2026-10-24", "2026-02-30").replace("AED", "BAD"), "STOP"));
        assertNull(result.purchaseDate()); assertNull(result.currency());
        assertNull(result.lines().getFirst().quantity());
        assertNull(result.lines().getFirst().unitPrice()); assertNull(result.lines().getFirst().taxPercent());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("quantity is invalid")));
    }

    @Test void overlongFieldsBecomeNullWithActionableWarningsForBothProviders() {
        var limits = Map.of("name", 200, "partNumber", 100, "sku", 100, "brand", 100, "category", 100,
                "supplierProductCode", 100, "supplierName", 150, "invoiceId", 80, "purchaseDate", 10, "currency", 3);
        for (var entry : limits.entrySet()) {
            var document = (tools.jackson.databind.node.ObjectNode) mapper.readTree(invoice(LINE));
            var target = document.has(entry.getKey()) ? document : (tools.jackson.databind.node.ObjectNode) document.path("lines").get(0);
            target.put(entry.getKey(), "X".repeat(entry.getValue() + 1));
            String json = mapper.writeValueAsString(document);
            var gemini = service.parseResponse(envelope(json, "STOP"));
            assertEquals(gemini, service.parseAnthropicResponse(anthropicEnvelope(json, "end_turn")));
            var result = mapper.valueToTree(gemini);
            var field = result.has(entry.getKey()) ? result.get(entry.getKey()) : result.path("lines").get(0).get(entry.getKey());
            assertTrue(field.isNull(), entry.getKey());
            assertTrue(gemini.warnings().stream().anyMatch(w -> w.contains(entry.getKey() + " exceeds " + entry.getValue())
                    && w.contains("manually") && w.contains("not truncated")), entry.getKey());
        }
        String boundary = invoice(LINE.replace("\"name\":\"Filter\"", "\"name\":\"" + "X".repeat(200) + "\""));
        assertEquals(200, service.parseResponse(envelope(boundary, "STOP")).lines().getFirst().name().length());
    }

    @Test void rejectsStringsOverHardSafetyLimitInsteadOfReturningThem() {
        String json = invoice(LINE.replace("\"name\":\"Filter\"", "\"name\":\"" + "X".repeat(2001) + "\""));
        status(HttpStatus.BAD_GATEWAY, () -> service.parseResponse(envelope(json, "STOP")));
        status(HttpStatus.BAD_GATEWAY, () -> service.parseAnthropicResponse(anthropicEnvelope(json, "end_turn")));
    }

    @Test void failsClosedForMissingMalformedTruncatedOrTooManyLines() {
        for (String json : new String[] {"{}", "not JSON", invoice(LINE).replace("\"total\":25.914,", ""),
                invoice(LINE.replace("\"quantity\":2", "\"quantity\":\"2\"")), invoice(LINE) + " {}",
                invoice(String.join(",", java.util.Collections.nCopies(101, LINE)))}) {
            status(HttpStatus.BAD_GATEWAY, () -> service.parseResponse(envelope(json, "STOP")));
        }
        status(HttpStatus.BAD_GATEWAY, () -> service.parseResponse(envelope(invoice(LINE), "MAX_TOKENS")));
        status(HttpStatus.BAD_GATEWAY, () -> service.parseResponse("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}".getBytes()));
    }

    @Test void sendsGeminiInlineImageAndSchemaToLocalFixture() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> key = new AtomicReference<>();
        server.createContext("/", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            path.set(exchange.getRequestURI().toString()); key.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            byte[] response = envelope(invoice(LINE), "STOP");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        server.start();
        try {
            var client = fixture(server);
            assertEquals("INV-1", client.extract(image("png")).invoiceId());
            var body = mapper.readTree(requestBody.get());
            assertEquals("image/png", body.path("contents").get(0).path("parts").get(1).path("inlineData").path("mimeType").asText());
            assertFalse(body.path("contents").get(0).path("parts").get(1).path("inlineData").path("data").asText().isEmpty());
            assertTrue(body.path("generationConfig").path("responseJsonSchema").path("properties").path("lines").path("description").asText().contains("100"));
            assertEquals("fixture-secret", key.get());
            assertEquals("/v1beta/models/gemini-3.1-flash-lite:generateContent", path.get());
            assertFalse(body.path("generationConfig").has("thinkingConfig"));
            fixture(server, "gemini", "gemini-3.8-flash").extract(image("png"));
            assertEquals("low", mapper.readTree(requestBody.get()).path("generationConfig").path("thinkingConfig").path("thinkingLevel").asText());
        } finally { server.stop(0); }
    }

    private byte[] anthropicEnvelope(String json, String stop) {
        return mapper.writeValueAsBytes(Map.of("type", "message", "role", "assistant", "stop_reason", stop,
                "content", new Object[] {Map.of("type", "text", "text", json)}));
    }

    @Test void sendsAnthropicImageWithSharedSchemaAndPreservedTaxPrompt() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> anthropicBody = new AtomicReference<>(), geminiBody = new AtomicReference<>();
        AtomicReference<String> key = new AtomicReference<>(), version = new AtomicReference<>();
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean anthropic = exchange.getRequestURI().getPath().equals("/v1/messages");
            if (anthropic) {
                anthropicBody.set(body);
                key.set(exchange.getRequestHeaders().getFirst("x-api-key"));
                version.set(exchange.getRequestHeaders().getFirst("anthropic-version"));
            } else { geminiBody.set(body); }
            byte[] response = anthropic ? anthropicEnvelope(invoice(LINE), "end_turn") : envelope(invoice(LINE), "STOP");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        server.start();
        try {
            var anthropic = fixture(server, "anthropic", "");
            assertEquals(fixture(server).extract(image("png")), anthropic.extract(image("png")));
            var body = mapper.readTree(anthropicBody.get());
            var gemini = mapper.readTree(geminiBody.get());
            assertEquals("claude-haiku-5-5", body.path("model").asText());
            assertEquals(16000, body.path("max_tokens").asInt());
            assertEquals("disabled", body.path("thinking").path("type").asText());
            assertEquals("low", body.path("output_config").path("effort").asText());
            assertEquals("json_schema", body.path("output_config").path("format").path("type").asText());
            assertEquals(gemini.path("generationConfig").path("responseJsonSchema"), body.path("output_config").path("format").path("schema"));
            var source = body.path("messages").get(0).path("content").get(0).path("source");
            assertEquals("base64", source.path("type").asText());
            assertEquals("image/png", source.path("media_type").asText());
            assertArrayEquals(image("png").getBytes(), java.util.Base64.getDecoder().decode(source.path("data").asText()));
            String prompt = body.path("messages").get(0).path("content").get(1).path("text").asText();
            assertEquals(gemini.path("contents").get(0).path("parts").get(0).path("text").asText(), prompt);
            assertTrue(prompt.contains("only shows tax-inclusive prices, leave unitPrice null"));
            assertEquals("fixture-secret", key.get()); assertEquals("2026-10-21", version.get());
            assertFalse(body.has("temperature")); assertFalse(body.has("top_p"));
        } finally { server.stop(0); }
    }

    @Test void anthropicUsesSameStrictValidationAndRejectsRefusalsOrTruncation() {
        for (String stop : new String[] {"refusal", "max_tokens", "tool_use", ""}) {
            status(HttpStatus.BAD_GATEWAY, () -> service.parseAnthropicResponse(anthropicEnvelope(invoice(LINE), stop)));
        }
        for (String json : new String[] {"{}", "not JSON", invoice(LINE) + " {}",
                invoice(LINE).replace("\"total\":25.914,", ""),
                invoice(String.join(",", java.util.Collections.nCopies(101, LINE))),
                invoice(LINE.replace("\"quantity\":2", "\"quantity\":\"2\""))}) {
            status(HttpStatus.BAD_GATEWAY, () -> service.parseAnthropicResponse(anthropicEnvelope(json, "end_turn")));
        }
        status(HttpStatus.BAD_GATEWAY, () -> service.parseAnthropicResponse("{\"type\":\"error\"}".getBytes()));
        var result = service.parseAnthropicResponse(anthropicEnvelope(invoice(LINE.replace("\"quantity\":2", "\"quantity\":0")
                .replace("\"unitPrice\":12.34", "\"unitPrice\":null")), "end_turn"));
        assertNull(result.lines().getFirst().quantity()); assertNull(result.lines().getFirst().unitPrice());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("quantity is invalid")));
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("unitPrice is unknown")));
    }

    @Test void anthropicProviderErrorsAreSafeAndNeverFallBackToDemo() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] response = "PRIVATE INVOICE fixture-secret".getBytes();
            exchange.sendResponseHeaders(429, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        server.start();
        try {
            var error = assertThrows(ResponseStatusException.class,
                    () -> fixture(server, "anthropic", "claude-haiku-5-5").extract(image("jpeg")));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatusCode());
            assertFalse(error.toString().contains("PRIVATE")); assertFalse(error.toString().contains("fixture-secret"));
            assertNull(error.getCause());
        } finally { server.stop(0); }
    }

    @Test void mapsProviderErrorsWithoutExposingBodyOrKey() throws Exception {
        for (int code : new int[] {401, 403, 429, 500}) {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                byte[] response = "PRIVATE INVOICE fixture-secret".getBytes();
                exchange.sendResponseHeaders(code, response.length);
                try (var output = exchange.getResponseBody()) { output.write(response); }
            });
            server.start();
            try {
                var error = assertThrows(ResponseStatusException.class, () -> fixture(server).extract(image("jpeg")));
                assertEquals(code == 429 ? HttpStatus.TOO_MANY_REQUESTS : code == 500 ? HttpStatus.BAD_GATEWAY : HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
                assertFalse(error.toString().contains("PRIVATE")); assertFalse(error.toString().contains("fixture-secret"));
                assertNull(error.getCause());
            } finally { server.stop(0); }
        }
    }

    @Test void configuredTimeoutCancelsSlowOrStalledProviderAndKeepsErrorsSafe() throws Exception {
        for (boolean sendHeaders : new boolean[]{false, true}) {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var release = new java.util.concurrent.CountDownLatch(1);
            server.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                try {
                    if (sendHeaders) exchange.sendResponseHeaders(200, 100);
                    release.await(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            });
            server.start();
            try {
                var timed = new PurchaseInvoiceAiService("gemini", "fixture-secret", "gemini-3.1-flash-lite",
                    HttpClient.newHttpClient(), URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"), java.time.Duration.ofSeconds(1));
                var error = assertThrows(ResponseStatusException.class, () -> timed.extract(image("png")));
                assertEquals(HttpStatus.GATEWAY_TIMEOUT, error.getStatusCode());
                assertTrue(error.getReason().contains("within 1 seconds"));
                assertTrue(error.getReason().contains("purchase order was not changed"));
                assertFalse(error.toString().contains("fixture-secret"));
                assertNull(error.getCause());
            } finally { release.countDown(); server.stop(0); }
        }
    }

    private PurchaseInvoiceAiService fixture(HttpServer server) {
        return fixture(server, "gemini", "gemini-3.1-flash-lite");
    }

    private PurchaseInvoiceAiService fixture(HttpServer server, String provider, String model) {
        return new PurchaseInvoiceAiService(provider, "fixture-secret", model, HttpClient.newHttpClient(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"));
    }
}
