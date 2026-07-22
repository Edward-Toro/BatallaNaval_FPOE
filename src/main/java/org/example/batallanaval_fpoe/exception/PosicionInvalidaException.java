package org.example.batallanaval_fpoe.exception;

/**
 * Excepción marcada (checked): se lanza al intentar colocar un barco
 * en una posición que se sale del tablero o se superpone con otro barco.
 * Es checked porque colocar un barco es una operación que el llamador
 * debe manejar explícitamente (puede reintentar con otra posición).
 *
 * @author Daniel
 */
public class PosicionInvalidaException extends Exception {
    public PosicionInvalidaException(String message) {
        super(message);
    }
}