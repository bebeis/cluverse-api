package cluverse.common.util;

import cluverse.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringNormalizerTest {

    @Test
    void 이메일을_필수값으로_정규화한다() {
        // when
        String result = StringNormalizer.requireNormalizedEmail(" LUNA@SNU.AC.KR ");

        // then
        assertThat(result).isEqualTo("luna@snu.ac.kr");
    }

    @Test
    void 이메일_도메인을_소문자로_추출한다() {
        // when
        String result = StringNormalizer.extractEmailDomain(" LUNA@SNU.AC.KR ");

        // then
        assertThat(result).isEqualTo("snu.ac.kr");
    }

    @Test
    void 선택_도메인은_null과_공백을_null로_정규화한다() {
        // when, then
        assertThat(StringNormalizer.normalizeOptionalDomain(null)).isNull();
        assertThat(StringNormalizer.normalizeOptionalDomain("   ")).isNull();
        assertThat(StringNormalizer.normalizeOptionalDomain(" SNU.AC.KR ")).isEqualTo("snu.ac.kr");
    }

    @Test
    void 필수_이메일이_비어있으면_예외가_발생한다() {
        // when, then
        assertThatThrownBy(() -> StringNormalizer.requireNormalizedEmail(" "))
                .isInstanceOf(BadRequestException.class);
    }
}
