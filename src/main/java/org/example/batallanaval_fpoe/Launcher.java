package org.example.batallanaval_fpoe;

/**
 * Clase de arranque auxiliar. Se usa como main class en el jar/ejecutable
 * para evitar los problemas de classpath que JavaFX presenta al lanzar
 * directamente una clase que extiende Application desde un jar modular.
 *
 * @author Daniel, Nicolás, Rober
 */
public class Launcher {
    public static void main(String[] args) {
        HelloApplication.main(args);
    }
}