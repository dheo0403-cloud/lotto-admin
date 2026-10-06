package com.example.lotto.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LottoRecommendationTest {

    @ParameterizedTest
    @CsvSource({
            "6, false, 1등, true",
            "5, true, 2등, true",
            "5, false, 3등, true",
            "4, true, 4등, true",
            "3, false, 5등, true",
            "2, true, 꽝, false",
            "0, false, 꽝, false"
    })
    void 일치_개수와_보너스로_등수_계산(int match, boolean bonus, String label, boolean matched) {
        LottoRecommendation rec = new LottoRecommendation();
        rec.updateResult(match, bonus);
        assertEquals(label, rec.getResultLabel());
        assertEquals(matched, rec.getIsMatched());
    }
}
