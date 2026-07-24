package org.example.batallanaval_fpoe;

import java.io.IOException;

import org.example.batallanaval_fpoe.controller.PrincipalController;
import org.example.batallanaval_fpoe.model.BatallaNavalFacade;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de Entrada Principal (Main Application Entry Point) — Batalla Naval FPOE.
 * <p>
 * Hereda de {@link Application} y gestiona el ciclo de vida de la aplicación JavaFX,
 * coordinando la navegación y transición suave entre las vistas FXML principales:
 * <ul>
 *   <li><b>Vista 1:</b> Pantalla de colocación de barcos ({@code ship-placement-view.fxml}).</li>
 *   <li><b>Vista 2:</b> Pantalla principal de combate táctico ({@code main-view.fxml}).</li>
 * </ul>
 * Garantiza que la ventana se despliegue maximizada por defecto y realiza la inyección
 * adecuada de la fachada del modelo {@link BatallaNavalFacade} al cambiar de vista.
 * </p>
 *
 * @author Daniel, Nicolás, Robert
 * @version 2.0
 * @see Application
 * @see BatallaNavalFacade
 * @see PrincipalController
 */
public class NavalBattleApp extends Application {

    private static Stage mainStage;

    /**
     * Inicia la ejecución del ciclo de vida de JavaFX.
     * Carga por defecto la pantalla de colocación e inspección de la flota.
     *
     * @param stage El escenario principal ({@link Stage}) proporcionado por el runtime de JavaFX.
     * @throws IOException Si ocurre un error de lectura durante la carga del recurso FXML.
     */
    @Override
    public void start(Stage stage) throws IOException {
        mainStage = stage;
        showPlacementScreen(stage);
    }

    /**
     * Muestra la pantalla de configuración e inicio de partida (colocación manual/aleatoria de barcos).
     * Configura la escena y maximiza la ventana para una experiencia táctica inmersiva.
     *
     * @param stage El escenario principal donde se presentará la escena de configuración.
     * @throws IOException Si el archivo {@code /ship-placement-view.fxml} no puede ser cargado.
     */
    public void showPlacementScreen(Stage stage) throws IOException {
        mainStage = stage;
        FXMLLoader loader = new FXMLLoader(
            NavalBattleApp.class.getResource("/ship-placement-view.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Batalla Naval — Configura tu flota");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Reinicia la aplicación regresando a la pantalla de alistamiento de flota.
     */
    public static void restartGame() {
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
     * Realiza la transición hacia la pantalla principal de combate.
     * Ejecuta el inicio formal de la partida (colocación automática de la flota enemiga)
     * e inyecta la fachada del modelo {@link BatallaNavalFacade} en el {@link PrincipalController}.
     *
     * @param stage  El escenario principal donde se presentará la escena de combate.
     * @param facade La fachada del modelo pre-configurada con el estado actual del juego.
     * @throws IOException Si el archivo {@code /main-view.fxml} no puede ser cargado.
     */
    public void showGameScreen(Stage stage, BatallaNavalFacade facade) throws IOException {
        mainStage = stage;
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
