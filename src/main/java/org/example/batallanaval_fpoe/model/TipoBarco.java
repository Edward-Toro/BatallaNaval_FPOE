package org.example.batallanaval_fpoe.model;

/**
 * Tipos de barco disponibles, con su tamaño (casillas que ocupa)
 * y la cantidad de unidades de ese tipo por flota.
 *
 * @author Daniel
 */
public enum TipoBarco {
    PORTAAVIONES(4, 1),
    SUBMARINO(3, 2),
    DESTRUCTOR(2, 3),
    FRAGATA(1, 4);

    private final int tamano;
    private final int cantidadPorFlota;

    TipoBarco(int tamano, int cantidadPorFlota) {
        this.tamano = tamano;
        this.cantidadPorFlota = cantidadPorFlota;
    }

    public int getTamano() {
        return tamano;
    }

    public int getCantidadPorFlota() {
        return cantidadPorFlota;
    }
}