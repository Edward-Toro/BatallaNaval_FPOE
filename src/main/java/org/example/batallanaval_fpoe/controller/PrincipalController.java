package org.example.batallanaval_fpoe.controller;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.example.batallanaval_fpoe.NavalBattleApp;
import org.example.batallanaval_fpoe.model.Barco;
import org.example.batallanaval_fpoe.model.BatallaNavalFacade;
import org.example.batallanaval_fpoe.model.Casilla;
import org.example.batallanaval_fpoe.model.EstadoCasilla;
import org.example.batallanaval_fpoe.model.EstadoDisparo;
import org.example.batallanaval_fpoe.model.Tablero;
import org.example.batallanaval_fpoe.model.TipoBarco;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * Controlador de la Vista Principal (Pantalla de Juego) — Batalla Naval FPOE.
 * <p>
 * Esta clase administra la interfaz de usuario orientada a eventos (JavaFX) durante la fase activa
 * del juego de Batalla Naval. Implementa un motor gráfico 2.5D en proyección isométrica responsivo
 * que se adapta automáticamente a las dimensiones de la ventana.
 * </p>
 * <p>
 * <b>Patrones de Diseño y Principios POE Aplicados:</b>
 * <ul>
 *   <li><b>Separación de Responsabilidades (SoC):</b> La vista y el controlador no manipulan la lógica
 *   interna del juego; todas las consultas de estado y acciones de disparo se delegan a la fachada {@link BatallaNavalFacade}.</li>
 *   <li><b>Programación Orientada a Eventos (POE):</b> Captura interacciones del usuario mediante listeners
 *   en JavaFX (eventos {@code MouseEvent}, observadores de propiedades {@code ReadOnlyDoubleProperty}).</li>
 *   <li><b>Renderizado Isométrico Responsivo:</b> Convierte matriz bidimensional de 10×10 en rombos isométricos
 *   con orden de dibujado back-to-front (por suma de diagonal) para preservar el orden Z visual.</li>
 * </ul>
 * </p>
 *
 * @author Daniel, Nicolás, Robert
 * @version 2.0
 * @see BatallaNavalFacade
 * @see Tablero
 * @see Casilla
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
    @FXML private Button btnShowMachine;
    @FXML private Button btnNewGame;

    // ── Model ────────────────────────────────────────────
    /** Fachada principal del modelo de dominio que coordina las reglas de juego. */
    private BatallaNavalFacade facade;
    /** Indica si el jugador ya realizó su primer disparo; a partir de ahí se bloquea "Ver Máquina". */
    private boolean primerDisparoRealizado = false;
    /** Referencia al tablero del jugador. */
    private Tablero playerBoard;
    /** Referencia al tablero de la máquina (enemigo). */
    private Tablero machineBoard;
    /** Termina partida*/
    private boolean partidaTerminada = false;

    // ── Visual grid references ───────────────────────────
    /** Matriz 10×10 de nodos JavaFX para el tablero 2D plano del jugador. */
    private final StackPane[][] playerCells = new StackPane[Tablero.TAMANO][Tablero.TAMANO];
    /** Matriz 10×10 de nodos JavaFX para el tablero 2D plano de la máquina. */
    private final StackPane[][] machineCells = new StackPane[Tablero.TAMANO][Tablero.TAMANO];

    // ── Column letters ───────────────────────────────────
    /** Etiquetas alfabéticas para la identificación de columnas en los tableros (A - J). */
    private static final String[] COLS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    // ── Ship images overlay ──────────────────────────────
    /** Pane transparente superpuesto para renderizar las imágenes vectoriales/sprites de los barcos. */
    private Pane shipImagesPane;
    /** Mapeo entre objetos Barco del modelo y su nodo visual ImageView correspondiente. */
    private final Map<Barco, ImageView> shipImageMap = new java.util.HashMap<>();
    /** Caché de imágenes de barcos por tipo para optimizar memoria y rendimiento. */
    private final Map<TipoBarco, Image> imageCache = new EnumMap<>(TipoBarco.class);


    // ══════════════════════════════════════════════════════
    //  ISOMETRIC PROJECTION CONSTANTS
    // ══════════════════════════════════════════════════════

    /** Ancho de losa diamante en pantalla en píxeles (eje X isométrico). */
    private double tileWidth  = 48.0;
    /** Alto de losa diamante en pantalla en píxeles (eje Y isométrico). */
    private double tileHeight = 24.0;
    /** Coordenada X del origen del tablero enemigo dentro del Pane contenedor. */
    private double originX = 300.0;
    /** Coordenada Y del origen del tablero enemigo dentro del Pane contenedor. */
    private double originY = 40.0;

    /** Polígono semi-transparente de resaltado táctico para la casilla bajo el cursor. */
    private Polygon highlight;
    /** Pane overlay para la rejilla de depuración geométrica. */
    private Pane debugOverlay;
    /** Contenedor StackPane padre que aloja la vista isométrica del enemigo. */
    private StackPane boardStack;
    /** Pane que aloja la colección de diamantes isométricos del tablero enemigo. */
    private Pane isoTilesPane;
    /** Matriz 10×10 de Polígonos isométricos del tablero de la máquina. */
    private final Polygon[][] isoTiles = new Polygon[Tablero.TAMANO][Tablero.TAMANO];

    /** Contenedor StackPane padre que aloja la vista isométrica del jugador. */
    private StackPane playerBoardStack;
    /** Pane que aloja la colección de diamantes isométricos del tablero del jugador. */
    private Pane playerIsoTilesPane;
    /** Matriz 10×10 de Polígonos isométricos del tablero del jugador. */
    private final Polygon[][] playerIsoTiles = new Polygon[Tablero.TAMANO][Tablero.TAMANO];
    /** Coordenada X del origen del tablero del jugador dentro de su Pane. */
    private double playerOriginX = 300.0;
    /** Coordenada Y del origen del tablero del jugador dentro de su Pane. */
    private double playerOriginY = 40.0;

    // ══════════════════════════════════════════════════════
    //  INITIALIZATION
    // ══════════════════════════════════════════════════════

    /**
     * Inicializa los componentes estáticos de la interfaz tras la carga del FXML.
     * Construye las barras de estado y paneles informativos iniciales de ambas flotas.
     */
    @FXML
    public void initialize() {
        buildFleetInfo(playerFleetInfo, "Jugador");
        buildFleetInfo(machineFleetInfo, "Enemigo");
        updateStatus("Preparando tableros...");
    }

    /**
     * Inyecta la fachada del modelo pre-configurada (con la flota colocada) y construye
     * los tableros isométricos e interactivos.
     *
     * @param facade Instancia de {@link BatallaNavalFacade} que actúa como punto de acceso al modelo.
     */
    public void setFacade(BatallaNavalFacade facade) {
        this.facade = facade;
        this.playerBoard = facade.getTableroJugador();
        this.machineBoard = facade.getTableroMaquina();

        // ── Player isometric board setup ──
        playerBoardStack = (StackPane) playerBoardContainer.getParent();
        buildPlayerIsometricBoard();

        // ── Machine isometric board setup ──
        boardStack = (StackPane) machineBoardContainer.getParent();
        buildIsometricBoard();

        setupResponsiveGrid();
        setupIsometricInteraction(boardStack);
        setupShowMachineHoldListener();

        // Pintar los barcos del jugador sobre su tablero
        renderShipImages();

        updateStatus("Tu turno — selecciona una casilla del tablero enemigo");
    }

    /**
     * Configura los eventos de presionar y soltar sobre el botón "Ver Máquina".
     * Muestra temporalmente la ubicación de los barcos enemigos mientras se mantenga presionado el botón.
     */
    private void setupShowMachineHoldListener() {
        if (btnShowMachine == null) return;

        btnShowMachine.setOnMousePressed(e -> {
            revealMachineBoard(true);
            updateStatus("Inspeccionando posición de la flota enemiga...");
        });

        btnShowMachine.setOnMouseReleased(e -> {
            revealMachineBoard(false);
            updateStatus("Tu turno — selecciona una casilla del tablero enemigo");
        });
    }

    /**
     * Revela u oculta temporalmente la ubicación de la flota enemiga en la grilla isométrica.
     */
    private void revealMachineBoard(boolean reveal) {
        if (isoTilesPane == null || machineBoard == null) return;

        // Reconstruye el tablero enemigo siempre oculto (comportamiento normal)
        buildGenericIsometricBoard(
                isoTilesPane,
                machineBoard,
                isoTiles,
                originX,
                originY,
                false
        );

        // Si se está revelando, resalta en amarillo solo las casillas con barco
        if (reveal) {
            int contador = 0;
            for (int f = 0; f < Tablero.TAMANO; f++) {
                for (int c = 0; c < Tablero.TAMANO; c++) {
                    Casilla casilla = machineBoard.getCasillas()[f][c];
                    if (casilla.tieneBarco() && isoTiles[f][c] != null) {
                        isoTiles[f][c].setFill(Color.web("#f1c40f"));
                        contador++;
                    }
                }
            }
            System.out.println("[DEBUG] Casillas pintadas de amarillo: " + contador);

        }
    }

    /**
     * Maneja la acción del botón "Nueva Partida", reiniciando el flujo del juego
     * y regresando a la pantalla de alistamiento y colocación de la flota.
     */
    @FXML
    private void onNewGame(ActionEvent event) {
        NavalBattleApp.restartGame();
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

    private double lastVw = -1;
    private double lastVh = -1;
    private void recalculateGrid() {
        if (boardStack == null || playerBoardStack == null) return;

        // ── Machine board dimensions ──
        Insets pad = boardStack.getPadding();
        double vw = boardStack.getWidth() - pad.getLeft() - pad.getRight();
        double vh = boardStack.getHeight() - pad.getTop() - pad.getBottom();
        if (vw <= 0 || vh <= 0) return;
        if (Math.abs(vw - lastVw) < 2 && Math.abs(vh - lastVh) < 2) return;
        lastVw = vw;
        lastVh = vh;

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

        renderShipImages();

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

    /**
 * Método genérico para construir la capa isométrica de cualquier tablero.
 */
    private void buildGenericIsometricBoard(
        Pane targetPane, 
        Tablero tablero, 
        Polygon[][] tileArray, 
        double ox, 
        double oy, 
        boolean showShips) {

        if (targetPane == null || tablero == null) return;
        targetPane.getChildren().clear();

        // 1. Dibujar grilla extendida difuminada en el fondo (100% alineada con el tablero)
        drawExtendedIsometricGrid(targetPane, ox, oy);

        int N = Tablero.TAMANO;
        double hw = tileWidth / 2.0;
        double hh = tileHeight / 2.0;

        for (int sum = 0; sum <= 2 * (N - 1); sum++) {
            for (int row = Math.max(0, sum - N + 1); row <= Math.min(sum, N - 1); row++) {
                int col = sum - row;
                Point2D center = gridToScreen(row, col, ox, oy);

                Polygon tile = new Polygon(
                    center.getX(),      center.getY() - hh,
                    center.getX() + hw, center.getY(),
                    center.getX(),      center.getY() + hh,
                    center.getX() - hw, center.getY()
                );

                Casilla casilla = tablero.getCasillas()[row][col];
                applyIsoColor(tile, casilla, showShips);

                tile.setStroke(Color.web("#2392b8"));
                tile.setStrokeWidth(1.0);

                tileArray[row][col] = tile;
                targetPane.getChildren().add(tile);

                // 2. Superponer imagen táctica según el estado de la casilla (Fuego, Bomba, Boom, Agua)
                applyIsoOverlay(targetPane, casilla, center, showShips);
            }
        }

        drawIsometricHeaders(targetPane, ox, oy);
    }

    /**
     * Dibuja los encabezados isométricos de filas (1-10) y columnas (A-J).
     */
    private void drawIsometricHeaders(Pane targetPane, double ox, double oy) {
        double hw = tileWidth / 2.0;
        double hh = tileHeight / 2.0;

        // Encabezados de Columna (A - J)
        for (int col = 0; col < Tablero.TAMANO; col++) {
            Point2D center = gridToScreen(0, col, ox, oy);
            Label label = new Label(COLS[col]);
            label.setStyle("-fx-text-fill: #7dd3fc; -fx-font-size: 11px; -fx-font-weight: bold;");
            label.setMouseTransparent(true);
            label.setLayoutX(center.getX() + hw / 2.0 - 4.0);
            label.setLayoutY(center.getY() - hh - 16.0);
            targetPane.getChildren().add(label);
        }

        // Encabezados de Fila (1 - 10)
        for (int row = 0; row < Tablero.TAMANO; row++) {
            Point2D center = gridToScreen(row, 0, ox, oy);
            Label label = new Label(String.valueOf(row + 1));
            label.setStyle("-fx-text-fill: #7dd3fc; -fx-font-size: 11px; -fx-font-weight: bold;");
            label.setMouseTransparent(true);
            label.setLayoutX(center.getX() - hw / 2.0 - 8.0);
            label.setLayoutY(center.getY() - hh - 16.0);
            targetPane.getChildren().add(label);
        }
    }

    /**
     * Dibuja líneas de grilla isométricas extendidas suavemente (1 casilla de margen)
     * para enmarcar el tablero sin colisionar con el tablero adyacente al redimensionar.
     */
    private void drawExtendedIsometricGrid(Pane targetPane, double ox, double oy) {
        int N = Tablero.TAMANO;
        int margin = 1;

        double hw = tileWidth / 2.0;
        double hh = tileHeight / 2.0;
        Point2D boardCenter = gridToScreen(4, 4, ox, oy);
        double maxDist = Math.hypot(hw * (N + margin), hh * (N + margin));

        for (int row = -margin; row < N + margin; row++) {
            for (int col = -margin; col < N + margin; col++) {
                if (row >= 0 && row < N && col >= 0 && col < N) continue;

                Point2D center = gridToScreen(row, col, ox, oy);
                double dist = center.distance(boardCenter);
                
                double opacity = 0.22 * Math.max(0.0, 1.0 - (dist / maxDist));
                if (opacity <= 0.02) continue;

                Polygon extTile = new Polygon(
                    center.getX(),      center.getY() - hh,
                    center.getX() + hw, center.getY(),
                    center.getX(),      center.getY() + hh,
                    center.getX() - hw, center.getY()
                );
                extTile.setFill(Color.TRANSPARENT);
                extTile.setStroke(Color.rgb(35, 146, 184, opacity));
                extTile.setStrokeWidth(0.8);
                extTile.setMouseTransparent(true);

                targetPane.getChildren().add(extTile);
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DEL TABLERO ISOMÉTRICO ENEMIGO
    // ══════════════════════════════════════════════════════

    /**
     * Construye el tablero enemigo como diamantes isométricos
     * superpuestos al StackPane, pintados según el estado del modelo.
     * Orden de dibujado: back-to-front (mayor row+col al frente).
     */
    private void buildIsometricBoard() {
        if (boardStack == null) return;
        if (isoTilesPane == null) {
            isoTilesPane = new Pane();
            isoTilesPane.setMouseTransparent(true);
            boardStack.getChildren().add(isoTilesPane);
        }
        buildGenericIsometricBoard(
            isoTilesPane, 
            machineBoard, 
            isoTiles, 
            originX, 
            originY, 
            false);

        if (highlight != null && !isoTilesPane.getChildren().contains(highlight)) {
            isoTilesPane.getChildren().add(highlight);
        }
    }

    private void renderShipImages() {
        if (playerBoardStack == null) return;
        if (shipImagesPane == null) {
            shipImagesPane = new Pane();
            shipImagesPane.setMouseTransparent(true);
            playerBoardStack.getChildren().add(shipImagesPane);
        }
        shipImagesPane.getChildren().clear();
        shipImageMap.clear();

        if (playerBoard == null || playerBoard.getFlota() == null) return;

        for (Barco barco : playerBoard.getFlota().getBarcos()) {
            List<Casilla> casillas = barco.getCasillas();
            if (casillas.isEmpty()) continue;

            int n = casillas.size();

            // ── Centro geometrico de las casillas en pantalla ──
            double cx = 0, cy = 0;
            for (Casilla c : casillas) {
                Point2D p = gridToScreen(c.getFila(), c.getColumna(), playerOriginX, playerOriginY);
                cx += p.getX();
                cy += p.getY();
            }
            cx /= n;
            cy /= n;

            // ── Determinar orientacion a partir de las casillas ──
            boolean isHorizontal = n > 1
                    ? (casillas.get(0).getFila() == casillas.get(1).getFila())
                    : true;
            
            Image image = imageCache.computeIfAbsent(barco.getTipo(), tipo -> {
                String path = switch (tipo) {
                    case PORTAAVIONES -> "/barcosimg/Portaavion-8.png";
                    case SUBMARINO   -> "/barcosimg/Submarino-8.png";
                    case DESTRUCTOR  -> "/barcosimg/Destructor-8.png";
                    case FRAGATA     -> "/barcosimg/fragata-8.png";
                };
                return new Image(getClass().getResourceAsStream(path));
            });

            ImageView iv = new ImageView(image);

            double tileDiagonal = Math.hypot(tileWidth / 2.0, tileHeight / 2.0);
            double scaleFactor = 0.9; // Ajusta el tamaño de la imagen

            double targetWidth  = (tileDiagonal * n) * scaleFactor;
            iv.setFitWidth(targetWidth);
            iv.setPreserveRatio(true);

            if (!isHorizontal) {
                iv.setScaleY(-1); // Flip horizontal for vertical orientation
                iv.setRotate(-180);
            }

            double calculatedHeight = targetWidth * (image.getHeight() / image.getWidth());

            iv.setLayoutX(cx - targetWidth / 2.0);
            iv.setLayoutY(cy - calculatedHeight / 2.0);

            shipImagesPane.getChildren().add(iv);
            shipImageMap.put(barco, iv);
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
                color = showShips ? "#176b87" : "#0c4258";
                break;
            case AGUA:     color = "#0e4a64"; break;
            case TOCADO:   color = "#c0392b"; break;
            case HUNDIDO:  color = "#641e16"; break;
            default:       color = "#0c4258"; break;
        }
        tile.setFill(Color.web(color));
    }

    /** Atajo para tablero enemigo — oculta barcos. */
    private void applyIsoColor(Polygon tile, Casilla casilla) {
        applyIsoColor(tile, casilla, false);
    }

    /** Caché de imágenes de estado de combate (Fuego, Bomba, Boom, Agua). */
    private final Map<String, Image> attackImageCache = new java.util.HashMap<>();

    private Image getAttackImage(String resourcePath) {
        return attackImageCache.computeIfAbsent(resourcePath, path -> {
            try {
                var is = getClass().getResourceAsStream(path);
                return is != null ? new Image(is) : null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Superpone la imagen táctica correspondiente según el estado de la casilla y el tablero:
     * <ul>
     *   <li><b>Tablero del Jugador:</b> Fuego.png (TOCADO), Boom.png (HUNDIDO), Agua.png (AGUA).</li>
     *   <li><b>Tablero Enemigo:</b> Bomba.png (TOCADO), Boom.png (HUNDIDO), Agua.png (AGUA).</li>
     * </ul>
     */
    private void applyIsoOverlay(Pane targetPane, Casilla casilla, Point2D center, boolean isPlayerBoard) {
        EstadoCasilla estado = casilla.getEstado();
        Image img = null;
        double scaleFactor = 0.85;

        switch (estado) {
            case AGUA:
                img = getAttackImage("/Agua.png");
                scaleFactor = 1.0;
                break;

            case TOCADO:
                img = isPlayerBoard ? getAttackImage("/Fuego.png") : getAttackImage("/Bomba.png");
                scaleFactor = isPlayerBoard ? 0.85 : 0.75;
                break;

            case HUNDIDO:
                img = getAttackImage("/Boom.png");
                scaleFactor = 0.95;
                break;

            default:
                return;
        }

        if (img != null) {
            ImageView iv = new ImageView(img);
            double w = tileWidth * scaleFactor;
            iv.setFitWidth(w);
            iv.setPreserveRatio(true);
            iv.setMouseTransparent(true);

            double h = w * (img.getHeight() / img.getWidth());
            iv.setLayoutX(center.getX() - w / 2.0);
            iv.setLayoutY(center.getY() - h / 2.0);

            targetPane.getChildren().add(iv);
        }
    }

    // ══════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DEL TABLERO ISOMÉTRICO DEL JUGADOR
    // ══════════════════════════════════════════════════════

    private Pane playerEffectsPane;

    /**
     * Construye el tablero del jugador como diamantes isométricos
     * mostrando los barcos (showShips = true), manteniendo las imágenes de los barcos
     * al frente (shipImagesPane.toFront()) sobre las baldosas e imágenes de impacto inferiores.
     */
    private void buildPlayerIsometricBoard() {
        if (playerBoardStack == null) return;

        if (playerIsoTilesPane == null) {
            playerIsoTilesPane = new Pane();
            playerIsoTilesPane.setMouseTransparent(true);
            playerBoardStack.getChildren().add(playerIsoTilesPane);
        }

        buildGenericIsometricBoard(
            playerIsoTilesPane, 
            playerBoard, 
            playerIsoTiles, 
            playerOriginX, 
            playerOriginY, 
            true);

        if (shipImagesPane != null) {
            shipImagesPane.toFront();
        }
    }

    // ══════════════════════════════════════════════════════
    //  CREACIÓN DE CASILLAS
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
    //  ACTUALIZACIÓN VISUAL DE CASILLAS
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
    //  MANEJADOR DE DISPAROS
    // ══════════════════════════════════════════════════════

    /**
     * Maneja el clic del jugador en el tablero enemigo.
     * Coordina el disparo mediante {@link BatallaNavalFacade#disparaJugador}
     * y cede el turno automáticamente a la máquina en caso de fallo (AGUA).
     */
    private void handleShot(int fila, int columna) {
        if (partidaTerminada) return;

        if (facade != null && !facade.esTurnoJugador()) {
            updateStatus("Es el turno de la máquina... espera.");
            return;
        }

        try {
            EstadoDisparo resultado = (facade != null)
                ? facade.disparaJugador(fila, columna)
                : machineBoard.disparar(fila, columna);

            if (!primerDisparoRealizado) {
                primerDisparoRealizado = true;
                if (btnShowMachine != null) {
                    btnShowMachine.setDisable(true);
                }
            }

            Casilla casilla = machineBoard.getCasillas()[fila][columna];

            // Actualizar diamante isométrico y capas de superposición (Bomba, Boom, Agua)
            if (isoTiles[fila][columna] != null) {
                applyIsoColor(isoTiles[fila][columna], casilla);
            }
            buildIsometricBoard();

            switch (resultado) {
                case AGUA:
                    updateStatus("Agua... ¡Turno de la máquina!");
                    if (lblTurn != null) lblTurn.setText("Turno: Máquina");
                    javafx.application.Platform.runLater(this::ejecutarTurnoMaquina);
                    break;
                case TOCADO:
                    updateStatus("¡Tocado! Impacto en el barco enemigo. ¡Sigues disparando!");
                    break;
                case HUNDIDO:
                    updateStatus("¡Barco enemigo hundido! ¡Sigues disparando!");
                    break;
                case VICTORIA:
                    updateStatus("¡VICTORIA! Has hundido toda la flota enemiga.");
                    if (lblTurn != null) lblTurn.setText("¡GANASTE!");
                    mostrarFinDePartida(true);
                    break;
            }
        } catch (Exception e) {
            updateStatus("Casilla ya disparada — elige otra.");
        }
    }

    /**
     * Ejecuta el turno automático de la máquina seleccionando casillas no disparadas
     * del tablero del jugador mediante {@link BatallaNavalFacade#disparaMaquina}.
     */
    private void ejecutarTurnoMaquina() {
        if (facade == null || facade.juegoTerminado() || facade.esTurnoJugador()) return;

        java.util.Random rand = new java.util.Random();
        int f, c;
        Casilla[][] casillasJugador = playerBoard.getCasillas();

        do {
            f = rand.nextInt(Tablero.TAMANO);
            c = rand.nextInt(Tablero.TAMANO);
        } while (casillasJugador[f][c].getEstado() == EstadoCasilla.AGUA ||
                 casillasJugador[f][c].getEstado() == EstadoCasilla.TOCADO ||
                 casillasJugador[f][c].getEstado() == EstadoCasilla.HUNDIDO);

        EstadoDisparo resultado = facade.disparaMaquina(f, c);
        Casilla casillaDisparada = casillasJugador[f][c];

        // Actualizar diamante isométrico del jugador y superposiciones (Fuego, Boom, Agua)
        if (playerIsoTiles[f][c] != null) {
            applyIsoColor(playerIsoTiles[f][c], casillaDisparada, true);
        }
        buildPlayerIsometricBoard();

        if (resultado == EstadoDisparo.AGUA) {
            updateStatus("La máquina disparó en (" + COLS[c] + (f + 1) + ") y dio en Agua. ¡Es tu turno!");
            if (lblTurn != null) lblTurn.setText("Turno: Jugador");
        } else if (resultado == EstadoDisparo.VICTORIA) {
            updateStatus("DERROTA... La máquina ha hundido toda tu flota.");
            if (lblTurn != null) lblTurn.setText("¡GAME OVER!");
            mostrarFinDePartida(false);
        } else {
            updateStatus("¡La máquina impactó tu barco en (" + COLS[c] + (f + 1) + ")! Vuelve a disparar...");
            javafx.application.Platform.runLater(this::ejecutarTurnoMaquina);
        }
    }
    /**
     * Muestra un diálogo de fin de partida y bloquea el tablero.
     * @param gano true si el jugador ganó, false si perdió.
     */
    private void mostrarFinDePartida(boolean gano) {
        partidaTerminada = true;
        if (boardStack != null) {
            boardStack.setOnMouseClicked(null);
            boardStack.setOnMouseMoved(null);
        }

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Fin de la partida");
        alert.setHeaderText(gano ? "¡Ganaste!" : "Perdiste");
        alert.setContentText(gano
                ? "Hundiste toda la flota enemiga. ¡Buen trabajo, almirante!"
                : "La máquina hundió toda tu flota. ¡Suerte para la próxima!");
        alert.showAndWait();
    }

    // ══════════════════════════════════════════════════════
    //  PANELES INFORMATIVOS DE FLOTA
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
    //  MÉTODOS AUXILIARES
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
    //  1. CONVERSIÓN DE COORDENADAS ISOMÉTRICAS
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
     * customizados compartiendo el mismo plano isométrico continuo.
     */
    private Point2D gridToScreen(int row, int col, double ox, double oy) {
        double sx = ox + (col - row) * (tileWidth  / 2.0);
        double sy = oy + (col + row) * (tileHeight / 2.0);
        return new Point2D(sx, sy);
    }

    /**
     * Convierte coordenadas de pantalla (mouseX, mouseY) a [fila, columna]
     * en el tablero enemigo.
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
    //  2. REJILLA DE DEPURACIÓN DE PROYECIÓN
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
    //  3. INTERACCIÓN Y RESALTADO TÁCTICO DE CASILLAS
    // ══════════════════════════════════════════════════════

    /**
     * Configura los eventos de mouse sobre el StackPane del tablero enemigo
     * para detección isométrica de casillas.
     * Los parámetros de grilla se recalculan automáticamente al resize.
     *
     * @param boardPane el StackPane que contiene al VBox del tablero enemigo
     */
    public void setupIsometricInteraction(StackPane boardPane) {
        if (machineBoardContainer != null) {
            machineBoardContainer.setMouseTransparent(true);
        }

        // Highlight polígono (se mueve con el cursor)
        if (highlight == null) {
            highlight = new Polygon();
            highlight.setMouseTransparent(true);
            highlight.setVisible(false);
        }

        if (isoTilesPane != null && !isoTilesPane.getChildren().contains(highlight)) {
            isoTilesPane.getChildren().add(highlight);
        }

        // ── Mouse Moved: resaltar casilla bajo cursor ──
        boardPane.setOnMouseMoved((MouseEvent e) -> {
            Insets pad = boardPane.getPadding();
            double mx = e.getX() - pad.getLeft();
            double my = e.getY() - pad.getTop();

            int[] cell = screenToGrid(mx, my);

            if (cell == null) {
                highlight.setVisible(false);
                return;
            }

            int row = cell[0];
            int col = cell[1];

            if (isoTiles[row][col] == null) {
                highlight.setVisible(false);
                return;
            }

            // Feedback gráfico dinámico según el estado de la casilla
            Casilla casilla = machineBoard.getCasillas()[row][col];
            if (casilla.getEstado() != EstadoCasilla.VACIA && casilla.getEstado() != EstadoCasilla.OCUPADA) {
                highlight.setFill(Color.rgb(231, 76, 60, 0.25)); // Rojo sutil para casilla ya disparada
                highlight.setStroke(Color.rgb(231, 76, 60, 0.85));
                highlight.setStrokeWidth(2.0);
            } else {
                highlight.setFill(Color.rgb(56, 189, 248, 0.35)); // Azul cian brillante táctico
                highlight.setStroke(Color.rgb(56, 189, 248, 1.0));
                highlight.setStrokeWidth(2.0);
            }

            // Copiar exactamente los 4 vértices del diamante en esa posición
            highlight.getPoints().setAll(isoTiles[row][col].getPoints());
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