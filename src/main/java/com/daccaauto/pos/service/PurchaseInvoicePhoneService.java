package com.daccaauto.pos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PurchaseInvoicePhoneService {
    private static final int MAX_BYTES = 3 * 1024 * 1024;
    private static final int MAX_CONNECTIONS = 20;
    private static final Duration LIFETIME = Duration.ofMinutes(10);
    private final PurchaseInvoiceAiService imageValidator;
    private final Clock clock;
    private final Map<String, Pair> pairs = new HashMap<>();

    @Autowired
    public PurchaseInvoicePhoneService(PurchaseInvoiceAiService imageValidator) {
        this(imageValidator, Clock.systemUTC());
    }

    PurchaseInvoicePhoneService(PurchaseInvoiceAiService imageValidator, Clock clock) {
        this.imageValidator = imageValidator;
        this.clock = clock;
    }

    public synchronized Connection connect(String owner) {
        requireOwner(owner);
        purgeExpired();
        pairs.values().removeIf(pair -> pair.owner.equals(owner));
        if (pairs.size() >= MAX_CONNECTIONS) {
            throw failure(HttpStatus.TOO_MANY_REQUESTS, "Phone connections are full. Try again later.");
        }
        String token = UUID.randomUUID().toString();
        Instant expiresAt = clock.instant().plus(LIFETIME);
        pairs.put(token, new Pair(owner, expiresAt));
        return new Connection(token, expiresAt);
    }

    public synchronized Photo get(String token, String owner) {
        Pair pair = owned(token, owner);
        return pair.bytes == null ? null : new Photo(pair.bytes, pair.contentType);
    }

    public synchronized void delete(String token, String owner) {
        owned(token, owner);
        pairs.remove(token);
    }

    public synchronized void upload(String token, MultipartFile file) {
        Pair pair = find(token);
        if (pair.bytes != null) {
            throw failure(HttpStatus.CONFLICT, "A photo has already been uploaded. Request a new connection.");
        }
        if (file == null || file.isEmpty()) {
            throw failure(HttpStatus.BAD_REQUEST, "Choose a JPEG or PNG image.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw failure(HttpStatus.PAYLOAD_TOO_LARGE, "Image must be at most 3 MB.");
        }
        imageValidator.validateImage(file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw failure(HttpStatus.BAD_REQUEST, "Could not read image.");
        }
        if (bytes.length > MAX_BYTES) {
            throw failure(HttpStatus.PAYLOAD_TOO_LARGE, "Image must be at most 3 MB.");
        }
        // Validation can take time; never revive a pair that expired during decoding.
        find(token);
        pair.bytes = bytes.clone();
        pair.contentType = bytes.length >= 8 && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50 ? "image/png" : "image/jpeg";
    }

    private Pair owned(String token, String owner) {
        requireOwner(owner);
        Pair pair = find(token);
        if (!pair.owner.equals(owner)) {
            throw failure(HttpStatus.NOT_FOUND, "Connection not found or expired.");
        }
        return pair;
    }

    private Pair find(String token) {
        purgeExpired();
        Pair pair = pairs.get(token);
        if (pair == null) {
            throw failure(HttpStatus.NOT_FOUND, "Connection not found or expired.");
        }
        return pair;
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        pairs.values().removeIf(pair -> !now.isBefore(pair.expiresAt));
    }

    private static void requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw failure(HttpStatus.NOT_FOUND, "Connection not found or expired.");
        }
    }

    private static ResponseStatusException failure(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    public record Connection(String token, Instant expiresAt) { }

    public record Photo(byte[] bytes, String contentType) {
        public Photo {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }

        @Override
        public String toString() {
            return "Photo[redacted]";
        }
    }

    private static final class Pair {
        private final String owner;
        private final Instant expiresAt;
        private byte[] bytes;
        private String contentType;

        private Pair(String owner, Instant expiresAt) {
            this.owner = owner;
            this.expiresAt = expiresAt;
        }
    }
}
