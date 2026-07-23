package org.example.batallanaval_fpoe;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.io.IOException;

/**
 * Punto de entrada de la aplicación JavaFX. Por ahora solo abre una
 * ventana vacía para validar que el esqueleto del proyecto compila
 * y ejecuta correctamente.
 *
 * @author Daniel, Nicolás, Robert
 */
public class NavalBattleApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
            NavalBattleApp.class.getResource("/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Batalla Naval - FPOE");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

}