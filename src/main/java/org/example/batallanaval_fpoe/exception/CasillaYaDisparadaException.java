package org.example.batallanaval_fpoe.exception;

/**
 * Excepción no marcada (unchecked): se lanza al intentar disparar
 * sobre una casilla que ya había sido disparada antes.
 * Es unchecked porque representa un error de lógica del programa
 * (la UI no debería permitir seleccionar una casilla ya usada).
 *
 * @author Daniel
 */
public class CasillaYaDisparadaException extends RuntimeException {
    public CasillaYaDisparadaException(String message) {
        super(message);
    }
}