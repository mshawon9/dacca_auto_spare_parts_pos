package com.daccaauto.pos.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SaleScannerService {
    private final Map<String, Pairing> pairings = new HashMap<>();
    private final Clock clock;

    public SaleScannerService() { this(Clock.systemUTC()); }

    SaleScannerService(Clock clock) { this.clock = clock; }

    public record Connection(String token, Instant expiresAt) { }
    public record Scan(long sequence, String code) { }

    private static class Pairing {
        final String owner;
        final Instant expiresAt;
        final ArrayDeque<Scan> scans = new ArrayDeque<>();
        long sequence;

        Pairing(String owner, Instant expiresAt) {
            this.owner = owner;
            this.expiresAt = expiresAt;
        }
    }

    public synchronized Connection create(String owner) {
        pairings.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(clock.instant())
            || entry.getValue().owner.equals(owner));
        if (pairings.size() >= 1000) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please try again later.");
        }
        String token = UUID.randomUUID().toString();
        Pairing pairing = new Pairing(owner, clock.instant().plusSeconds(600));
        pairings.put(token, pairing);
        return new Connection(token, pairing.expiresAt);
    }

    private Pairing require(String token) {
        Pairing pairing = pairings.get(token);
        if (pairing == null || !pairing.expiresAt.isAfter(clock.instant())) {
            pairings.remove(token);
            throw new ResponseStatusException(HttpStatus.GONE, "Scanner link expired. Connect the phone again.");
        }
        return pairing;
    }

    public synchronized void submit(String token, String code) {
        Pairing pairing = require(token);
        if (code == null || code.isBlank() || code.length() > 200 || code.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scan a valid product barcode or QR code (up to 200 characters).");
        }
        if (pairing.scans.size() >= 10) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Wait for the sale screen to receive the scans.");
        }
        pairing.scans.add(new Scan(++pairing.sequence, code.strip()));
    }

    public synchronized Scan poll(String token, String owner, long acknowledged) {
        Pairing pairing = require(token);
        checkOwner(pairing, owner);
        while (!pairing.scans.isEmpty() && pairing.scans.peek().sequence() <= acknowledged) {
            pairing.scans.remove();
        }
        return pairing.scans.peek();
    }

    public synchronized void close(String token, String owner) {
        Pairing pairing = pairings.get(token);
        if (pairing != null) {
            checkOwner(pairing, owner);
            pairings.remove(token);
        }
    }

    private void checkOwner(Pairing pairing, String owner) {
        if (!pairing.owner.equals(owner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
