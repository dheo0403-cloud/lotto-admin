package com.example.lotto.service;

import com.example.lotto.client.LottoClient;
import com.example.lotto.dto.DhLottoResponse;
import com.example.lotto.entity.BatchLog;
import com.example.lotto.entity.LottoDraw;
import com.example.lotto.repository.BatchLogRepository;
import com.example.lotto.repository.LottoDrawJdbcRepository;
import com.example.lotto.repository.LottoDrawRepository;
import com.example.lotto.repository.LottoRecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class LottoSyncServiceTest {

    private final LottoClient client = mock(LottoClient.class);
    private final LottoDrawRepository drawRepo = mock(LottoDrawRepository.class);
    private final BatchLogRepository logRepo = mock(BatchLogRepository.class);
    private final LottoDrawJdbcRepository jdbcRepo = mock(LottoDrawJdbcRepository.class);
    private final LottoRecommendationRepository recRepo = mock(LottoRecommendationRepository.class);
    private final LottoRecommendationService recService = mock(LottoRecommendationService.class);
    private final LottoSyncService service =
            new LottoSyncService(client, drawRepo, logRepo, jdbcRepo, recRepo, recService);

    @BeforeEach
    void setUp() {
        // DB 최신 회차 = 1244
        LottoDraw latest = new LottoDraw();
        latest.setId(1244);
        when(drawRepo.findTopByOrderByIdDesc()).thenReturn(Optional.of(latest));
    }

    private DhLottoResponse draw(int no) {
        DhLottoResponse.DrawInfo info = new DhLottoResponse.DrawInfo();
        info.setLtEpsd(no);
        info.setTm1WnNo(1); info.setTm2WnNo(2); info.setTm3WnNo(3);
        info.setTm4WnNo(4); info.setTm5WnNo(5); info.setTm6WnNo(6);
        info.setBnsWnNo(7);
        info.setLtRflYmd("20261010");
        DhLottoResponse.Data data = new DhLottoResponse.Data();
        data.setList(List.of(info));
        DhLottoResponse res = new DhLottoResponse();
        res.setData(data);
        return res;
    }

    private List<String> loggedStatuses() {
        ArgumentCaptor<BatchLog> captor = ArgumentCaptor.forClass(BatchLog.class);
        verify(logRepo, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream().map(BatchLog::getStatus).toList();
    }

    @Test
    void 새_회차_저장_후_다음_회차_추천_자동_생성() {
        when(client.fetchLottoDraw(1245)).thenReturn(draw(1245));
        when(client.fetchLottoDraw(1246)).thenReturn(draw(1245)); // 1246은 아직 없음 → 최신(1245) 반환
        when(recRepo.existsByTargetDraw(1246)).thenReturn(false);

        service.syncLatestDraws();

        verify(jdbcRepo).saveAllBatch(anyList());
        verify(recService).generateRecommendations(5);
        assertEquals(List.of("SUCCESS"), loggedStatuses());
    }

    @Test
    void 다음_회차_추천이_이미_있으면_생성_안_함() {
        when(client.fetchLottoDraw(1245)).thenReturn(draw(1245));
        when(client.fetchLottoDraw(1246)).thenReturn(draw(1245));
        when(recRepo.existsByTargetDraw(1246)).thenReturn(true);

        service.syncLatestDraws();

        verify(recService, never()).generateRecommendations(anyInt());
    }

    @Test
    void 새_회차가_아직_없으면_NO_NEW_로그만_남김() {
        when(client.fetchLottoDraw(1245)).thenReturn(draw(1244));

        service.syncLatestDraws();

        verify(jdbcRepo, never()).saveAllBatch(anyList());
        verify(recService, never()).generateRecommendations(anyInt());
        assertEquals(List.of("NO_NEW"), loggedStatuses());
    }

    @Test
    void API_오류면_FAIL_로그_남기고_추천_생성_안_함() {
        when(client.fetchLottoDraw(1245)).thenReturn(new DhLottoResponse()); // 클라이언트가 오류를 빈 응답으로 변환

        service.syncLatestDraws();

        verify(recService, never()).generateRecommendations(anyInt());
        assertEquals(List.of("FAIL"), loggedStatuses());
    }

    @Test
    void 중간에_오류나면_받은_회차는_저장하되_추천은_만들지_않음() {
        when(client.fetchLottoDraw(1245)).thenReturn(draw(1245));
        when(client.fetchLottoDraw(1246)).thenReturn(new DhLottoResponse());

        service.syncLatestDraws();

        verify(jdbcRepo).saveAllBatch(anyList());
        verify(recService, never()).generateRecommendations(anyInt());
        assertEquals(List.of("SUCCESS", "FAIL"), loggedStatuses());
    }
}
