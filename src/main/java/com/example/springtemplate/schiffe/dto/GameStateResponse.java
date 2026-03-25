package com.example.springtemplate.schiffe.dto;



import com.example.springtemplate.schiffe.model.enums.CellState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GameStateResponse {

    private String gameId;
    private String phase;           // PLACEMENT, BATTLE, FINISHED
    private String difficulty;

    // Player's own board — full view (ships visible)
    private CellState[][] playerBoard;
    private List<ShipDTO> playerShips;

    // Bot's board — fog of war (ships hidden, only shots visible)
    private CellState[][] botBoard;
    private List<ShipDTO> botShips;  // sunk ships only, revealed to player

    private String winner;           // null, "PLAYER", or "BOT"

    public GameStateResponse() {}

}