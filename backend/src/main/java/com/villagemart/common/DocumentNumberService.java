package com.villagemart.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates gap-tolerant, unique human readable document numbers such as {@code INV-000123}.
 * The sequence row is locked for the remainder of the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    public static final String SALE = "SALE";
    public static final String PURCHASE = "PURCHASE";
    public static final String SALES_RETURN = "SALES_RETURN";
    public static final String PURCHASE_RETURN = "PURCHASE_RETURN";
    public static final String PAYMENT = "PAYMENT";

    private final DocumentSequenceRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(String sequenceName) {
        DocumentSequence seq = repository.lockByName(sequenceName)
                .orElseThrow(() -> new IllegalStateException("Missing document sequence " + sequenceName));
        long value = seq.getNextValue();
        seq.setNextValue(value + 1);
        return "%s-%06d".formatted(seq.getPrefix(), value);
    }
}
