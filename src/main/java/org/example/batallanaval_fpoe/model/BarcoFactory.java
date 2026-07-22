package org.example.batallanaval_fpoe.model;

/**
 * Factory encargada de crear instancias de {@link Barco} según su tipo.
 * Patrón de diseño CREACIONAL (Factory Method): centraliza la creación
 * de barcos para que el resto del código no dependa de "new Barco(...)"
 * directamente.
 *
 * @author Daniel
 */
public class BarcoFactory {

    public static Barco crear(TipoBarco tipo) {
        return new Barco(tipo);
    }
}