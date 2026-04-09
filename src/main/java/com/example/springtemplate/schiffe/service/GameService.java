package com.example.springtemplate.schiffe.service;

import com.example.springtemplate.schiffe.dto.*;
import com.example.springtemplate.schiffe.model.*;
import com.example.springtemplate.schiffe.model.enums.*;
import com.example.springtemplate.schiffe.websocket.WebSocketEventPublisher;
import com.example.springtemplate.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final ValidationService validationService;
    private final BotService botService;
    private final LobbyService lobbyService;
    private final WebSocketEventPublisher eventPublisher;

    // In-memory game store — keyed by gameId
    private final Map<String, Game> games = new HashMap<>();

    public GameService(ValidationService validationService, BotService botService,
                       LobbyService lobbyService, WebSocketEventPublisher eventPublisher) {
        this.validationService = validationService;
        this.botService = botService;
        this.lobbyService = lobbyService;
        this.eventPublisher = eventPublisher;
    }

    // ── Create game ──────────────────────────────────────────────────────────

    /**
     * Creates a new game owned by the currently logged-in user.
     */
    public String createGame(Difficulty difficulty) {
        User currentUser = getCurrentUser();
        Game game = new Game(difficulty, currentUser.getId());
        placeBotShips(game.getBotBoard());
        botService.initGame(game.getId());
        games.put(game.getId(), game);
        return game.getId();
    }

    // ── Place player ships ───────────────────────────────────────────────────

    public List<String> placePlayerShips(String gameId, List<PlacementRequest> requests) {
        Game game = getGameForCurrentUser(gameId);

        if (game.getPhase() != GamePhase.PLACEMENT) {
            return List.of("Schiffe können jetzt nicht mehr platziert werden.");
        }

        List<String> errors = validationService.validatePlacements(requests);
        if (!errors.isEmpty()) {
            return errors;
        }

        Board playerBoard = game.getPlayerBoard();
        if (!playerBoard.getShips().isEmpty()) {
            resetBoard(playerBoard);
        }

        for (PlacementRequest req : requests) {
            Ship ship = new Ship(req.getShipType(), req.getX(), req.getY(), req.isHorizontal());
            playerBoard.placeShip(ship);
        }

        game.setPhase(GamePhase.BATTLE);
        return Collections.emptyList();
    }

    // ── Fire ─────────────────────────────────────────────────────────────────

    public FireResponse fire(String gameId, int x, int y) {
        Game game = getGameForCurrentUser(gameId);

        if (game.getPhase() != GamePhase.BATTLE) {
            throw new IllegalStateException("Das Spiel ist nicht in der Kampfphase.");
        }

        String shotError = validationService.validateShot(x, y, game.getBotBoard());
        if (shotError != null) {
            throw new IllegalArgumentException(shotError);
        }

        // ── Player shot ──────────────────────────────────────────────────────
        Cell hitCell = game.getBotBoard().receiveShot(x, y);
        ShotResultDTO playerResult = buildShotResult(x, y, hitCell);
        game.addPlayerShot(toShot(playerResult));

        FireResponse response = new FireResponse();
        response.setPlayerShot(playerResult);

        if (game.getBotBoard().allShipsSunk()) {
            game.setWinner("PLAYER");
            response.setWinner("PLAYER");
            response.setGamePhase(GamePhase.FINISHED.name());
            botService.removeGame(gameId);
            return response;
        }

        // ── Bot shot ─────────────────────────────────────────────────────────
        int[] botCoords = botService.pickShot(gameId, game.getPlayerBoard(), game.getDifficulty());
        Cell botHitCell = game.getPlayerBoard().receiveShot(botCoords[0], botCoords[1]);
        ShotResultDTO botResult = buildShotResult(botCoords[0], botCoords[1], botHitCell);
        game.addBotShot(toShot(botResult));

        botService.reportShotResult(gameId, botCoords[0], botCoords[1], botResult.getResult());

        response.setBotShot(botResult);

        if (game.getPlayerBoard().allShipsSunk()) {
            game.setWinner("BOT");
            response.setWinner("BOT");
            response.setGamePhase(GamePhase.FINISHED.name());
            botService.removeGame(gameId);
        } else {
            response.setGamePhase(GamePhase.BATTLE.name());
        }

        return response;
    }

    // ── Get state ────────────────────────────────────────────────────────────

    public GameStateResponse getState(String gameId) {
        Game game = getGameForCurrentUser(gameId);

        GameStateResponse response = new GameStateResponse();
        response.setGameId(game.getId());
        response.setPhase(game.getPhase().name());
        response.setDifficulty(game.getDifficulty().name());
        response.setWinner(game.getWinner());

        response.setPlayerBoard(game.getPlayerBoard().getFullView());
        response.setPlayerShips(
                game.getPlayerBoard().getShips().stream()
                        .map(ShipDTO::from)
                        .collect(Collectors.toList())
        );

        response.setBotBoard(game.getBotBoard().getFogOfWarView());
        response.setBotShips(
                game.getBotBoard().getShips().stream()
                        .filter(Ship::isSunk)
                        .map(ShipDTO::from)
                        .collect(Collectors.toList())
        );

        return response;
    }

    // ── Multiplayer: place ships ─────────────────────────────────────────────

    public List<String> placeShipsMultiplayer(String gameId, List<PlacementRequest> requests,
                                               User currentUser) {
        MultiplayerGame game = lobbyService.getGameForPlayer(gameId, currentUser.getId());

        if (game.getPhase() != GamePhase.PLACEMENT && game.getPhase() != GamePhase.WAITING_FOR_PLACEMENT) {
            return List.of("Schiffe können jetzt nicht mehr platziert werden.");
        }

        List<String> errors = validationService.validatePlacements(requests);
        if (!errors.isEmpty()) {
            return errors;
        }

        Board board = game.getBoardFor(currentUser.getId());
        if (!board.getShips().isEmpty()) {
            resetBoard(board);
        }

        for (PlacementRequest req : requests) {
            Ship ship = new Ship(req.getShipType(), req.getX(), req.getY(), req.isHorizontal());
            board.placeShip(ship);
        }

        // Mark this player as ready
        if (game.getPlayerOneId().equals(currentUser.getId())) {
            game.setPlayerOneReady(true);
        } else {
            game.setPlayerTwoReady(true);
        }

        if (game.isBothReady()) {
            game.setPhase(GamePhase.BATTLE);
            game.setCurrentTurn(game.getPlayerOneId());
            eventPublisher.publishToGame(gameId,
                    new BothReadyEvent(game.getPlayerOneEmail(), gameId));
        } else {
            game.setPhase(GamePhase.WAITING_FOR_PLACEMENT);
        }

        return Collections.emptyList();
    }

    // ── Multiplayer: fire ────────────────────────────────────────────────────

    public FireResponse fireMultiplayer(String gameId, int x, int y, User currentUser) {
        MultiplayerGame game = lobbyService.getGameForPlayer(gameId, currentUser.getId());

        if (game.getPhase() != GamePhase.BATTLE) {
            throw new IllegalStateException("Das Spiel ist nicht in der Kampfphase.");
        }

        if (!game.isPlayerTurn(currentUser.getId())) {
            throw new IllegalStateException("Nicht dein Zug.");
        }

        Board opponentBoard = game.getOpponentBoard(currentUser.getId());
        String shotError = validationService.validateShot(x, y, opponentBoard);
        if (shotError != null) {
            throw new IllegalArgumentException(shotError);
        }

        Cell hitCell = opponentBoard.receiveShot(x, y);
        ShotResultDTO shotResult = buildShotResult(x, y, hitCell);
        game.getShotsFor(currentUser.getId()).add(toShot(shotResult));

        boolean gameOver = opponentBoard.allShipsSunk();
        String winnerEmail = null;
        String winnerRole = null;

        if (gameOver) {
            winnerEmail = currentUser.getUsername();
            winnerRole = game.getRoleFor(currentUser.getId());
            game.setWinnerId(currentUser.getId());
            game.setWinnerEmail(winnerEmail);
            game.setPhase(GamePhase.FINISHED);
            eventPublisher.publishToGame(gameId, new GameOverEvent(winnerEmail, winnerRole));
        } else {
            game.switchTurn();
        }

        ShotFiredEvent shotEvent = new ShotFiredEvent(
                currentUser.getUsername(),
                game.getRoleFor(currentUser.getId()),
                shotResult,
                game.getNextTurnEmail(),
                game.getNextTurnRole(),
                winnerEmail,
                gameOver
        );
        eventPublisher.publishToGame(gameId, shotEvent);

        FireResponse response = new FireResponse();
        response.setPlayerShot(shotResult);
        response.setBotShot(null);
        response.setWinner(winnerEmail);
        response.setGamePhase(game.getPhase().name());
        return response;
    }

    // ── Multiplayer: get state ───────────────────────────────────────────────

    public GameStateResponse getStateMultiplayer(String gameId, User currentUser) {
        MultiplayerGame game = lobbyService.getGameForPlayer(gameId, currentUser.getId());

        Board myBoard = game.getBoardFor(currentUser.getId());
        Board opponentBoard = game.getOpponentBoard(currentUser.getId());

        GameStateResponse response = new GameStateResponse();
        response.setGameId(game.getId());
        response.setPhase(game.getPhase().name());
        response.setDifficulty(null);
        response.setWinner(game.getWinnerEmail());

        response.setPlayerBoard(myBoard.getFullView());
        response.setPlayerShips(
                myBoard.getShips().stream()
                        .map(ShipDTO::from)
                        .collect(Collectors.toList())
        );

        response.setBotBoard(opponentBoard.getFogOfWarView());
        response.setBotShips(
                opponentBoard.getShips().stream()
                        .filter(Ship::isSunk)
                        .map(ShipDTO::from)
                        .collect(Collectors.toList())
        );

        return response;
    }

    // ── Security helpers ─────────────────────────────────────────────────────

    /**
     * Returns the currently authenticated User from the SecurityContext.
     * Spring Security puts the User object there after JWT validation.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Kein authentifizierter Benutzer gefunden.");
        }
        return (User) authentication.getPrincipal();
    }

    /**
     * Fetches a game and verifies it belongs to the current user.
     * Throws NoSuchElementException if not found, SecurityException if wrong owner.
     */
    private Game getGameForCurrentUser(String gameId) {
        Game game = games.get(gameId);
        if (game == null) {
            throw new NoSuchElementException("Spiel nicht gefunden: " + gameId);
        }
        User currentUser = getCurrentUser();
        if (!game.getOwnerId().equals(currentUser.getId())) {
            throw new SecurityException("Zugriff verweigert: Das ist nicht dein Spiel.");
        }
        return game;
    }

    // ── Bot ship placement ───────────────────────────────────────────────────

    private void placeBotShips(Board board) {
        List<ShipType> fleet = List.of(
                ShipType.SCHLACHTSCHIFF,
                ShipType.KREUZER, ShipType.KREUZER,
                ShipType.ZERSTOERER, ShipType.ZERSTOERER, ShipType.ZERSTOERER,
                ShipType.UBOOT, ShipType.UBOOT, ShipType.UBOOT, ShipType.UBOOT
        );

        Random random = new Random();

        for (ShipType type : fleet) {
            boolean placed = false;
            int attempts = 0;

            while (!placed && attempts < 1000) {
                attempts++;
                int x = random.nextInt(Board.SIZE);
                int y = random.nextInt(Board.SIZE);
                boolean horizontal = random.nextBoolean();

                PlacementRequest req = new PlacementRequest();
                req.setShipType(type);
                req.setX(x);
                req.setY(y);
                req.setHorizontal(horizontal);

                List<String> errors = validationService.validateSinglePlacement(req, board);
                if (errors.isEmpty()) {
                    board.placeShip(new Ship(type, x, y, horizontal));
                    placed = true;
                }
            }

            if (!placed) {
                throw new IllegalStateException("Bot konnte Schiff nicht platzieren: " + type);
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ShotResultDTO buildShotResult(int x, int y, Cell cell) {
        ShotResult result;
        ShipType shipType = null;

        if (cell.getState() == CellState.SUNK) {
            result = ShotResult.SUNK;
            shipType = cell.getShip().getType();
        } else if (cell.getState() == CellState.HIT) {
            result = ShotResult.HIT;
            shipType = cell.getShip().getType();
        } else {
            result = ShotResult.MISS;
        }

        return new ShotResultDTO(x, y, result, shipType);
    }

    private Shot toShot(ShotResultDTO dto) {
        return new Shot(dto.getX(), dto.getY(), dto.getResult(), dto.getShipType());
    }

    private void resetBoard(Board board) {
        for (Ship ship : board.getShips()) {
            for (int[] coord : ship.getCoordinates()) {
                Cell cell = board.getCell(coord[0], coord[1]);
                cell.setState(CellState.EMPTY);
                cell.setShip(null);
            }
        }
        board.getShips().clear();
    }
}
