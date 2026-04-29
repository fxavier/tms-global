package pt.xavier.tms.shared.util;

import java.time.Year;
import java.util.Locale;

public final class CodeGenerator {

    private CodeGenerator() {
    }

    public static String generateActivityCode(long sequence) {
        return generateSequentialCode("ACT", Year.now().getValue(), sequence, 6);
    }

    public static String generateSequentialCode(String prefix, int year, long sequence, int padding) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        if (padding < 1) {
            throw new IllegalArgumentException("padding must be positive");
        }

        String normalizedPrefix = prefix.trim().toUpperCase(Locale.ROOT);
        return String.format(Locale.ROOT, "%s-%d-%0" + padding + "d", normalizedPrefix, year, sequence);
    }
}
