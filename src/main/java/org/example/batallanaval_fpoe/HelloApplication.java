package org.example.batallanaval_fpoe;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

/**
 * Punto de entrada de la aplicación JavaFX. Por ahora solo abre una
 * ventana vacía para validar que el esqueleto del proyecto compila
 * y ejecuta correctamente.
 *
 * @author Daniel, Nicolás, Robert
 */
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new StackPane(), 800, 600);
        stage.setTitle("Batalla Naval - FPOE");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}