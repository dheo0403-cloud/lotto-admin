package com.example.lotto.service;

import com.example.lotto.entity.LottoDraw;
import com.example.lotto.repository.LottoDrawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LottoDrawService {
    private final LottoDrawRepository lottoDrawRepository;

    public Page<LottoDraw> getDraws(Pageable pageable) {
        return lottoDrawRepository.findAll(pageable);
    }

    public Optional<LottoDraw> getDraw(int drawNo) {
        return lottoDrawRepository.findById(drawNo);
    }
}
