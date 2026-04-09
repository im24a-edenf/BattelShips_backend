package com.example.springtemplate.schiffe.model;

import com.example.springtemplate.schiffe.model.enums.GamePhase;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class MultiplayerGame {

    private final String id;
    private final String roomCode;

    @Setter
    private GamePhase phase;

    private final UUID playerOneId;
    @Setter
    private UUID playerTwoId;

    private final String playerOneEmail;
    @Setter
    private String playerTwoEmail;

    private final Board playerOneBoard;
    private final Board playerTwoBoard;

    @Setter
    private boolean playerOneReady;
    @Setter
    private boolean playerTwoReady;

    @Setter
    private UUID currentTurn;

    private final List<Shot> playerOneShots;
    private final List<Shot> playerTwoShots;

    @Setter
    private UUID winnerId;
    @Setter
    private String winnerEmail;

    public MultiplayerGame(String roomCode, UUID playerOneId, String playerOneEmail) {
        this.id = UUID.randomUUID().toString();
        this.roomCode = roomCode;
        this.phase = GamePhase.WAITING_FOR_PLAYER;
        this.playerOneId = playerOneId;
        this.playerOneEmail = playerOneEmail;
        this.playerOneBoard = new Board();
        this.playerTwoBoard = new Board();
        this.playerOneReady = false;
        this.playerTwoReady = false;
        this.playerOneShots = new ArrayList<>();
        this.playerTwoShots = new ArrayList<>();
    }

    public boolean isBothJoined() {
        return playerTwoId != null;
    }

    public boolean isBothReady() {
        return playerOneReady && playerTwoReady;
    }

    public Board getBoardFor(UUID playerId) {
        return playerOneId.equals(playerId) ? playerOneBoard : playerTwoBoard;
    }

    public Board getOpponentBoard(UUID playerId) {
        return playerOneId.equals(playerId) ? playerTwoBoard : playerOneBoard;
    }

    public boolean isPlayerTurn(UUID playerId) {
        return currentTurn != null && currentTurn.equals(playerId);
    }

    public void switchTurn() {
        if (currentTurn == null) return;
        currentTurn = currentTurn.equals(playerOneId) ? playerTwoId : playerOneId;
    }

    public String getRoleFor(UUID playerId) {
        return playerOneId.equals(playerId) ? "PLAYER_ONE" : "PLAYER_TWO";
    }

    public List<Shot> getShotsFor(UUID playerId) {
        return playerOneId.equals(playerId) ? playerOneShots : playerTwoShots;
    }

    public String getOpponentEmail(UUID playerId) {
        return playerOneId.equals(playerId) ? playerTwoEmail : playerOneEmail;
    }

    public String getNextTurnEmail() {
        if (currentTurn == null) return null;
        return currentTurn.equals(playerOneId) ? playerOneEmail : playerTwoEmail;
    }

    public String getNextTurnRole() {
        if (currentTurn == null) return null;
        return currentTurn.equals(playerOneId) ? "PLAYER_ONE" : "PLAYER_TWO";
    }
}
