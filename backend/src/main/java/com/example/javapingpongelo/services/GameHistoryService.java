package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.GameResult;
import com.example.javapingpongelo.models.PlayerGameHistory;
import com.example.javapingpongelo.repositories.GameHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameHistoryService {

    @Autowired
    GameHistoryRepository gameHistoryRepository;

    public List<GameResult> getGameHistory(UUID playerId) {
        return convertToGameResults(gameHistoryRepository.findByPlayerId(playerId));
    }

    /**
     * Convert PlayerGameHistory entries to GameResult objects for backward compatibility
     */
    private List<GameResult> convertToGameResults(List<PlayerGameHistory> historyEntries) {
        if (historyEntries == null) return new ArrayList<>();

        return historyEntries.stream()
                             .map(entry -> GameResult.builder()
                                                     .date(entry.getGameDate())
                                                     .isWin(entry.isWin())
                                                     .gameType(entry.getGameType())
                                                     .gameIdentifier("GAME_RESULT")
                                                     .build())
                             .collect(Collectors.toList());
    }

    public Page<PlayerGameHistory> getGameHistoryPaged(UUID playerId, Pageable pageable) {
        return gameHistoryRepository.findByPlayerId(playerId, pageable);
    }
}