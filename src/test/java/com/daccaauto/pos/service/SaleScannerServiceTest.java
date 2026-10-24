package com.daccaauto.pos.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class SaleScannerServiceTest {
    private final SaleScannerService service = new SaleScannerService();

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-24T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }

    @Test
    void expiresAfterTenMinutes() {
        MutableClock clock = new MutableClock();
        Instant now = clock.instant();
        var timed = new SaleScannerService(clock);
        var connection = timed.create("cashier");
        assertEquals(now.plusSeconds(600), connection.expiresAt());
        clock.now = now.plusSeconds(600);
        assertEquals(HttpStatus.GONE, assertThrows(ResponseStatusException.class,
            () -> timed.submit(connection.token(), "123")).getStatusCode());
    }

    @Test
    void deliversScansInOrderAndRetriesUntilAcknowledged() {
        var connection = service.create("cashier");
        service.submit(connection.token(), " 0012345 ");
        service.submit(connection.token(), "9876543");
        var first = service.poll(connection.token(), "cashier", 0);
        assertEquals("0012345", first.code());
        assertEquals(first, service.poll(connection.token(), "cashier", 0));
        var second = service.poll(connection.token(), "cashier", first.sequence());
        assertEquals("9876543", second.code());
        assertNull(service.poll(connection.token(), "cashier", second.sequence()));
    }

    @Test
    void onlyTheOriginatingSaleSessionCanReadOrDisconnect() {
        var connection = service.create("cashier");
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            () -> service.poll(connection.token(), "other", 0)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
            () -> service.close(connection.token(), "other")).getStatusCode());
    }

    @Test
    void reconnectingAndClosingInvalidateOldPhoneLinks() {
        var old = service.create("cashier");
        var current = service.create("cashier");
        assertEquals(HttpStatus.GONE, assertThrows(ResponseStatusException.class,
            () -> service.submit(old.token(), "123")).getStatusCode());
        service.close(current.token(), "cashier");
        assertEquals(HttpStatus.GONE, assertThrows(ResponseStatusException.class,
            () -> service.submit(current.token(), "123")).getStatusCode());
    }

    @Test
    void rejectsInvalidCodesAndBoundsPendingScans() {
        var connection = service.create("cashier");
        for (String code : new String[]{"", " ", "abc\n123", "a".repeat(201)}) {
            assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.submit(connection.token(), code)).getStatusCode());
        }
        for (int i = 0; i < 10; i++) service.submit(connection.token(), "123");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(ResponseStatusException.class,
            () -> service.submit(connection.token(), "123")).getStatusCode());
    }
}
