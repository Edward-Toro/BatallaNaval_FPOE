package org.example.batallanaval_fpoe.model;

import org.example.batallanaval_fpoe.exception.CasillaYaDisparadaException;
import org.example.batallanaval_fpoe.exception.PosicionInvalidaException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * Tablero de 10x10 con la flota de un jugador y el historial de disparos
 * recibidos. Se usa tanto para el tablero de posición (jugador humano)
 * como, con otra instancia, para el tablero de la máquina.
 *
 * @author Daniel, Nicolas y Robert
 */
public class Tablero {

    public static final int TAMANO = 10;

    private final Casilla[][] casillas;
    private final Flota flota;
    private final Queue<int[]> historialDisparos;
    private final List<DisparoListener> listeners;

    public Tablero() {
        this.casillas = new Casilla[TAMANO][TAMANO];
        for (int f = 0; f < TAMANO; f++) {
            for (int c = 0; c < TAMANO; c++) {
                casillas[f][c] = new Casilla(f, c);
            }
        }
        this.flota = new Flota();
        this.historialDisparos = new LinkedList<>();
        this.listeners = new ArrayList<>();
    }

    public Casilla[][] getCasillas() {
        return casillas;
    }

    public Flota getFlota() {
        return flota;
    }

    public void agregarListener(DisparoListener listener) {
        listeners.add(listener);
    }

    private void notificarDisparo(int fila, int columna, EstadoDisparo resultado) {
        for (DisparoListener listener : listeners) {
            listener.onDisparo(fila, columna, resultado);
        }
    }

    public void colocarBarco(Barco barco, int filaInicio, int columnaInicio, Orientacion orientacion)
            throws PosicionInvalidaException {
        int tamano = barco.getTipo().getTamano();
        int[][] posiciones = new int[tamano][2];

        for (int i = 0; i < tamano; i++) {
            int fila = orientacion == Orientacion.VERTICAL ? filaInicio + i : filaInicio;
            int columna = orientacion == Orientacion.HORIZONTAL ? columnaInicio + i : columnaInicio;

            if (fila < 0 || fila >= TAMANO || columna < 0 || columna >= TAMANO) {
                throw new PosicionInvalidaException(
                        "El barco se sale del tablero en la posición (" + fila + ", " + columna + ")");
            }
            if (casillas[fila][columna].tieneBarco()) {
                throw new PosicionInvalidaException(
                        "Ya hay un barco ubicado en (" + fila + ", " + columna + ")");
            }
            posiciones[i][0] = fila;
            posiciones[i][1] = columna;
        }

        for (int[] pos : posiciones) {
            Casilla casilla = casillas[pos[0]][pos[1]];
            casilla.setBarco(barco);
            barco.agregarCasilla(casilla);
        }
    }

    public void colocarFlotaAleatoria() {
        Random random = new Random();

        for (Barco barco : flota.getBarcos()) {
            boolean colocado = false;

            while (!colocado) {
                int fila = random.nextInt(TAMANO);
                int columna = random.nextInt(TAMANO);
                Orientacion orientacion = random.nextBoolean() ? Orientacion.HORIZONTAL : Orientacion.VERTICAL;

                try {
                    colocarBarco(barco, fila, columna, orientacion);
                    colocado = true;
                } catch (PosicionInvalidaException e) {
                    // posición inválida, se reintenta con otra al azar
                }
            }
        }
    }

    public EstadoDisparo disparar(int fila, int columna) {
        Casilla casilla = casillas[fila][columna];

        if (casilla.getEstado() == EstadoCasilla.AGUA
                || casilla.getEstado() == EstadoCasilla.TOCADO
                || casilla.getEstado() == EstadoCasilla.HUNDIDO) {
            throw new CasillaYaDisparadaException(
                    "La casilla (" + fila + ", " + columna + ") ya fue disparada antes");
        }

        historialDisparos.add(new int[]{fila, columna});

        EstadoDisparo resultado;

        if (!casilla.tieneBarco()) {
            casilla.setEstado(EstadoCasilla.AGUA);
            resultado = EstadoDisparo.AGUA;
        } else {
            Barco barco = casilla.getBarco();
            boolean hundido = barco.recibirImpacto();

            if (hundido) {
                for (Casilla c : barco.getCasillas()) {
                    c.setEstado(EstadoCasilla.HUNDIDO);
                }
                resultado = flota.estaCompletamenteHundida() ? EstadoDisparo.VICTORIA : EstadoDisparo.HUNDIDO;
            } else {
                casilla.setEstado(EstadoCasilla.TOCADO);
                resultado = EstadoDisparo.TOCADO;
            }
        }

        notificarDisparo(fila, columna, resultado);
        return resultado;
    }

    public Queue<int[]> getHistorialDisparos() {
        return historialDisparos;
    }
}