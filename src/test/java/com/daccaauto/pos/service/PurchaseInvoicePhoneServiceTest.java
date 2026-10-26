package com.daccaauto.pos.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseInvoicePhoneServiceTest {
    private final MutableClock clock = new MutableClock();
    private final PurchaseInvoicePhoneService service = new PurchaseInvoicePhoneService(
            new PurchaseInvoiceAiService("demo", "", "unused"), clock);

    @Test
    void ownerOnlyAndReplacement() {
        var first = service.connect("desktop");
        assertNull(service.get(first.token(), "desktop"));
        status(HttpStatus.NOT_FOUND, () -> service.get(first.token(), "phone"));
        status(HttpStatus.NOT_FOUND, () -> service.get(first.token(), null));
        status(HttpStatus.NOT_FOUND, () -> service.delete(first.token(), "phone"));
        var replacement = service.connect("desktop");
        status(HttpStatus.NOT_FOUND, () -> service.get(first.token(), "desktop"));
        assertNull(service.get(replacement.token(), "desktop"));
    }

    @Test
    void expiresAtExactlyTenMinutesAndReclaimsCapacity() {
        var connection = service.connect("desktop");
        assertEquals(clock.instant().plusSeconds(600), connection.expiresAt());
        clock.now = clock.now.plusSeconds(600);
        status(HttpStatus.NOT_FOUND, () -> service.get(connection.token(), "desktop"));
        status(HttpStatus.NOT_FOUND, () -> service.upload(connection.token(), image()));
        for (int i = 0; i < 20; i++) service.connect("owner" + i);
        status(HttpStatus.TOO_MANY_REQUESTS, () -> service.connect("extra"));
        clock.now = clock.now.plusSeconds(600);
        assertNotNull(service.connect("extra"));
    }

    @Test
    void uploadIsOneShotAndReadsRetainImmutableBytesUntilDelete() throws Exception {
        var connection = service.connect("desktop");
        var file = image();
        service.upload(connection.token(), file);
        byte[] expected = file.getBytes();
        var photo = service.get(connection.token(), "desktop");
        assertEquals("image/png", photo.contentType());
        assertArrayEquals(expected, photo.bytes());
        byte[] exposed = photo.bytes();
        exposed[0] = 0;
        assertArrayEquals(expected, photo.bytes());
        assertArrayEquals(expected, service.get(connection.token(), "desktop").bytes());
        status(HttpStatus.CONFLICT, () -> service.upload(connection.token(), file));
        service.delete(connection.token(), "desktop");
        status(HttpStatus.NOT_FOUND, () -> service.get(connection.token(), "desktop"));
    }

    @Test
    void invalidAndOversizeUploadsDoNotConsumePair() {
        var connection = service.connect("desktop");
        status(HttpStatus.BAD_REQUEST, () -> service.upload(connection.token(),
                new MockMultipartFile("file", "bad.jpg", "image/jpeg", new byte[]{1, 2, 3})));
        status(HttpStatus.PAYLOAD_TOO_LARGE, () -> service.upload(connection.token(),
                new MockMultipartFile("file", "large.jpg", "image/jpeg", new byte[3 * 1024 * 1024 + 1])));
        assertNull(service.get(connection.token(), "desktop"));
        service.upload(connection.token(), image());
    }

    private static MockMultipartFile image() {
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XkAAAAASUVORK5CYII=");
        return new MockMultipartFile("file", "invoice.png", "image/png", png);
    }

    private static void status(HttpStatus expected, Runnable operation) {
        assertEquals(expected, assertThrows(ResponseStatusException.class, operation::run).getStatusCode());
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-24T00:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
