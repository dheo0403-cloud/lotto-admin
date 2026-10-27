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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 통계 기반 로또 번호 추천 서비스
 *
 * 회차당 한 번만 생성한다(이미 추천이 있는 회차는 거부). 아래 전략을 순서대로 돌아가며 1세트씩 배정:
 * 1. 기댓값 극대화 (독식형) - 순수 난수 + 인기 패턴 회피 (홀짝/합계 무시)
 * 2. 이월수 포함 (트렌드형) - 직전 회차 번호 중 1개 고정 + 복합 가중치
 * 3. 통계 밸런스 (안정형)   - 구간별 1개씩 + 복합 가중치 (전체 70% + 최근 50회 30%)
 * 4. 장기 미출현 (회귀형)   - 마지막 출현 후 지난 회차 수만큼 가중
 * 5. 궁합수 (동반형)        - 먼저 뽑힌 번호와 함께 나온 횟수만큼 가중
 *
 * 공통 조건: 인기 패턴 회피(AC값 포함), 역대 1등 조합 제외, 다른 세트와 공유 번호 2개 이하.
 * 추첨 확률은 어떤 조합이든 같다. 인기 패턴 회피는 1등 당첨 시 당첨금을 나눌 사람 수를 줄이려는 목적이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LottoRecommendationService {

    private final LottoRecommendationRepository recommendationRepository;
    private final LottoDrawRepository lottoDrawRepository;

    // 자동 생성 세트 수 = 전략 수 (전략마다 1세트)
    public static final int AUTO_COUNT = 5;

    private static final int TOTAL_NUMBERS = 45;
    private static final int PICK_COUNT = 6;
    private static final int RECENT_N = 50;
    private static final int SUM_MIN = 100;
    private static final int SUM_MAX = 175;
    private static final int MIN_AC = 7;        // AC값: 번호 간 차이값 종류 수 - 5 (0~10)
    private static final int MAX_SHARED = 2;    // 다른 세트와 겹칠 수 있는 번호 수
    private static final int ATTEMPTS = 2000;
    private static final Random random = new Random();

    // 5개 구간 정의 (대시보드 통계 구간과 동일)
    private static final int[][] ZONES = {{1, 10}, {11, 20}, {21, 30}, {31, 40}, {41, 45}};

    // 전략 이름, 홀짝·합계·연속 조건 적용 여부, 후보 조합 1개를 만드는 함수
    private record Strategy(String name, boolean balanced, Supplier<List<Integer>> next) {}

    @Transactional
    public List<LottoRecommendation> generateRecommendations(int count) {
        int latestDraw = lottoDrawRepository.findTopByOrderByIdDesc()
                .map(LottoDraw::getId)
                .orElse(0);
        int targetDraw = latestDraw + 1;
        if (recommendationRepository.existsByTargetDraw(targetDraw)) {
            throw new IllegalStateException(targetDraw + "회 추천 번호가 이미 있습니다.");
        }

        List<LottoDraw> allDraws = lottoDrawRepository.findAll();
        List<Strategy> strategies;
        if (allDraws.isEmpty()) {
            log.warn("No draw data. Using pure random only.");
            strategies = List.of(new Strategy("순수 랜덤", false, this::strategyPureRandom));
        } else {
            strategies = buildStrategies(allDraws);
        }

        // 역대 1등 조합은 다시 추천하지 않음
        Set<List<Integer>> pastWins = allDraws.stream().map(this::numbersOf).collect(Collectors.toSet());

        List<List<Integer>> picked = new ArrayList<>();
        List<LottoRecommendation> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Strategy s = strategies.get(i % strategies.size());
            List<Integer> numbers = pick(s, pastWins, picked);
            picked.add(numbers);
            results.add(saveRecommendation(targetDraw, numbers, s.name()));
        }
        return results;
    }

    private List<Strategy> buildStrategies(List<LottoDraw> allDraws) {
        // === 통계 데이터 준비 ===
        Map<Integer, Integer> fullFrequency = calcFrequency(allDraws);
        List<LottoDraw> recentDraws = allDraws.stream()
                .sorted(Comparator.comparingInt(LottoDraw::getId).reversed())
                .limit(RECENT_N)
                .toList();
        Map<Integer, Integer> recentFrequency = calcFrequency(recentDraws);

        // 복합 가중치: 전체 빈도 70% + 최근 50회 빈도 30%
        Map<Integer, Double> compositeWeight = new LinkedHashMap<>();
        for (int n = 1; n <= TOTAL_NUMBERS; n++) {
            double fullW = fullFrequency.getOrDefault(n, 0);
            double recentW = recentFrequency.getOrDefault(n, 0);
            compositeWeight.put(n, fullW * 0.7 + recentW * 0.3 * ((double) allDraws.size() / RECENT_N));
        }

        LottoDraw lastDraw = recentDraws.get(0);
        List<Integer> lastDrawNumbers = numbersOf(lastDraw);

        // 미출현 가중치: 마지막 출현 후 지난 회차 수 + 1 (한 번도 안 나왔으면 전체 회차 수 + 1)
        int[] lastSeen = new int[TOTAL_NUMBERS + 1];
        for (LottoDraw d : allDraws) {
            for (int n : numbersOf(d)) lastSeen[n] = Math.max(lastSeen[n], d.getId());
        }
        Map<Integer, Double> overdueWeight = new LinkedHashMap<>();
        for (int n = 1; n <= TOTAL_NUMBERS; n++) {
            overdueWeight.put(n, (double) (lastDraw.getId() - lastSeen[n] + 1));
        }

        // 궁합수: 두 번호가 같은 회차에 함께 나온 횟수
        int[][] pairCount = new int[TOTAL_NUMBERS + 1][TOTAL_NUMBERS + 1];
        for (LottoDraw d : allDraws) {
            List<Integer> nums = numbersOf(d);
            for (int a : nums) for (int b : nums) if (a != b) pairCount[a][b]++;
        }

        return List.of(
                new Strategy("기댓값 극대화 (독식형)", false, this::strategyPureRandom),
                new Strategy("이월수 포함 (트렌드형)", true,
                        () -> weightedPick(compositeWeight, List.of(lastDrawNumbers.get(random.nextInt(PICK_COUNT))))),
                new Strategy("통계 밸런스 (안정형)", true, () -> strategyComprehensive(compositeWeight)),
                new Strategy("장기 미출현 (회귀형)", true, () -> weightedPick(overdueWeight, List.of())),
                new Strategy("궁합수 (동반형)", true, () -> strategyPair(compositeWeight, pairCount))
        );
    }

    // 전략 후보를 조건을 만족할 때까지 다시 뽑는다. 끝내 못 찾으면 조건을 풀지 않고 실패로 끝낸다.
    private List<Integer> pick(Strategy s, Set<List<Integer>> pastWins, List<List<Integer>> picked) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            List<Integer> numbers = s.next().get();
            if (s.balanced() && !isValidComprehensive(numbers)) continue;
            if (isAntiPattern(numbers) || pastWins.contains(numbers) || overlapsTooMuch(numbers, picked)) continue;
            return numbers;
        }
        throw new IllegalStateException(s.name() + " 조건을 만족하는 조합을 찾지 못했습니다.");
    }

    // ===================================================================
    //  전략별 후보 생성 (정렬된 6개 번호 1세트)
    // ===================================================================

    // 구간마다 1개씩 가중치로 뽑고, 나머지 1개는 전체에서 가중치로 뽑는다
    private List<Integer> strategyComprehensive(Map<Integer, Double> compositeWeight) {
        List<Integer> numbers = new ArrayList<>();
        for (int[] zone : ZONES) {
            Map<Integer, Double> zoneWeights = new LinkedHashMap<>();
            for (int n = zone[0]; n <= zone[1]; n++) zoneWeights.put(n, compositeWeight.get(n));
            numbers.add(weightedRandomPickOne(zoneWeights));
        }
        return weightedPick(compositeWeight, numbers);
    }

    // 첫 번호는 복합 가중치, 이후 번호는 (1 + 이미 뽑은 번호들과 함께 나온 횟수 합) 가중치
    private List<Integer> strategyPair(Map<Integer, Double> compositeWeight, int[][] pairCount) {
        List<Integer> numbers = new ArrayList<>(List.of(weightedRandomPickOne(compositeWeight)));
        while (numbers.size() < PICK_COUNT) {
            Map<Integer, Double> weights = new LinkedHashMap<>();
            for (int n = 1; n <= TOTAL_NUMBERS; n++) {
                if (numbers.contains(n)) continue;
                double w = 1;
                for (int p : numbers) w += pairCount[p][n];
                weights.put(n, w);
            }
            numbers.add(weightedRandomPickOne(weights));
        }
        Collections.sort(numbers);
        return numbers;
    }

    private List<Integer> strategyPureRandom() {
        List<Integer> pool = new ArrayList<>(IntStream.rangeClosed(1, TOTAL_NUMBERS).boxed().toList());
        Collections.shuffle(pool);
        List<Integer> selected = new ArrayList<>(pool.subList(0, PICK_COUNT));
        Collections.sort(selected);
        return selected;
    }

    // fixed를 포함하고 나머지는 가중치로 채운다
    private List<Integer> weightedPick(Map<Integer, Double> weights, List<Integer> fixed) {
        List<Integer> numbers = new ArrayList<>(fixed);
        Map<Integer, Double> remaining = new LinkedHashMap<>(weights);
        numbers.forEach(remaining::remove);
        while (numbers.size() < PICK_COUNT) {
            int picked = weightedRandomPickOne(remaining);
            numbers.add(picked);
            remaining.remove(picked);
        }
        Collections.sort(numbers);
        return numbers;
    }

    // ===================================================================
    //  필터
    // ===================================================================

    /**
     * 사람들이 많이 고르는 패턴 (1등 당첨 시 당첨금을 나눌 사람이 많아짐)
     */
    boolean isAntiPattern(List<Integer> numbers) {
        // 1. 생일 편향: 1~31이 5개 이상
        if (numbers.stream().filter(n -> n <= 31).count() >= 5) return true;

        // 2. 5의 배수가 5개 이상
        if (numbers.stream().filter(n -> n % 5 == 0).count() >= 5) return true;

        // 3. 끝자리가 같은 번호가 4개 이상
        Map<Integer, Long> byLastDigit = numbers.stream().collect(Collectors.groupingBy(n -> n % 10, Collectors.counting()));
        if (Collections.max(byLastDigit.values()) >= 4) return true;

        // 4. 등차수열 (간격이 모두 같음)
        int gap = numbers.get(1) - numbers.get(0);
        boolean arithmetic = true;
        for (int i = 2; i < numbers.size(); i++) {
            if (numbers.get(i) - numbers.get(i - 1) != gap) { arithmetic = false; break; }
        }
        if (arithmetic) return true;

        // 5. 용지(가로 7칸) 한 줄: 같은 가로줄 또는 같은 세로줄
        if (numbers.stream().map(n -> (n - 1) / 7).distinct().count() == 1) return true;
        if (numbers.stream().map(n -> (n - 1) % 7).distinct().count() == 1) return true;

        // 6. AC값이 낮은 규칙적인 조합
        return acValue(numbers) < MIN_AC;
    }

    // AC값 = 모든 번호 쌍 차이값의 종류 수 - (번호 수 - 1)
    int acValue(List<Integer> numbers) {
        Set<Integer> diffs = new HashSet<>();
        for (int i = 0; i < numbers.size(); i++) {
            for (int j = i + 1; j < numbers.size(); j++) diffs.add(numbers.get(j) - numbers.get(i));
        }
        return diffs.size() - (numbers.size() - 1);
    }

    /**
     * 통계 밸런스 검증: 홀짝 비율 + 합계 범위 + 연속 번호 제한
     */
    private boolean isValidComprehensive(List<Integer> numbers) {
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

    // 이미 뽑은 세트 중 하나라도 MAX_SHARED개 넘게 겹치면 true (같은 조합 중복도 여기서 걸림)
    private boolean overlapsTooMuch(List<Integer> numbers, List<List<Integer>> picked) {
        for (List<Integer> p : picked) {
            if (numbers.stream().filter(p::contains).count() > MAX_SHARED) return true;
        }
        return false;
    }

    // ===================================================================
    //  공통 유틸리티
    // ===================================================================

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
        return weights.keySet().stream().reduce((a, b) -> b).orElseThrow();
    }

    private List<Integer> numbersOf(LottoDraw d) {
        List<Integer> nums = new ArrayList<>(List.of(d.getNum1(), d.getNum2(), d.getNum3(),
                d.getNum4(), d.getNum5(), d.getNum6()));
        Collections.sort(nums);
        return nums;
    }

    private Map<Integer, Integer> calcFrequency(List<LottoDraw> draws) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 1; i <= TOTAL_NUMBERS; i++) freq.put(i, 0);
        for (LottoDraw d : draws) {
            for (int n : numbersOf(d)) freq.merge(n, 1, Integer::sum);
        }
        return freq;
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
}
