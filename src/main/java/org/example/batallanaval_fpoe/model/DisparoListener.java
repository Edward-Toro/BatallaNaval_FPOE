package org.example.batallanaval_fpoe.model;

/**
 * Patrón de diseño de COMPORTAMIENTO (Observer). Quien implemente esta
 * interfaz puede "suscribirse" al Tablero para enterarse de cada disparo
 * sin que el Tablero necesite saber nada de la interfaz gráfica.
 *
 * @author Daniel, Nicolas y Robert
 */
public interface DisparoListener {
    void onDisparo(int fila, int columna, EstadoDisparo resultado);
}