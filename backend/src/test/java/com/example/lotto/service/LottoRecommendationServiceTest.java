package com.example.lotto.service;

import com.example.lotto.entity.LottoDraw;
import com.example.lotto.entity.LottoRecommendation;
import com.example.lotto.repository.LottoDrawRepository;
import com.example.lotto.repository.LottoRecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LottoRecommendationServiceTest {

    private final LottoRecommendationRepository recRepo = mock(LottoRecommendationRepository.class);
    private final LottoDrawRepository drawRepo = mock(LottoDrawRepository.class);
    private final LottoRecommendationService service = new LottoRecommendationService(recRepo, drawRepo);

    @BeforeEach
    void setUp() {
        // 테스트 전용 회차 300개 (시드 고정)
        Random r = new Random(42);
        List<LottoDraw> draws = new ArrayList<>();
        for (int id = 1; id <= 300; id++) {
            List<Integer> pool = new ArrayList<>(IntStream.rangeClosed(1, 45).boxed().toList());
            Collections.shuffle(pool, r);
            List<Integer> n = new ArrayList<>(pool.subList(0, 6));
            Collections.sort(n);
            LottoDraw d = new LottoDraw();
            d.setId(id);
            d.setNum1(n.get(0)); d.setNum2(n.get(1)); d.setNum3(n.get(2));
            d.setNum4(n.get(3)); d.setNum5(n.get(4)); d.setNum6(n.get(5));
            draws.add(d);
        }
        when(drawRepo.findAll()).thenReturn(draws);
        when(drawRepo.findTopByOrderByIdDesc()).thenReturn(Optional.of(draws.get(299)));
        when(recRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private List<Integer> nums(LottoRecommendation r) {
        return List.of(r.getNum1(), r.getNum2(), r.getNum3(), r.getNum4(), r.getNum5(), r.getNum6());
    }

    @Test
    void 전략별_1세트씩_생성되고_세트간_공유번호_2개_이하() {
        // 무작위 생성이라 여러 번 돌려서 조건 위반·생성 실패가 없는지 확인
        for (int run = 0; run < 200; run++) {
            List<LottoRecommendation> recs = service.generateRecommendations(LottoRecommendationService.AUTO_COUNT);
            assertEquals(LottoRecommendationService.AUTO_COUNT, recs.size());
            assertEquals(LottoRecommendationService.AUTO_COUNT,
                    recs.stream().map(LottoRecommendation::getStrategy).distinct().count());
            for (int i = 0; i < recs.size(); i++) {
                List<Integer> a = nums(recs.get(i));
                assertEquals(301, recs.get(i).getTargetDraw());
                assertEquals(6, new HashSet<>(a).size());
                assertTrue(a.stream().allMatch(n -> n >= 1 && n <= 45));
                assertFalse(service.isAntiPattern(a), a.toString());
                for (int j = i + 1; j < recs.size(); j++) {
                    List<Integer> b = nums(recs.get(j));
                    assertTrue(a.stream().filter(b::contains).count() <= 2, a + " vs " + b);
                }
            }
        }
    }

    @Test
    void 열세트_요청하면_전략을_돌아가며_배정() {
        List<LottoRecommendation> recs = service.generateRecommendations(10);
        assertEquals(10, recs.size());
        assertEquals(recs.get(0).getStrategy(), recs.get(6).getStrategy());
        assertEquals(recs.get(3).getStrategy(), recs.get(9).getStrategy());
    }

    @Test
    void 이미_추천이_있는_회차는_거부() {
        when(recRepo.existsByTargetDraw(301)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.generateRecommendations(6));
        verify(recRepo, never()).save(any());
    }

    @Test
    void 인기_패턴_판정() {
        assertTrue(service.isAntiPattern(List.of(5, 10, 15, 20, 25, 30)));   // 등차·5의 배수
        assertTrue(service.isAntiPattern(List.of(1, 2, 3, 4, 5, 6)));        // 가로 한 줄
        assertTrue(service.isAntiPattern(List.of(3, 10, 17, 24, 31, 38)));   // 세로 한 줄
        assertTrue(service.isAntiPattern(List.of(1, 11, 21, 31, 33, 44)));   // 끝자리 1이 4개
        assertFalse(service.isAntiPattern(List.of(3, 11, 24, 32, 38, 43)));
        assertEquals(10, service.acValue(List.of(1, 2, 4, 8, 16, 32)));     // 차이값 15종류 - 5
    }
}
