package pt.xavier.tms.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class CodeGeneratorTests {

    @Test
    void generatesSequentialCodeWithNormalizedPrefixAndPadding() {
        assertThat(CodeGenerator.generateSequentialCode(" act ", 2026, 42, 6))
                .isEqualTo("ACT-2026-000042");
    }

    @Test
    void rejectsInvalidSequence() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CodeGenerator.generateSequentialCode("ACT", 2026, 0, 6));
    }
}
