package org.example.batallanaval_fpoe.model;

/**
 * Una casilla individual del tablero 10x10.
 *
 * @author Daniel
 */
public class Casilla implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private final int fila;
    private final int columna;
    private EstadoCasilla estado;
    private Barco barco; // null si no hay barco en esta casilla

    public Casilla(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
        this.estado = EstadoCasilla.VACIA;
        this.barco = null;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public EstadoCasilla getEstado() {
        return estado;
    }

    public void setEstado(EstadoCasilla estado) {
        this.estado = estado;
    }

    public Barco getBarco() {
        return barco;
    }

    public void setBarco(Barco barco) {
        this.barco = barco;
        this.estado = EstadoCasilla.OCUPADA;
    }

    public boolean tieneBarco() {
        return barco != null;
    }
}