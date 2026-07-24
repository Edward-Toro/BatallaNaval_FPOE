package org.example.batallanaval_fpoe;

import java.io.IOException;

import org.example.batallanaval_fpoe.controller.PrincipalController;
import org.example.batallanaval_fpoe.model.BatallaNavalFacade;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación JavaFX.
 * Carga primero la pantalla de colocación de barcos,
 * y al iniciar partida navega a la pantalla de juego.
 *
 * @author Daniel, Nicolás, Robert
 */
public class NavalBattleApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        showPlacementScreen(stage);
    }

    /**
     * Muestra la pantalla de configuración (colocación de barcos).
     */
    public void showPlacementScreen(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            NavalBattleApp.class.getResource("/ship-placement-view.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Batalla Naval — Configura tu flota");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Navega a la pantalla de juego con el facade ya configurado
     * (el tablero del jugador tiene la flota colocada manualmente).
     */
    public void showGameScreen(Stage stage, BatallaNavalFacade facade) throws IOException {
        // Colocar flota de la máquina
        facade.iniciarPartida();

        FXMLLoader loader = new FXMLLoader(
            NavalBattleApp.class.getResource("/main-view.fxml"));
        Scene scene = new Scene(loader.load());

        // Inyectar el facade en el controller después de que initialize() corra
        PrincipalController controller = loader.getController();
        controller.setFacade(facade);

        stage.setTitle("Batalla Naval - FPOE");
        stage.setScene(scene);
        stage.show();
        stage.setMaximized(true);
    }
}
