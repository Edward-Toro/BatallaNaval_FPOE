package org.example.batallanaval_fpoe.model;

/**
 * Resultado posible de un disparo sobre el tablero.
 * Nicolás y Robert: este es el valor que les interesa para
 * pintar la casilla y para saber si el turno pasa o sigue.
 *
 * @author Daniel
 */
public enum EstadoDisparo {
    AGUA,
    TOCADO,
    HUNDIDO,
    VICTORIA // se dispara y además se hunde el último barco de la flota
}