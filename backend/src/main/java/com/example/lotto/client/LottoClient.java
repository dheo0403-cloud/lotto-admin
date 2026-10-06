package com.example.lotto.client;

import com.example.lotto.dto.DhLottoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
public class LottoClient {

    private static final String API_URL = "https://www.dhlottery.co.kr/lt645/selectPstLt645Info.do";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 특정 회차의 로또 추첨 결과를 조회합니다.
     * @param drawNo 조회할 회차 번호
     * @return DhLottoResponse (API 응답)
     */
    public DhLottoResponse fetchLottoDraw(int drawNo) {
        try {
            String url = API_URL + "?drwNo=" + drawNo;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                DhLottoResponse lottoResponse = objectMapper.readValue(response.body(), DhLottoResponse.class);
                log.debug("API response for draw {}: {}", drawNo, response.body());
                return lottoResponse;
            } else {
                log.warn("API returned status {} for draw {}", response.statusCode(), drawNo);
                return new DhLottoResponse();
            }
        } catch (Exception e) {
            log.error("Failed to fetch draw {} from API", drawNo, e);
            return new DhLottoResponse();
        }
    }

    /**
     * 최신 회차의 로또 추첨 결과를 조회합니다.
     * drwNo 파라미터 없이 호출하면 최신 회차가 반환됩니다.
     * @return DhLottoResponse (최신 회차)
     */
    public DhLottoResponse fetchLatestDraw() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                DhLottoResponse lottoResponse = objectMapper.readValue(response.body(), DhLottoResponse.class);
                log.info("Fetched latest draw from API");
                return lottoResponse;
            } else {
                log.warn("API returned status {} for latest draw", response.statusCode());
                return new DhLottoResponse();
            }
        } catch (Exception e) {
            log.error("Failed to fetch latest draw from API", e);
            return new DhLottoResponse();
        }
    }
}
