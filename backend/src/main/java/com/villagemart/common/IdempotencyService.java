package com.villagemart.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Stores idempotency keys so that client retries never create duplicate business transactions.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{8,80}$");

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public void validateKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw ApiException.badRequest("INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key header must be 8-80 characters of letters, digits, '-' or '_'");
        }
    }

    public String hash(Object request) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash request", e);
        }
    }

    /**
     * Returns the resource id of an earlier completed request with the same key, verifying the payload matches.
     */
    @Transactional(readOnly = true)
    public Optional<Long> findExisting(String operation, String key, String requestHash) {
        return repository.findByOperationAndIdemKey(operation, key).map(rec -> {
            if (!rec.getRequestHash().equals(requestHash)) {
                throw ApiException.conflict("IDEMPOTENCY_CONFLICT",
                        "Idempotency-Key was already used with a different request");
            }
            return rec.getResourceId();
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String operation, String key, String requestHash, Long resourceId) {
        IdempotencyRecord rec = new IdempotencyRecord();
        rec.setOperation(operation);
        rec.setIdemKey(key);
        rec.setRequestHash(requestHash);
        rec.setResourceId(resourceId);
        rec.setCreatedAt(Instant.now());
        repository.saveAndFlush(rec);
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
