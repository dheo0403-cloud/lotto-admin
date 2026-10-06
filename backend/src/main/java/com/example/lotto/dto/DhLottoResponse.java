package com.example.lotto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DhLottoResponse {

    private String resultCode;
    private String resultMessage;
    private Data data;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private List<DrawInfo> list;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DrawInfo {
        private Integer ltEpsd;       // 회차
        private Integer tm1WnNo;      // 번호1
        private Integer tm2WnNo;      // 번호2
        private Integer tm3WnNo;      // 번호3
        private Integer tm4WnNo;      // 번호4
        private Integer tm5WnNo;      // 번호5
        private Integer tm6WnNo;      // 번호6
        private Integer bnsWnNo;      // 보너스 번호
        private String ltRflYmd;      // 추첨일 (yyyyMMdd)
        private Integer rnk1WnNope;   // 1등 당첨자 수
        private Long rnk1WnAmt;       // 1등 당첨금 (1인당)
        private Long rnk1SumWnAmt;    // 1등 총 당첨금
    }

    /**
     * API 응답이 유효한지 확인
     */
    public boolean isSuccess() {
        return data != null && data.getList() != null && !data.getList().isEmpty();
    }

    /**
     * 첫 번째 DrawInfo 반환
     */
    public DrawInfo getFirstDraw() {
        if (isSuccess()) {
            return data.getList().get(0);
        }
        return null;
    }
}
