package com.example.lotto.service;

import com.example.lotto.entity.LottoDraw;
import com.example.lotto.entity.LottoRecommendation;
import com.example.lotto.repository.LottoDrawRepository;
import com.example.lotto.repository.LottoRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 통계 및 기댓값 기반 로또 번호 추천 서비스
 *
 * 5세트 고정 생성 구조:
 * 1. 기댓값 극대화 (독식형) - 1세트: 순수 난수 + 안티 패턴 필터 (생일 편향, 배수 등 배제)
 * 2. 이월수 포함 (트렌드형) - 1세트: 직전 회차 당첨 번호 중 1개 무작위 고정 + 가중치
 * 3. 통계 밸런스 (안정형)   - 2세트: 전체 70% + 최근 30% 빈도 가중치 (홀짝, 합계 적용)
 * 4. 핫콜드 믹스 (극단형)   - 1세트: 최근 50회 최다 4개 + 최소 2개
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LottoRecommendationService {

    private final LottoRecommendationRepository recommendationRepository;
    private final LottoDrawRepository lottoDrawRepository;

    private static final int TOTAL_NUMBERS = 45;
    private static final int PICK_COUNT = 6;
    private static final int RECENT_N = 50;
    private static final int SUM_MIN = 100;
    private static final int SUM_MAX = 175;
    private static final Random random = new Random();

    // 5개 구간 정의
    private static final int[][] ZONES = {{1, 9}, {10, 19}, {20, 29}, {30, 39}, {40, 45}};

    @Transactional
    public List<LottoRecommendation> generateRecommendations(int count) {
        int latestDraw = lottoDrawRepository.findTopByOrderByIdDesc()
                .map(LottoDraw::getId)
                .orElse(0);
        int targetDraw = latestDraw + 1;

        // 전체 당첨 데이터 로드
        List<LottoDraw> allDraws = lottoDrawRepository.findAll();
        if (allDraws.isEmpty()) {
            log.warn("No draw data. Falling back to pure random.");
            return generatePureRandomOnly(targetDraw, count);
        }

        // === 통계 데이터 준비 ===
        Map<Integer, Integer> fullFrequency = calcFrequency(allDraws);
        List<LottoDraw> recentDraws = allDraws.stream()
                .sorted(Comparator.comparingInt(LottoDraw::getId).reversed())
                .limit(RECENT_N)
                .toList();
        Map<Integer, Integer> recentFrequency = calcFrequency(recentDraws);

        // 복합 가중치: 전체 빈도 70% + 최근 50회 빈도 30%
        Map<Integer, Double> compositeWeight = new HashMap<>();
        for (int n = 1; n <= TOTAL_NUMBERS; n++) {
            double fullW = fullFrequency.getOrDefault(n, 0);
            double recentW = recentFrequency.getOrDefault(n, 0);
            compositeWeight.put(n, fullW * 0.7 + recentW * 0.3 * ((double) allDraws.size() / RECENT_N));
        }

        List<LottoRecommendation> results = new ArrayList<>();
        Set<String> usedSets = new HashSet<>(); // 중복 세트 방지

        // === 1. 기댓값 극대화 (독식형) - 1세트 ===
        if (count >= 1) {
            List<Integer> numbers = strategyMaxReturn();
            usedSets.add(numbers.toString());
            results.add(saveRecommendation(targetDraw, numbers, "기댓값 극대화 (독식형)"));
        }

        // === 2. 이월수 포함 (트렌드형) - 1세트 ===
        if (count >= 2) {
            LottoDraw lastDraw = allDraws.stream().max(Comparator.comparingInt(LottoDraw::getId)).orElse(null);
            List<Integer> numbers = strategyCarryOver(compositeWeight, lastDraw, usedSets);
            usedSets.add(numbers.toString());
            results.add(saveRecommendation(targetDraw, numbers, "이월수 포함 (트렌드형)"));
        }

        // === 3. 통계 밸런스 (안정형) - 2세트 ===
        int compSets = (count >= 4) ? 2 : ((count >= 3) ? 1 : 0);
        for (int i = 0; i < compSets; i++) {
            List<Integer> numbers = strategyComprehensive(compositeWeight, usedSets);
            usedSets.add(numbers.toString());
            results.add(saveRecommendation(targetDraw, numbers, "통계 밸런스 (안정형)"));
        }

        // === 4. 핫콜드 믹스 (극단형) - 1세트 (나머지 분량) ===
        while (results.size() < count) {
            List<Integer> numbers = strategyHotColdMix(recentFrequency, usedSets);
            usedSets.add(numbers.toString());
            results.add(saveRecommendation(targetDraw, numbers, "핫콜드 믹스 (극단형)"));
        }

        return results;
    }

    // ===================================================================
    //  1. 기댓값 극대화: 순수 랜덤 + 안티 패턴 엄격 적용
    // ===================================================================
    private List<Integer> strategyMaxReturn() {
        for (int attempt = 0; attempt < 1000; attempt++) {
            List<Integer> pool = new ArrayList<>(IntStream.rangeClosed(1, TOTAL_NUMBERS).boxed().toList());
            Collections.shuffle(pool);
            List<Integer> numbers = new ArrayList<>(pool.subList(0, PICK_COUNT));
            Collections.sort(numbers);

            // 기댓값을 극대화하기 위해 안티 패턴만 엄격하게 검증 (홀짝/합계 무시)
            if (!isAntiPattern(numbers)) {
                return numbers;
            }
        }
        return strategyPureRandom(); // fallback
    }

    // ===================================================================
    //  2. 이월수 포함: 직전 회차 중 1개 고정 + 나머지 가중치
    // ===================================================================
    private List<Integer> strategyCarryOver(Map<Integer, Double> compositeWeight, LottoDraw lastDraw, Set<String> usedSets) {
        if (lastDraw == null) return strategyComprehensive(compositeWeight, usedSets);

        List<Integer> lastDrawNumbers = Arrays.asList(
                lastDraw.getNum1(), lastDraw.getNum2(), lastDraw.getNum3(),
                lastDraw.getNum4(), lastDraw.getNum5(), lastDraw.getNum6()
        );

        for (int attempt = 0; attempt < 500; attempt++) {
            List<Integer> numbers = new ArrayList<>();
            // 직전 당첨 번호 중 무작위 1개 고정 추출 (편향 배제)
            int carryOverNum = lastDrawNumbers.get(random.nextInt(6));
            numbers.add(carryOverNum);

            // 나머지 5개 가중치 기반 추출
            Map<Integer, Double> remainingWeights = new LinkedHashMap<>();
            for (int n = 1; n <= TOTAL_NUMBERS; n++) {
                if (!numbers.contains(n)) {
                    remainingWeights.put(n, compositeWeight.getOrDefault(n, 1.0));
                }
            }

            while (numbers.size() < PICK_COUNT) {
                int picked = weightedRandomPickOne(remainingWeights);
                numbers.add(picked);
                remainingWeights.remove(picked);
            }

            Collections.sort(numbers);

            if (isValidComprehensive(numbers) && !isAntiPattern(numbers) && !usedSets.contains(numbers.toString())) {
                return numbers;
            }
        }
        return fallbackPick(compositeWeight);
    }

    // ===================================================================
    //  3. 통계 밸런스: 빈도+트렌드+구간+홀짝+합계 모든 조건을 동시에 만족
    // ===================================================================
    private List<Integer> strategyComprehensive(Map<Integer, Double> compositeWeight, Set<String> usedSets) {
        for (int attempt = 0; attempt < 500; attempt++) {
            List<Integer> numbers = new ArrayList<>();

            for (int[] zone : ZONES) {
                Map<Integer, Double> zoneWeights = new LinkedHashMap<>();
                for (int n = zone[0]; n <= zone[1]; n++) {
                    if (!numbers.contains(n)) {
                        zoneWeights.put(n, compositeWeight.getOrDefault(n, 1.0));
                    }
                }
                int picked = weightedRandomPickOne(zoneWeights);
                numbers.add(picked);
            }

            Map<Integer, Double> remainingWeights = new LinkedHashMap<>();
            for (int n = 1; n <= TOTAL_NUMBERS; n++) {
                if (!numbers.contains(n)) {
                    remainingWeights.put(n, compositeWeight.getOrDefault(n, 1.0));
                }
            }
            numbers.add(weightedRandomPickOne(remainingWeights));

            Collections.sort(numbers);

            if (isValidComprehensive(numbers) && !isAntiPattern(numbers) && !usedSets.contains(numbers.toString())) {
                return numbers;
            }
        }
        return fallbackPick(compositeWeight);
    }

    // ===================================================================
    //  4. 핫콜드 믹스: 최근 50회 Hot 4개 + Cold 2개
    // ===================================================================
    private List<Integer> strategyHotColdMix(Map<Integer, Integer> recentFreq, Set<String> usedSets) {
        List<Integer> sortedByRecent = IntStream.rangeClosed(1, TOTAL_NUMBERS)
                .boxed()
                .sorted(Comparator.comparingInt((Integer n) -> recentFreq.getOrDefault(n, 0)).reversed())
                .toList();

        List<Integer> hotPool = new ArrayList<>(sortedByRecent.subList(0, 15));
        List<Integer> coldPool = new ArrayList<>(sortedByRecent.subList(30, 45));

        for (int attempt = 0; attempt < 100; attempt++) {
            Set<Integer> picked = new LinkedHashSet<>();

            Collections.shuffle(hotPool);
            for (int h : hotPool) {
                if (picked.size() >= 4) break;
                picked.add(h);
            }

            Collections.shuffle(coldPool);
            for (int c : coldPool) {
                if (picked.size() >= 6) break;
                picked.add(c);
            }

            List<Integer> numbers = new ArrayList<>(picked);
            Collections.sort(numbers);

            if (numbers.size() == 6 && isValidComprehensive(numbers) && !isAntiPattern(numbers) && !usedSets.contains(numbers.toString())) {
                return numbers;
            }
        }
        return strategyPureRandom();
    }

    // ===================================================================
    //  안티 패턴 검증 (기댓값 극대화의 핵심)
    // ===================================================================
    private boolean isAntiPattern(List<Integer> numbers) {
        // 1. 생일 편향 배제 (1~31 범위의 숫자가 5개 이상이면 사람들이 너무 많이 찍는 번호)
        long birthdayBiasCount = numbers.stream().filter(n -> n <= 31).count();
        if (birthdayBiasCount >= 5) return true;

        // 2. 배수 패턴 배제 (모든 숫자가 5의 배수이거나 7의 배수 등 특정 수열인 경우)
        long count5 = numbers.stream().filter(n -> n % 5 == 0).count();
        if (count5 >= 5) return true; // 5개 이상이 5의 배수일 때

        // 추가적인 안티 패턴이 필요하다면 여기에 로직 추가
        return false;
    }

    /**
     * 통계 밸런스 검증: 홀짝 비율 + 합계 범위 + 연속 번호 제한
     */
    private boolean isValidComprehensive(List<Integer> numbers) {
        if (numbers.size() != PICK_COUNT) return false;

        // 홀짝 비율: 2:4 ~ 4:2
        long oddCount = numbers.stream().filter(n -> n % 2 == 1).count();
        if (oddCount < 2 || oddCount > 4) return false;

        // 합계: 100 ~ 175
        int sum = numbers.stream().mapToInt(Integer::intValue).sum();
        if (sum < SUM_MIN || sum > SUM_MAX) return false;

        // 연속 번호 3개 이상 방지
        int consecutive = 1;
        for (int i = 1; i < numbers.size(); i++) {
            if (numbers.get(i) == numbers.get(i - 1) + 1) {
                consecutive++;
                if (consecutive >= 3) return false;
            } else {
                consecutive = 1;
            }
        }

        return true;
    }

    // ===================================================================
    //  순수 랜덤 및 공통 유틸리티
    // ===================================================================
    private List<Integer> strategyPureRandom() {
        List<Integer> pool = new ArrayList<>(IntStream.rangeClosed(1, TOTAL_NUMBERS).boxed().toList());
        Collections.shuffle(pool);
        List<Integer> selected = new ArrayList<>(pool.subList(0, PICK_COUNT));
        Collections.sort(selected);
        return selected;
    }

    private int weightedRandomPickOne(Map<Integer, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double rand = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (Map.Entry<Integer, Double> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (rand < cumulative) {
                return entry.getKey();
            }
        }
        return weights.keySet().stream().reduce((a, b) -> b).orElse(1);
    }

    private Map<Integer, Integer> calcFrequency(List<LottoDraw> draws) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 1; i <= TOTAL_NUMBERS; i++) freq.put(i, 0);

        for (LottoDraw d : draws) {
            freq.merge(d.getNum1(), 1, Integer::sum);
            freq.merge(d.getNum2(), 1, Integer::sum);
            freq.merge(d.getNum3(), 1, Integer::sum);
            freq.merge(d.getNum4(), 1, Integer::sum);
            freq.merge(d.getNum5(), 1, Integer::sum);
            freq.merge(d.getNum6(), 1, Integer::sum);
        }
        return freq;
    }

    private List<Integer> fallbackPick(Map<Integer, Double> compositeWeight) {
        Set<Integer> picked = new LinkedHashSet<>();
        while (picked.size() < PICK_COUNT) {
            Map<Integer, Double> remaining = new LinkedHashMap<>();
            for (int n = 1; n <= TOTAL_NUMBERS; n++) {
                if (!picked.contains(n)) {
                    remaining.put(n, compositeWeight.getOrDefault(n, 1.0));
                }
            }
            picked.add(weightedRandomPickOne(remaining));
        }
        List<Integer> result = new ArrayList<>(picked);
        Collections.sort(result);
        return result;
    }

    private LottoRecommendation saveRecommendation(int targetDraw, List<Integer> numbers, String strategy) {
        LottoRecommendation rec = new LottoRecommendation();
        rec.setTargetDraw(targetDraw);
        rec.setNum1(numbers.get(0));
        rec.setNum2(numbers.get(1));
        rec.setNum3(numbers.get(2));
        rec.setNum4(numbers.get(3));
        rec.setNum5(numbers.get(4));
        rec.setNum6(numbers.get(5));
        rec.setStrategy(strategy);
        return recommendationRepository.save(rec);
    }

    private List<LottoRecommendation> generatePureRandomOnly(int targetDraw, int count) {
        List<LottoRecommendation> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(saveRecommendation(targetDraw, strategyPureRandom(), "순수 랜덤"));
        }
        return results;
    }
}
