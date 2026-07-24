package org.example.batallanaval_fpoe;

import java.io.IOException;
import java.util.Optional;

import org.example.batallanaval_fpoe.controller.PrincipalController;
import org.example.batallanaval_fpoe.model.BatallaNavalFacade;
import org.example.batallanaval_fpoe.model.GameStateManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/**
 * Punto de Entrada Principal (Main Application Entry Point) — Batalla Naval FPOE.
 * <p>
 * Hereda de {@link Application} y gestiona el ciclo de vida de la aplicación JavaFX,
 * coordinando la navegación entre las vistas FXML principales, la solicitud del
 * nickname del jugador y la restauración de partidas guardadas (HU-6).
 * </p>
 *
 * @author Daniel, Nicolás, Robert
 * @version 3.0
 */
public class NavalBattleApp extends Application {

    private static Stage mainStage;
    private static String nicknameActual = "Jugador";

    @Override
    public void start(Stage stage) throws IOException {
        mainStage = stage;

        pedirNickname();

        if (GameStateManager.existePartidaGuardada()) {
            boolean continuar = confirmarContinuarPartida();
            if (continuar) {
                BatallaNavalFacade facadeGuardado = GameStateManager.cargarPartida();
                if (facadeGuardado != null) {
                    showGameScreen(stage, facadeGuardado, false);
                    return;
                }
            } else {
                GameStateManager.borrarPartidaGuardada();
            }
        }

        showPlacementScreen(stage);
    }

    /**
     * Solicita al jugador su nickname mediante un diálogo simple.
     */
    private void pedirNickname() {
        TextInputDialog dialog = new TextInputDialog(GameStateManager.leerNickname());
        dialog.setTitle("Batalla Naval");
        dialog.setHeaderText("Bienvenido, Almirante");
        dialog.setContentText("Ingresa tu nickname:");

        Optional<String> resultado = dialog.showAndWait();
        nicknameActual = resultado.map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse("Jugador");
    }

    /**
     * Pregunta si se desea continuar la última partida guardada o empezar una nueva.
     */
    private boolean confirmarContinuarPartida() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Partida guardada encontrada");
        alert.setHeaderText("Hay una partida en progreso");
        alert.setContentText("¿Deseas continuar donde la dejaste, o empezar una nueva?");

        ButtonType continuar = new ButtonType("Continuar");
        ButtonType nueva = new ButtonType("Nueva partida");
        alert.getButtonTypes().setAll(continuar, nueva);

        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == continuar;
    }

    public void showPlacementScreen(Stage stage) throws IOException {
        mainStage = stage;
        FXMLLoader loader = new FXMLLoader(
                NavalBattleApp.class.getResource("/ship-placement-view.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Batalla Naval — Configura tu flota");
        stage.setScene(scene);
        stage.show();
    }

    public static void restartGame() {
        GameStateManager.borrarPartidaGuardada();
        if (mainStage != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        NavalBattleApp.class.getResource("/ship-placement-view.fxml"));
                Scene scene = new Scene(loader.load());
                mainStage.setTitle("Batalla Naval — Configura tu flota");
                mainStage.setScene(scene);
                mainStage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Transición a la pantalla de combate.
     *
     * @param iniciarNueva true si se debe colocar la flota de la máquina desde cero
     *                      (partida nueva); false si se está restaurando una partida
     *                      guardada (las flotas ya vienen colocadas de antes).
     */
    public void showGameScreen(Stage stage, BatallaNavalFacade facade, boolean iniciarNueva) throws IOException {
        mainStage = stage;
        if (iniciarNueva) {
            facade.iniciarPartida();
        }

        FXMLLoader loader = new FXMLLoader(
                NavalBattleApp.class.getResource("/main-view.fxml"));
        Scene scene = new Scene(loader.load());

        PrincipalController controller = loader.getController();
        controller.setNickname(nicknameActual);
        controller.setFacade(facade);

        stage.setTitle("Batalla Naval - FPOE");
        stage.setScene(scene);
        stage.show();
        stage.setMaximized(true);
    }
}