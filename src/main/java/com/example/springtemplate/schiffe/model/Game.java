package com.example.springtemplate.schiffe.model;

import com.example.springtemplate.schiffe.model.enums.Difficulty;
import com.example.springtemplate.schiffe.model.enums.GamePhase;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Game {

    private final String id;
    private final Difficulty difficulty;

    /** The UUID of the logged-in user who created this game */
    private final UUID ownerId;

    @Setter
    private GamePhase phase;

    private final Board playerBoard;
    private final Board botBoard;

    private final List<Shot> playerShots;
    private final List<Shot> botShots;

    private String winner;

    public Game(Difficulty difficulty, UUID ownerId) {
        this.id = UUID.randomUUID().toString();
        this.difficulty = difficulty;
        this.ownerId = ownerId;
        this.phase = GamePhase.PLACEMENT;
        this.playerBoard = new Board();
        this.botBoard = new Board();
        this.playerShots = new ArrayList<>();
        this.botShots = new ArrayList<>();
        this.winner = null;
    }

    public void setWinner(String winner) {
        this.winner = winner;
        this.phase = GamePhase.FINISHED;
    }

    public void addPlayerShot(Shot shot) { playerShots.add(shot); }
    public void addBotShot(Shot shot) { botShots.add(shot); }

    public boolean isFinished() { return phase == GamePhase.FINISHED; }
}