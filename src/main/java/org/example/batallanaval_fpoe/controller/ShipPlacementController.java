package org.example.batallanaval_fpoe.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;

import org.example.batallanaval_fpoe.NavalBattleApp;
import org.example.batallanaval_fpoe.model.Barco;
import org.example.batallanaval_fpoe.model.BatallaNavalFacade;
import org.example.batallanaval_fpoe.model.Casilla;
import org.example.batallanaval_fpoe.model.EstadoCasilla;
import org.example.batallanaval_fpoe.model.Orientacion;
import org.example.batallanaval_fpoe.exception.PosicionInvalidaException;
import org.example.batallanaval_fpoe.model.Tablero;
import org.example.batallanaval_fpoe.model.TipoBarco;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de la Vista de Configuración (Colocación de Flota) — Batalla Naval FPOE.
 * <p>
 * Gestiona la fase de alistamiento de la flota del jugador antes del combate.
 * Soporta posicionamiento manual interactivo mediante Drag &amp; Drop y clicks,
 * rotación dinámica de orientación (Horizontal / Vertical), auto-ubicación aleatoria
 * delegada al modelo y validación en tiempo real de restricciones de espacio.
 * </p>
 * <p>
 * <b>Patrones y Principios POE Implementados:</b>
 * <ul>
 *   <li><b>Drag and Drop JavaFX:</b> Inicia eventos de arrastre desde la paleta de barcos ({@code Dragboard})
 *   hacia la cuadrícula isométrica 2.5D con pre-visualización táctica de validez.</li>
 *   <li><b>Integridad del Modelo:</b> Llama exclusivamente a métodos del modelo {@link Tablero#colocarBarco}
 *   y captura excepciones de dominio como {@link PosicionInvalidaException}.</li>
 *   <li><b>Renderizado Isométrico Dinámico:</b> Mantiene el plano 2.5D ajustado al contenedor con
 *   grilla extendida de fondo y cálculo responsivo al redimensionar la ventana.</li>
 * </ul>
 * </p>
 *
 * @author Daniel, Nicolás, Robert
 * @version 2.0
 * @see BatallaNavalFacade
 * @see Tablero
 * @see Barco
 * @see Orientacion
 */
public class ShipPlacementController {

    // ── Inyecciones FXML ──────────────────────────────────
    @FXML private StackPane boardStack;
    @FXML private VBox shipPalette;
    @FXML private Label lblOrientation;
    @FXML private Label lblProgress;
    @FXML private Button btnStart;
    @FXML private Button btnAutoPlace;
    @FXML private Button btnClear;
    @FXML private Label lblStatus;

    // ── Modelo de Dominio ──────────────────────────────────
    private BatallaNavalFacade facade;
    private Tablero playerBoard;
    private Orientacion currentOrientation = Orientacion.HORIZONTAL;
    private int shipsPlaced = 0;
    private static final int TOTAL_SHIPS = 10;

    // ── Grilla e Información Isométrica ───────────────────
    private double tileWidth  = 48.0;
    private double tileHeight = 24.0;
    private double originX    = 300.0;
    private double originY    = 40.0;
    private Pane isoTilesPane;
    private Pane previewPane;
    private final Polygon[][] isoTiles = new Polygon[Tablero.TAMANO][Tablero.TAMANO];
    private Polygon highlight;

    // ── Losas de Pre-visualización (tamaño máximo = 4) ─────
    private static final int MAX_SHIP_SIZE = 4;
    private final Polygon[] previewTiles = new Polygon[MAX_SHIP_SIZE];

    // ── Capa de Imágenes y Sprites de Barcos ──────────────
    private Pane shipImagesPane;
    private final Map<Barco, ImageView> shipImageMap = new java.util.HashMap<>();
    private final Map<TipoBarco, Image> imageCache = new EnumMap<>(TipoBarco.class);

    // ── Arrastre y Soltado (Drag & Drop) ──────────────────
    private static final DataFormat SHIP_DATA =
            new DataFormat("application/x-batallanaval-ship");
    private TipoBarco draggingType;
    private Barco draggingShip;

    // ── Tarjetas de la Paleta Lateral ──────────────────────
    private final Map<TipoBarco, VBox> paletteCards = new EnumMap<>(TipoBarco.class);

    // ══════════════════════════════════════════════════════
    //  INICIALIZACIÓN
    // ══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        facade = new BatallaNavalFacade();
        playerBoard = facade.getTableroJugador();

        buildIsometricBoard();
        buildShipImagesPane();
        buildPreviewPane();
        buildShipPalette();
        setupDragAndDrop();
        setupKeyBindings();
        setupResponsiveGrid();

        updateProgress();
        btnStart.setDisable(true);
    }

    // ══════════════════════════════════════════════════════
    //  CONSTRUCCIÓN DE LA GRILLA ISOMÉTRICA
    // ══════════════════════════════════════════════════════

    /**
     * Construye el tablero isometrico del jugador sobre el StackPane.
     * Cada diamante representa una casilla vacia o con barco.
     */
    private void buildIsometricBoard() {
        if (isoTilesPane == null) {
            isoTilesPane = new Pane();
            isoTilesPane.setMouseTransparent(true);
            boardStack.getChildren().add(1, isoTilesPane);
        }
        isoTilesPane.getChildren().clear();

        int N = Tablero.TAMANO;
        double hw = tileWidth  / 2.0;
        double hh = tileHeight / 2.0;

        // Dibujar grilla extendida en el fondo de la pantalla de colocación
        drawExtendedIsometricGrid();

        for (int sum = 0; sum <= 2 * (N - 1); sum++) {
            for (int row = Math.max(0, sum - N + 1); row <= Math.min(sum, N - 1); row++) {
                int col = sum - row;
                Point2D center = gridToScreen(row, col);

                Polygon tile = new Polygon(
                    center.getX(),      center.getY() - hh,
                    center.getX() + hw, center.getY(),
                    center.getX(),      center.getY() + hh,
                    center.getX() - hw, center.getY()
                );

                Casilla casilla = playerBoard.getCasillas()[row][col];
                applyIsoColor(tile, casilla);

                tile.setStroke(Color.web("#2392b8"));
                tile.setStrokeWidth(1.0);

                isoTiles[row][col] = tile;
                isoTilesPane.getChildren().add(tile);
            }
        }
        drawIsometricHeaders();
    }

    /**
     * Dibuja líneas de grilla isométricas extendidas alrededor del tablero del jugador.
     * Proyecta la rejilla 5 casillas más allá de las aristas y aplica una degradación de
     * opacidad por distancia (efecto plano oceánico difuminado).
     */
    private void drawExtendedIsometricGrid() {
        int N = Tablero.TAMANO;
        int margin = 1;

        double hw = tileWidth / 2.0;
        double hh = tileHeight / 2.0;
        Point2D boardCenter = gridToScreen(4, 4);
        double maxDist = Math.hypot(hw * (N + margin), hh * (N + margin));

        for (int row = -margin; row < N + margin; row++) {
            for (int col = -margin; col < N + margin; col++) {
                if (row >= 0 && row < N && col >= 0 && col < N) continue;

                Point2D center = gridToScreen(row, col);
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

                isoTilesPane.getChildren().add(extTile);
            }
        }
    }

    private static final String[] COLS = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    /**
     * Dibuja las etiquetas numéricas de fila (1-10) y alfabéticas de columna (A-J)
     * a lo largo de las aristas superiores de la proyección isométrica.
     */
    private void drawIsometricHeaders() {
        double hw = tileWidth / 2.0;
        double hh = tileHeight / 2.0;

        for (int col = 0; col < Tablero.TAMANO; col++) {
            Point2D center = gridToScreen(0, col);
            Label label = new Label(COLS[col]);
            label.setStyle("-fx-text-fill: #7dd3fc; -fx-font-size: 11px; -fx-font-weight: bold;");
            label.setMouseTransparent(true);
            label.setLayoutX(center.getX() + hw / 2.0 - 4.0);
            label.setLayoutY(center.getY() - hh - 16.0);
            isoTilesPane.getChildren().add(label);
        }

        for (int row = 0; row < Tablero.TAMANO; row++) {
            Point2D center = gridToScreen(row, 0);
            Label label = new Label(String.valueOf(row + 1));
            label.setStyle("-fx-text-fill: #7dd3fc; -fx-font-size: 11px; -fx-font-weight: bold;");
            label.setMouseTransparent(true);
            label.setLayoutX(center.getX() - hw / 2.0 - 8.0);
            label.setLayoutY(center.getY() - hh - 16.0);
            isoTilesPane.getChildren().add(label);
        }
    }

    /**
     * Crea el pane de preview y los poligonos de previsualizacion
     * (verde = valido, rojo = invalido) durante el drag.
     */
    private void buildPreviewPane() {
        if (previewPane == null) {
            previewPane = new Pane();
            previewPane.setMouseTransparent(true);
            boardStack.getChildren().add(2, previewPane);
        }
        previewPane.getChildren().clear();

        for (int i = 0; i < MAX_SHIP_SIZE; i++) {
            Polygon p = new Polygon();
            p.setFill(Color.rgb(46, 204, 113, 0.35));
            p.setStroke(Color.rgb(46, 204, 113, 0.8));
            p.setStrokeWidth(1.5);
            p.setVisible(false);
            previewTiles[i] = p;
            previewPane.getChildren().add(p);
        }
    }

    /**
     * Crea el pane de overlay para imagenes de barcos colocados.
     * Se situa entre los tiles y el preview para que las imagenes
     * se vean encima del tablero pero debajo del preview de arrastre.
     */
    private void buildShipImagesPane() {
        if (shipImagesPane == null) {
            shipImagesPane = new Pane();
            shipImagesPane.setMouseTransparent(true);
            boardStack.getChildren().add(2, shipImagesPane);
        }
        shipImagesPane.getChildren().clear();
        shipImageMap.clear();
    }

    /**
     * Superpone imagenes de barcos sobre las casillas del tablero isometrico.
     * Cada imagen se posiciona en el centro de las casillas que ocupa el barco,
     * con la rotacion adecuada segun la orientacion (isometrica).
     */
    private void renderShipImages() {
        shipImagesPane.getChildren().clear();
        shipImageMap.clear();

        for (Barco barco : playerBoard.getFlota().getBarcos()) {
            List<Casilla> casillas = barco.getCasillas();
            if (casillas.isEmpty()) continue;

            int n = casillas.size();

            // ── Centro geometrico de las casillas en pantalla ──
            double cx = 0, cy = 0;
            for (Casilla c : casillas) {
                Point2D p = gridToScreen(c.getFila(), c.getColumna());
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
            double scaleFactor = 0.9; // Ajusta el tamaño de la imagen para

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
     * Aplica color al diamante segun el estado de la casilla.
     */
    private void applyIsoColor(Polygon tile, Casilla casilla) {
        String color;
        switch (casilla.getEstado()) {
            case OCUPADA:  color = "#176b87"; break;
            case AGUA:     color = "#0e4a64"; break;
            case TOCADO:   color = "#c0392b"; break;
            case HUNDIDO:  color = "#641e16"; break;
            default:       color = "#0c4258"; break;
        }
        tile.setFill(Color.web(color));
    }

    // ══════════════════════════════════════════════════════
    //  PROYECCIÓN Y CONVERSIÓN ISOMÉTRICA
    // ══════════════════════════════════════════════════════

    public Point2D gridToScreen(int row, int col) {
        double sx = originX + (col - row) * (tileWidth  / 2.0);
        double sy = originY + (col + row) * (tileHeight / 2.0);
        return new Point2D(sx, sy);
    }

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
    //  CÁLCULO RESPONSIVO DE GRILLA
    // ══════════════════════════════════════════════════════

    private void setupResponsiveGrid() {
        boardStack.widthProperty().addListener((obs, o, n) -> recalculateGrid());
        boardStack.heightProperty().addListener((obs, o, n) -> recalculateGrid());

        boardStack.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                newS.getRoot().layoutBoundsProperty().addListener((o, ov, nv) -> recalculateGrid());
            }
        });

        Platform.runLater(this::recalculateGrid);
    }

    private void recalculateGrid() {
        Insets pad = boardStack.getPadding();
        double vw = boardStack.getWidth()  - pad.getLeft() - pad.getRight();
        double vh = boardStack.getHeight() - pad.getTop()  - pad.getBottom();
        if (vw <= 0 || vh <= 0) return;

        int N = Tablero.TAMANO;
        double gridW = (N - 1);
        double gridH = (N - 1) / 2.0;

        double escalaW = vw / (gridW + 1);
        double escalaH = vh / (gridH + 1);
        double escala  = Math.min(escalaW, escalaH);

        tileWidth  = escala;
        tileHeight = escala / 2.0;

        originX = vw / 2.0;
        originY = (vh - (N - 1) * tileHeight) / 2.0;

        buildIsometricBoard();
        buildPreviewPane();
        renderShipImages();
    }

    // ══════════════════════════════════════════════════════
    //  PALETA DE BARCOS Y TARJETAS
    // ══════════════════════════════════════════════════════

    /**
     * Construye la paleta lateral con las tarjetas de cada tipo de barco.
     * Cada tarjeta es draggeable y muestra cuantas unidades restan.
     */
    private void buildShipPalette() {
        shipPalette.getChildren().clear();
        paletteCards.clear();

        for (TipoBarco tipo : TipoBarco.values()) {
            VBox card = createShipCard(tipo);
            paletteCards.put(tipo, card);
            shipPalette.getChildren().add(card);
        }
    }

    private VBox createShipCard(TipoBarco tipo) {
        VBox card = new VBox(4);
        card.getStyleClass().add("ship-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 10, 8, 10));

        // Nombre + tamano
        Label name = new Label(tipo.name() + "  [" + tipo.getTamano() + "]");
        name.getStyleClass().add("ship-card-name");

        // Imagen del barco
        String imgFile = switch (tipo) {
            case PORTAAVIONES -> "/barcosimg/Portaavion-8.png";
            case SUBMARINO    -> "/barcosimg/Submarino-8.png";
            case DESTRUCTOR   -> "/barcosimg/Destructor-8.png";
            case FRAGATA      -> "/barcosimg/fragata-8.png";
        };
        ImageView shipImg = new ImageView(new Image(getClass().getResourceAsStream(imgFile)));
        shipImg.setPreserveRatio(true);
        shipImg.setFitHeight(40);
        shipImg.setFitWidth(160);
        shipImg.setSmooth(true);

        // Conteo restante
        int total = tipo.getCantidadPorFlota();
        Label count = new Label("\u00D7" + total + " restante" + (total > 1 ? "s" : ""));
        count.getStyleClass().add("ship-card-count");

        card.getChildren().addAll(name, shipImg, count);

        // ── Drag source ──
        card.setOnDragDetected((MouseEvent e) -> {
            Barco ship = findUnplacedShip(tipo);
            if (ship == null) return;

            draggingShip = ship;
            draggingType = tipo;

            Dragboard db = card.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.put(SHIP_DATA, tipo.name());
            db.setContent(content);

            card.setOpacity(0.4);
            e.consume();
        });

        card.setOnDragDone((DragEvent e) -> {
            card.setOpacity(1.0);
            draggingType = null;
            draggingShip = null;
            clearPreview();
            e.consume();
        });

        return card;
    }

    /**
     * Actualiza la apariencia de las tarjetas de la paleta
     * segun cuantos barcos de cada tipo quedan por colocar.
     */
    private void updatePalette() {
        for (TipoBarco tipo : TipoBarco.values()) {
            int remaining = countRemaining(tipo);
            VBox card = paletteCards.get(tipo);
            if (card == null) continue;

            // Actualizar label de conteo
            Label countLabel = (Label) card.getChildren().get(2);
            countLabel.setText("\u00D7" + remaining + " restante" + (remaining != 1 ? "s" : ""));

            // Dimmear si no quedan
            if (remaining == 0) {
                card.setOpacity(0.3);
                card.setDisable(true);
                if (!card.getStyleClass().contains("ship-card-placed")) {
                    card.getStyleClass().add("ship-card-placed");
                }
            } else {
                card.setOpacity(1.0);
                card.setDisable(false);
                card.getStyleClass().remove("ship-card-placed");
            }
        }
    }

    private Barco findUnplacedShip(TipoBarco tipo) {
        for (Barco barco : playerBoard.getFlota().getBarcos()) {
            if (barco.getTipo() == tipo && barco.getCasillas().isEmpty()) {
                return barco;
            }
        }
        return null;
    }

    private int countRemaining(TipoBarco tipo) {
        int count = 0;
        for (Barco barco : playerBoard.getFlota().getBarcos()) {
            if (barco.getTipo() == tipo && barco.getCasillas().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    // ══════════════════════════════════════════════════════
    //  DRAG & DROP
    // ══════════════════════════════════════════════════════

    private void setupDragAndDrop() {
        Insets pad = boardStack.getPadding();

        // ── Drag over: aceptar y mostrar preview ──
        boardStack.setOnDragOver((javafx.scene.input.DragEvent e) -> {
            if (e.getGestureSource() != boardStack && e.getDragboard().hasContent(SHIP_DATA)) {
                e.acceptTransferModes(TransferMode.COPY);

                double mx = e.getX() - pad.getLeft();
                double my = e.getY() - pad.getTop();
                showPreview(mx, my);
            }
            e.consume();
        });

        // ── Drag entered ──
        boardStack.setOnDragEntered((javafx.scene.input.DragEvent e) -> {
            if (e.getGestureSource() != boardStack && e.getDragboard().hasContent(SHIP_DATA)) {
                boardStack.setOpacity(0.92);
            }
            e.consume();
        });

        // ── Drag exited: limpiar preview ──
        boardStack.setOnDragExited((javafx.scene.input.DragEvent e) -> {
            boardStack.setOpacity(1.0);
            clearPreview();
            e.consume();
        });

        // ── Drag dropped: colocar barco ──
        boardStack.setOnDragDropped((javafx.scene.input.DragEvent e) -> {
            boolean success = false;

            if (draggingShip != null && draggingType != null) {
                double mx = e.getX() - pad.getLeft();
                double my = e.getY() - pad.getTop();
                int[] cell = screenToGrid(mx, my);

                if (cell != null && isValidPlacement(cell[0], cell[1],
                        currentOrientation, draggingType.getTamano())) {
                    try {
                        playerBoard.colocarBarco(draggingShip, cell[0], cell[1], currentOrientation);
                        shipsPlaced++;
                        rebuildIsoBoard();
                        updatePalette();
                        updateProgress();
                        updateStatus("Barco colocado en (" + cell[0] + ", " + cell[1] + ")");
                        success = true;
                    } catch (PosicionInvalidaException ex) {
                        updateStatus("Error: " + ex.getMessage());
                    }
                } else {
                    updateStatus("Posicion invalida — intenta en otra casilla");
                }
            }

            e.setDropCompleted(success);
            clearPreview();
            e.consume();
        });

        // ── Right-click: quitar barco ──
        boardStack.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                double mx = e.getX() - pad.getLeft();
                double my = e.getY() - pad.getTop();
                int[] cell = screenToGrid(mx, my);

                if (cell != null) {
                    Casilla casilla = playerBoard.getCasillas()[cell[0]][cell[1]];
                    if (casilla.tieneBarco()) {
                        removeBarcoFromTablero(casilla.getBarco());
                        rebuildIsoBoard();
                        updatePalette();
                        updateProgress();
                        updateStatus("Barco removido — puedes reposicionarlo");
                    }
                }
                e.consume();
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  PREVIEW
    // ══════════════════════════════════════════════════════

    /**
     * Muestra la previsualizacion de colocacion durante el drag.
     * Verde si la posicion es valida, rojo si no.
     */
    private void showPreview(double mouseX, double mouseY) {
        if (draggingType == null) {
            clearPreview();
            return;
        }

        int[] cell = screenToGrid(mouseX, mouseY);
        if (cell == null) {
            clearPreview();
            return;
        }

        int startRow = cell[0];
        int startCol = cell[1];
        int size = draggingType.getTamano();
        boolean valid = isValidPlacement(startRow, startCol, currentOrientation, size);

        String fillColor   = valid ? "rgba(46,204,113,0.35)"  : "rgba(231,76,60,0.35)";
        String strokeColor = valid ? "rgba(46,204,113,0.8)"   : "rgba(231,76,60,0.8)";

        double hw = tileWidth  / 2.0;
        double hh = tileHeight / 2.0;

        int[][] cells = getShipCells(startRow, startCol, currentOrientation, size);
        for (int i = 0; i < MAX_SHIP_SIZE; i++) {
            if (i < cells.length) {
                int r = cells[i][0];
                int c = cells[i][1];
                // Ocultar si esta fuera del tablero (aun si la validacion lo permite parcialmente)
                if (r < 0 || r >= Tablero.TAMANO || c < 0 || c >= Tablero.TAMANO) {
                    previewTiles[i].setVisible(false);
                    continue;
                }
                Point2D center = gridToScreen(r, c);
                previewTiles[i].getPoints().setAll(
                    center.getX(),      center.getY() - hh,
                    center.getX() + hw, center.getY(),
                    center.getX(),      center.getY() + hh,
                    center.getX() - hw, center.getY()
                );
                previewTiles[i].setFill(Color.web(fillColor));
                previewTiles[i].setStroke(Color.web(strokeColor));
                previewTiles[i].setVisible(true);
            } else {
                previewTiles[i].setVisible(false);
            }
        }
    }

    private void clearPreview() {
        for (Polygon p : previewTiles) {
            p.setVisible(false);
        }
    }

    // ══════════════════════════════════════════════════════
    //  OPERACIONES DE COLOCACIÓN DE BARCOS
    // ══════════════════════════════════════════════════════

    private boolean isValidPlacement(int startRow, int startCol, Orientacion orient, int size) {
        for (int i = 0; i < size; i++) {
            int row = orient == Orientacion.VERTICAL ? startRow + i : startRow;
            int col = orient == Orientacion.HORIZONTAL ? startCol + i : startCol;

            if (row < 0 || row >= Tablero.TAMANO || col < 0 || col >= Tablero.TAMANO) {
                return false;
            }
            if (playerBoard.getCasillas()[row][col].tieneBarco()) {
                return false;
            }
        }
        return true;
    }

    private int[][] getShipCells(int startRow, int startCol, Orientacion orient, int size) {
        int[][] cells = new int[size][2];
        for (int i = 0; i < size; i++) {
            cells[i][0] = orient == Orientacion.VERTICAL ? startRow + i : startRow;
            cells[i][1] = orient == Orientacion.HORIZONTAL ? startCol + i : startCol;
        }
        return cells;
    }

    /**
     * Remueve un barco del tablero — invierte la operacion de colocarBarco.
     * Prepara la eliminacion sin tocar la interfaz del modelo.
     */
    private void removeBarcoFromTablero(Barco barco) {
        for (Casilla c : barco.getCasillas()) {
            c.setBarco(null);
            c.setEstado(EstadoCasilla.VACIA);
        }
        barco.getCasillas().clear();
        shipsPlaced--;
    }

    private void rebuildIsoBoard() {
        if (isoTilesPane != null) {
            isoTilesPane.getChildren().clear();
        }
        buildIsometricBoard();
        renderShipImages();
    }

    private void updateProgress() {
        lblProgress.setText(shipsPlaced + " / " + TOTAL_SHIPS + " barcos colocados");
        btnStart.setDisable(shipsPlaced < TOTAL_SHIPS);
    }

    // ══════════════════════════════════════════════════════
    //  ACCIONES DE BOTONES E INTERFAZ (FXML)
    // ══════════════════════════════════════════════════════

    @FXML
    private void toggleOrientation(javafx.event.ActionEvent event) {
        currentOrientation = (currentOrientation == Orientacion.HORIZONTAL)
                ? Orientacion.VERTICAL
                : Orientacion.HORIZONTAL;

        String label = (currentOrientation == Orientacion.HORIZONTAL)
                ? "Horizontal" : "Vertical";
        lblOrientation.setText("Orientacion: " + label);
        updateStatus("Orientacion cambiada a " + label);
    }

    @FXML
    private void autoPlace(javafx.event.ActionEvent event) {
        clearBoard(event);
        playerBoard.colocarFlotaAleatoria();
        shipsPlaced = TOTAL_SHIPS;

        rebuildIsoBoard();
        updatePalette();
        updateProgress();
        updateStatus("Flota auto-ubicada — presiona Iniciar Partida");
    }

    @FXML
    private void clearBoard(javafx.event.ActionEvent event) {
        // Recrear el tablero desde cero
        facade = new BatallaNavalFacade();
        playerBoard = facade.getTableroJugador();
        shipsPlaced = 0;

        rebuildIsoBoard();
        updatePalette();
        updateProgress();
        updateStatus("Tablero limpiado — arrastra los barcos de nuevo");
    }

    @FXML
    private void startGame(javafx.event.ActionEvent event) {
        if (shipsPlaced < TOTAL_SHIPS) return;

        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            NavalBattleApp app = new NavalBattleApp();
            app.showGameScreen(stage, facade);
        } catch (IOException e) {
            updateStatus("Error al cargar la pantalla de juego");
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  ACCESOS DIRECTOS DE TECLADO
    // ══════════════════════════════════════════════════════

    private void setupKeyBindings() {
        // Se configura cuando la escena este disponible
        boardStack.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                newS.setOnKeyPressed((KeyEvent e) -> {
                    if (e.getCode() == KeyCode.R) {
                        toggleOrientation(new javafx.event.ActionEvent());
                    }
                });
                // Enfocar para recibir teclas
                newS.getRoot().requestFocus();
            }
        });
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════

    private void updateStatus(String message) {
        lblStatus.setText(message);
    }
}
