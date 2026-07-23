package org.example.batallanaval_fpoe.controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import org.example.batallanaval_fpoe.model.Casilla;
import org.example.batallanaval_fpoe.model.EstadoCasilla;
import org.example.batallanaval_fpoe.model.Tablero;

/**
 * Controlador principal — genera los tableros 10×10 visualmente
 * sin tocar la lógica del modelo.
 *
 * @author Daniel, Nicolás, Robert
 */
public class PrincipalController {

    // ── FXML injections ──────────────────────────────────
    @FXML private VBox playerBoardContainer;
    @FXML private VBox machineBoardContainer;
    @FXML private Label lblTurn;
    @FXML private Label lblStatus;
    @FXML private Label lblPlayerTitle;
    @FXML private Label lblMachineTitle;
    @FXML private HBox playerFleetInfo;
    @FXML private HBox machineFleetInfo;

    // ── Model ────────────────────────────────────────────
    private Tablero playerBoard;
    private Tablero machineBoard;

    // ── Visual grid references ───────────────────────────
    private final StackPane[][] playerCells = new StackPane[Tablero.TAMANO][Tablero.TAMANO];
    private final StackPane[][] machineCells = new StackPane[Tablero.TAMANO][Tablero.TAMANO];

    // ── Column letters ───────────────────────────────────
    private static final String[] COLS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    // ══════════════════════════════════════════════════════
    //  ISOMETRIC PROJECTION CONSTANTS
    // ══════════════════════════════════════════════════════

    /** Ancho de losa diamante en pantalla (eje X isométrico). */
    private double tileWidth  = 48.0;
    /** Alto de losa diamante en pantalla (eje Y isométrico). */
    private double tileHeight = 24.0;
    /** Origen X del tablero isométrico dentro del Pane. */
    private double originX = 300.0;
    /** Origen Y del tablero isométrico dentro del Pane. */
    private double originY = 40.0;

    /** Rectángulo semi-transparente que resalta la casilla bajo el cursor. */
    private Polygon highlight;
    /** Pane overlay para el debug grid. */
    private Pane debugOverlay;
    /** StackPane padre que contiene al VBox (para el overlay). */
    private StackPane boardStack;
    /** Pane que contiene los diamantes isométricos del tablero enemigo. */
    private Pane isoTilesPane;
    /** Referencia a cada diamante isométrico para actualizar su color. */
    private final Polygon[][] isoTiles = new Polygon[Tablero.TAMANO][Tablero.TAMANO];

    /** StackPane del tablero del jugador (su padre en el FXML). */
    private StackPane playerBoardStack;
    /** Pane que contiene los diamantes isométricos del tablero del jugador. */
    private Pane playerIsoTilesPane;
    /** Referencia a cada diamante isométrico del jugador para actualizar su color. */
    private final Polygon[][] playerIsoTiles = new Polygon[Tablero.TAMANO][Tablero.TAMANO];
    /** Origen X del tablero isométrico del jugador dentro del Pane. */
    private double playerOriginX = 300.0;
    /** Origen Y del tablero isométrico del jugador dentro del Pane. */
    private double playerOriginY = 40.0;

    // ══════════════════════════════════════════════════════
    //  INITIALIZATION
    // ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        playerBoard = new Tablero();
        machineBoard = new Tablero();

        playerBoard.colocarFlotaAleatoria();
        machineBoard.colocarFlotaAleatoria();

        // ── Player isometric board setup ──
        playerBoardStack = (StackPane) playerBoardContainer.getParent();
        buildPlayerIsometricBoard();

        // ── Machine isometric board setup ──
        boardStack = (StackPane) machineBoardContainer.getParent();
        buildIsometricBoard();

        buildFleetInfo(playerFleetInfo, "Jugador");
        buildFleetInfo(machineFleetInfo, "Enemigo");

        setupResponsiveGrid();
        setupIsometricInteraction(boardStack);

        updateStatus("Tu turno — selecciona una casilla del tablero enemigo");
    }

    // ══════════════════════════════════════════════════════
    //  RESPONSIVE GRID — recalcula al resize
    // ══════════════════════════════════════════════════════

    /**
     * Bindea los listeners de tamaño al StackPane y calcula la grilla
     * isométrica para que coincida con la imagen de fondo (contain).
     */
    private void setupResponsiveGrid() {
        boardStack.widthProperty().addListener((obs, oldVal, newVal) -> recalculateGrid());
        boardStack.heightProperty().addListener((obs, oldVal, newVal) -> recalculateGrid());
        playerBoardStack.widthProperty().addListener((obs, oldVal, newVal) -> recalculateGrid());
        playerBoardStack.heightProperty().addListener((obs, oldVal, newVal) -> recalculateGrid());

        // Primer cálculo (post-layout)
        boardStack.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                boardStack.widthProperty().addListener((o, ov, nv) -> recalculateGrid());
                playerBoardStack.widthProperty().addListener((o, ov, nv) -> recalculateGrid());
                // Layout pulse: esperar a que el layout se resuelva
                newS.getRoot().layoutBoundsProperty().addListener((o, ov, nv) -> {
                    recalculateGrid();
                });
            }
        });

        // Cálculo inicial diferido
        javafx.application.Platform.runLater(this::recalculateGrid);
    }

    /**
     * Recalcula tileWidth, tileHeight, originX, originY a partir del
     * tamaño real del StackPane contenedor. Mantiene la proporción 2:1
     * del grid isométrico y centra la imagen (contain behavior).
     */
    private void recalculateGrid() {
        // ── Machine board dimensions ──
        Insets pad = boardStack.getPadding();
        double vw = boardStack.getWidth() - pad.getLeft() - pad.getRight();
        double vh = boardStack.getHeight() - pad.getTop() - pad.getBottom();
        if (vw <= 0 || vh <= 0) return;

        int N = Tablero.TAMANO;

        // Ajustar al contenedor manteniendo proporción 2:1
        double gridW = (N - 1);
        double gridH = (N - 1) / 2.0;

        double escalaW = vw / (gridW + 1);
        double escalaH = vh / (gridH + 1);
        double escala  = Math.min(escalaW, escalaH);

        tileWidth  = escala;
        tileHeight = escala / 2.0;

        // Origen machine board
        originX = vw / 2.0;
        originY = (vh - (N - 1) * tileHeight) / 2.0;

        // ── Player board origin ──
        Insets pPad = playerBoardStack.getPadding();
        double pvw = playerBoardStack.getWidth() - pPad.getLeft() - pPad.getRight();
        double pvh = playerBoardStack.getHeight() - pPad.getTop() - pPad.getBottom();

        if (pvw > 0 && pvh > 0) {
            playerOriginX = pvw / 2.0;
            playerOriginY = (pvh - (N - 1) * tileHeight) / 2.0;
        }

        // Redibujar ambos tableros
        buildIsometricBoard();
        buildPlayerIsometricBoard();
        if (debugOverlay != null) {
            drawDebugGrid();
        }

        // Actualizar highlight si visible
        if (highlight != null && highlight.isVisible()) {
            highlight.setVisible(false);
        }

        System.out.printf(
            "[GRID] Recalculado — machine(W=%.0f H=%.0f) player(W=%.0f H=%.0f) | tileW=%.1f tileH=%.1f%n",
            vw, vh, pvw, pvh, tileWidth, tileHeight
        );
    }

    // ══════════════════════════════════════════════════════
    //  BOARD BUILDER
    // ══════════════════════════════════════════════════════

    /**
     * Construye un tablero visual completo dentro del VBox contenedor.
     *
     * @param container   VBox destino del FXML
     * @param cellRefs    array 10×10 de StackPane para referencia visual
     * @param tablero     el modelo Tablero asociado
     * @param isEnemy     true si es el tablero enemigo (click para disparar)
     */
    private void buildBoard(VBox container, StackPane[][] cellRefs, Tablero tablero, boolean isEnemy) {
        container.getChildren().clear();

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(2);
        grid.setVgap(2);

        // ── Corner spacer ──
        StackPane corner = new StackPane();
        corner.getStyleClass().add("corner-spacer");
        grid.add(corner, 0, 0);

        // ── Column headers (A-J) ──
        for (int c = 0; c < Tablero.TAMANO; c++) {
            StackPane header = new StackPane(new Label(COLS[c]));
            header.getStyleClass().add("col-header");
            grid.add(header, c + 1, 0);
        }

        // ── Rows ──
        for (int f = 0; f < Tablero.TAMANO; f++) {
            // Row number header
            StackPane rowHeader = new StackPane(new Label(String.valueOf(f + 1)));
            rowHeader.getStyleClass().add("row-header");
            grid.add(rowHeader, 0, f + 1);

            // Cells
            for (int c = 0; c < Tablero.TAMANO; c++) {
                StackPane cell = createCell(f, c, tablero, isEnemy);
                cellRefs[f][c] = cell;
                grid.add(cell, c + 1, f + 1);
            }
        }

        container.getChildren().add(grid);
    }

    // ══════════════════════════════════════════════════════
    //  ISOMETRIC BOARD BUILDER
    // ══════════════════════════════════════════════════════

    /**
     * Construye el tablero enemigo como diamantes isométricos
     * superpuestos al StackPane, pintados según el estado del modelo.
     * Orden de dibujado: back-to-front (mayor row+col al frente).
     */
    private void buildIsometricBoard() {
        if (isoTilesPane == null) {
            isoTilesPane = new Pane();
            isoTilesPane.setMouseTransparent(true);
            // Insertar después del VBox (índice 1) para que quede encima del fondo
            boardStack.getChildren().add(1, isoTilesPane);
        }
        isoTilesPane.getChildren().clear();

        int N = Tablero.TAMANO;
        double hw = tileWidth  / 2.0;
        double hh = tileHeight / 2.0;

        // Back-to-front: dibujar por diagonales (row + col creciente)
        for (int sum = 0; sum <= 2 * (N - 1); sum++) {
            for (int row = Math.max(0, sum - N + 1); row <= Math.min(sum, N - 1); row++) {
                int col = sum - row;
                Point2D center = gridToScreen(row, col);

                // Diamante: norte → este → sur → oeste
                Polygon tile = new Polygon(
                    center.getX(),      center.getY() - hh,
                    center.getX() + hw, center.getY(),
                    center.getX(),      center.getY() + hh,
                    center.getX() - hw, center.getY()
                );

                Casilla casilla = machineBoard.getCasillas()[row][col];
                applyIsoColor(tile, casilla);

                tile.setStroke(Color.web("#1e3a5f"));
                tile.setStrokeWidth(0.8);

                isoTiles[row][col] = tile;
                isoTilesPane.getChildren().add(tile);
            }
        }
    }

    /**
     * Aplica color al diamante isométrico según el estado de la casilla.
     * @param showShips si true, OCUPADA se pinta con color de barco (azul);
     *                  si false, OCUPADA se oculta igual que VACIA.
     */
    private void applyIsoColor(Polygon tile, Casilla casilla, boolean showShips) {
        String color;
        switch (casilla.getEstado()) {
            case OCUPADA:
                color = showShips ? "#2980b9" : "#0e2a3d";
                break;
            case AGUA:     color = "#1a3a5c"; break;
            case TOCADO:   color = "#7b241c"; break;
            case HUNDIDO:  color = "#4a1a1a"; break;
            default:       color = "#0e2a3d"; break;
        }
        tile.setFill(Color.web(color));
    }

    /** Atajo para tablero enemigo — oculta barcos. */
    private void applyIsoColor(Polygon tile, Casilla casilla) {
        applyIsoColor(tile, casilla, false);
    }

    // ══════════════════════════════════════════════════════
    //  PLAYER ISOMETRIC BOARD BUILDER
    // ══════════════════════════════════════════════════════

    /**
     * Construye el tablero del jugador como diamantes isométricos
     * mostrando los barcos (showShips = true).
     */
    private void buildPlayerIsometricBoard() {
        if (playerIsoTilesPane == null) {
            playerIsoTilesPane = new Pane();
            playerIsoTilesPane.setMouseTransparent(true);
            playerBoardStack.getChildren().add(1, playerIsoTilesPane);
        }
        playerIsoTilesPane.getChildren().clear();

        int N = Tablero.TAMANO;
        double hw = tileWidth  / 2.0;
        double hh = tileHeight / 2.0;

        // Back-to-front
        for (int sum = 0; sum <= 2 * (N - 1); sum++) {
            for (int row = Math.max(0, sum - N + 1); row <= Math.min(sum, N - 1); row++) {
                int col = sum - row;
                Point2D center = gridToScreen(row, col, playerOriginX, playerOriginY);

                Polygon tile = new Polygon(
                    center.getX(),      center.getY() - hh,
                    center.getX() + hw, center.getY(),
                    center.getX(),      center.getY() + hh,
                    center.getX() - hw, center.getY()
                );

                Casilla casilla = playerBoard.getCasillas()[row][col];
                applyIsoColor(tile, casilla, true);

                tile.setStroke(Color.web("#1e3a5f"));
                tile.setStrokeWidth(0.8);

                playerIsoTiles[row][col] = tile;
                playerIsoTilesPane.getChildren().add(tile);
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  CELL CREATION
    // ══════════════════════════════════════════════════════

    /**
     * Crea una casilla visual individual.
     */
    private StackPane createCell(int fila, int columna, Tablero tablero, boolean isEnemy) {
        StackPane cell = new StackPane();
        cell.setAlignment(Pos.CENTER);
        cell.getStyleClass().addAll("grid-cell", "cell-vacia");

        // Store row/col as properties for event handling
        cell.getProperties().put("fila", fila);
        cell.getProperties().put("columna", columna);

        if (isEnemy) {
            // Click handler para disparar
            cell.setOnMouseClicked(e -> handleShot(fila, columna));
            cell.setOnMouseEntered(e -> {
                EstadoCasilla estado = tablero.getCasillas()[fila][columna].getEstado();
                if (estado == EstadoCasilla.VACIA || estado == EstadoCasilla.OCUPADA) {
                    cell.setStyle("-fx-border-color: #f39c12; -fx-border-width: 2;");
                }
            });
            cell.setOnMouseExited(e -> {
                cell.setStyle("");
            });
        } else {
            // Tablero del jugador — solo visual, no clickable
            cell.setDisable(true);
            cell.setOpacity(1.0);
        }

        // Actualizar apariencia según el estado del modelo
        refreshCellVisual(cell, tablero.getCasillas()[fila][columna], isEnemy);

        return cell;
    }

    // ══════════════════════════════════════════════════════
    //  CELL VISUAL REFRESH
    // ══════════════════════════════════════════════════════

    /**
     * Sincroniza la apariencia visual de una celda con su estado en el modelo.
     */
    private void refreshCellVisual(StackPane cell, Casilla casilla, boolean showShips) {
        cell.getStyleClass().removeAll(
            "cell-vacia", "cell-ocupada", "cell-agua", "cell-tocado", "cell-hundido"
        );

        // Limpiar icono previo
        cell.getChildren().clear();

        EstadoCasilla estado = casilla.getEstado();

        switch (estado) {
            case VACIA:
                cell.getStyleClass().add("cell-vacia");
                break;

            case OCUPADA:
                if (showShips) {
                    // En tablero enemigo NO mostramos barcos
                    cell.getStyleClass().add("cell-vacia");
                } else {
                    cell.getStyleClass().add("cell-ocupada");
                    cell.getChildren().add(createIcon("■", "#2980b9"));
                }
                break;

            case AGUA:
                cell.getStyleClass().add("cell-agua");
                cell.getChildren().add(createIcon("○", "#5d7b96"));
                break;

            case TOCADO:
                cell.getStyleClass().add("cell-tocado");
                cell.getChildren().add(createIcon("✕", "#e74c3c"));
                break;

            case HUNDIDO:
                cell.getStyleClass().add("cell-hundido");
                cell.getChildren().add(createIcon("✕", "#f1948a"));
                break;
        }
    }

    /**
     * Crea un Label con un símbolo/icono simple.
     */
    private Label createIcon(String symbol, String color) {
        Label icon = new Label(symbol);
        icon.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        return icon;
    }

    // ══════════════════════════════════════════════════════
    //  SHOT HANDLER
    // ══════════════════════════════════════════════════════

    /**
     * Maneja el clic del jugador en el tablero enemigo.
     */
    private void handleShot(int fila, int columna) {
        try {
            var resultado = machineBoard.disparar(fila, columna);
            Casilla casilla = machineBoard.getCasillas()[fila][columna];

            // Actualizar diamante isométrico
            if (isoTiles[fila][columna] != null) {
                applyIsoColor(isoTiles[fila][columna], casilla);
            }

            switch (resultado) {
                case AGUA:
                    updateStatus("Agua... nada por ahí.");
                    break;
                case TOCADO:
                    updateStatus("¡Tocado! Impacto en el barco enemigo.");
                    break;
                case HUNDIDO:
                    updateStatus("¡Barco enemigo hundido!");
                    break;
                case VICTORIA:
                    updateStatus("¡VICTORIA! Has hundido toda la flota enemiga.");
                    lblTurn.setText("¡GANASTE!");
                    break;
            }
        } catch (Exception e) {
            updateStatus("Casilla ya disparada — elige otra.");
        }
    }

    // ══════════════════════════════════════════════════════
    //  FLEET INFO PANELS
    // ══════════════════════════════════════════════════════

    /**
     * Construye el panel informativo de la flota debajo de cada tablero.
     */
    private void buildFleetInfo(HBox container, String label) {
        container.getChildren().clear();

        Label title = new Label(label + " — Flota:");
        title.getStyleClass().add("ship-name");
        container.getChildren().add(title);

        String[] shipNames = {"Portaaviones", "Submarino", "Destructor", "Fragata"};
        int[] shipSizes = {4, 3, 2, 1};
        int[] shipCounts = {1, 2, 3, 4};

        for (int i = 0; i < shipNames.length; i++) {
            Label ship = new Label(shipNames[i] + " (" + shipSizes[i] + ")");
            ship.getStyleClass().add("ship-count");
            ship.setPadding(new Insets(0, 4, 0, 12));
            container.getChildren().add(ship);
        }
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════

    private void updateStatus(String message) {
        lblStatus.setText(message);
    }

    /**
     * Desactiva todas las celtas de un tablero (fin de juego).
     */
    private void disableBoard(StackPane[][] cells) {
        for (int f = 0; f < Tablero.TAMANO; f++) {
            for (int c = 0; c < Tablero.TAMANO; c++) {
                cells[f][c].setOnMouseClicked(null);
                cells[f][c].setOnMouseEntered(null);
                cells[f][c].setOnMouseExited(null);
            }
        }
    }

    @FXML
    private void showMachine(ActionEvent event) {
        updateStatus("Vista del tablero enemigo actualizada.");
    }

    // ══════════════════════════════════════════════════════
    //  1. ISOMETRIC CONVERSION METHODS
    // ══════════════════════════════════════════════════════

    /**
     * Convierte coordenadas de cuadrícula (fila, columna) a pantalla (X, Y).
     * Proyección isométrica estándar: ejes a +30° y -30°.
     *
     *   screenX = originX + (col - row) * (tileWidth  / 2)
     *   screenY = originY + (col + row) * (tileHeight / 2)
     *
     * @param row fila de la casilla (0-9)
     * @param col columna de la casilla (0-9)
     * @return Point2D con el centro de la casilla en pantalla
     */
    public Point2D gridToScreen(int row, int col) {
        double sx = originX + (col - row) * (tileWidth  / 2.0);
        double sy = originY + (col + row) * (tileHeight / 2.0);
        return new Point2D(sx, sy);
    }

    /**
     * Variante parametrizada — permite proyectar con orígenes y tileSize
     * customizados (usado por el tablero del jugador que tiene otro StackPane).
     */
    private Point2D gridToScreen(int row, int col, double ox, double oy) {
        double sx = ox + (col - row) * (tileWidth  / 2.0);
        double sy = oy + (col + row) * (tileHeight / 2.0);
        return new Point2D(sx, sy);
    }

    /**
     * Convierte coordenadas de pantalla (mouseX, mouseY) a [fila, columna].
     * Inversión de la matriz isométrica:
     *
     *   dx = (mouseX - originX) / (tileWidth  / 2)
     *   dy = (mouseY - originY) / (tileHeight / 2)
     *   col = (dx + dy) / 2
     *   row = (dy - dx) / 2
     *
     * @return int[]{fila, columna} o null si está fuera del tablero
     */
    public int[] screenToGrid(double mouseX, double mouseY) {
        double dx = (mouseX - originX) / (tileWidth  / 2.0);
        double dy = (mouseY - originY) / (tileHeight / 2.0);

        double col = (dx + dy) / 2.0;
        double row = (dy - dx) / 2.0;

        int r = (int) Math.round(row);
        int c = (int) Math.round(col);

        if (r < 0 || r >= Tablero.TAMANO || c < 0 || c >= Tablero.TAMANO) {
            return null;
        }
        return new int[]{r, c};
    }

    // ══════════════════════════════════════════════════════
    //  2. DEBUG GRID OVERLAY
    // ══════════════════════════════════════════════════════

    /**
     * Dibuja una cuadrícula de depuración sobre el Pane del tablero enemigo.
     * Cada casilla es un rombo semi-transparente (rojo = fuera, verde = válido).
     * Ajusta originX, originY, tileWidth, tileHeight hasta que coincida.
     */
    public void drawDebugGrid() {
        if (debugOverlay == null) {
            debugOverlay = new Pane();
            debugOverlay.setMouseTransparent(true);
            boardStack.getChildren().add(debugOverlay);
        }
        debugOverlay.getChildren().clear();

        double hw = tileWidth  / 2.0;
        double hh = tileHeight / 2.0;

        for (int row = 0; row < Tablero.TAMANO; row++) {
            for (int col = 0; col < Tablero.TAMANO; col++) {
                Point2D center = gridToScreen(row, col);

                // 4 vértices del rombo
                Polygon diamond = new Polygon(
                    center.getX(),      center.getY() - hh,  // norte
                    center.getX() + hw, center.getY(),        // este
                    center.getX(),      center.getY() + hh,  // sur
                    center.getX() - hw, center.getY()         // oeste
                );

                diamond.setFill(Color.rgb(0, 200, 80, 0.15));
                diamond.setStroke(Color.rgb(0, 200, 80, 0.6));
                diamond.setStrokeWidth(1.0);

                debugOverlay.getChildren().add(diamond);
            }
        }

        System.out.printf(
            "[DEBUG] Grid dibujado — originX=%.1f  originY=%.1f  tileW=%.1f  tileH=%.1f%n",
            originX, originY, tileWidth, tileHeight
        );
    }

    /**
     * Elimina el overlay de depuración.
     */
    public void clearDebugGrid() {
        if (debugOverlay != null) {
            debugOverlay.getChildren().clear();
        }
    }

    // ══════════════════════════════════════════════════════
    //  3. INTERACTION & HIGHLIGHTING
    // ══════════════════════════════════════════════════════

    /**
     * Configura los eventos de mouse sobre el StackPane del tablero enemigo
     * para detección isométrica de casillas.
     * Los parámetros de grilla se recalculan automáticamente al resize.
     *
     * @param boardPane el StackPane que contiene al VBox del tablero enemigo
     */
    public void setupIsometricInteraction(StackPane boardPane) {
        // Highlight polígono (se mueve con el cursor)
        highlight = new Polygon();
        highlight.setFill(Color.rgb(243, 156, 18, 0.25));
        highlight.setStroke(Color.rgb(243, 156, 18, 0.8));
        highlight.setStrokeWidth(2.0);
        highlight.setMouseTransparent(true);
        highlight.setVisible(false);
        boardPane.getChildren().add(highlight);

        // ── Mouse Moved: resaltar casilla bajo cursor ──
        boardPane.setOnMouseMoved((MouseEvent e) -> {
            // Convertir coords del StackPane al área de contenido (restar padding)
            Insets pad = boardPane.getPadding();
            double mx = e.getX() - pad.getLeft();
            double my = e.getY() - pad.getTop();
            double hw = tileWidth  / 2.0;
            double hh = tileHeight / 2.0;

            int[] cell = screenToGrid(mx, my);

            if (cell == null) {
                highlight.setVisible(false);
                return;
            }

            int row = cell[0];
            int col = cell[1];
            Point2D center = gridToScreen(row, col);

            highlight.getPoints().setAll(
                center.getX(),      center.getY() - hh,
                center.getX() + hw, center.getY(),
                center.getX(),      center.getY() + hh,
                center.getX() - hw, center.getY()
            );
            highlight.setVisible(true);
        });

        boardPane.setOnMouseExited(e -> highlight.setVisible(false));

        // ── Mouse Clicked: disparar ──
        boardPane.setOnMouseClicked((MouseEvent e) -> {
            Insets pad = boardPane.getPadding();
            double mx = e.getX() - pad.getLeft();
            double my = e.getY() - pad.getTop();
            int[] cell = screenToGrid(mx, my);

            if (cell == null) return;

            handleShot(cell[0], cell[1]);
        });
    }
}