package org.example.batallanaval_fpoe.model;

import org.example.batallanaval_fpoe.exception.PosicionInvalidaException;

/**
 * Patrón de diseño ESTRUCTURAL (Facade). Simplifica el manejo de los
 * dos tableros (jugador y máquina) detrás de una interfaz sencilla,
 * para que el controller no tenga que coordinar directamente Tablero,
 * Flota y turnos por su cuenta.
 *
 * @author Daniel, Nicolas y Robert
 */
public class BatallaNavalFacade {

    private final Tablero tableroJugador;
    private final Tablero tableroMaquina;
    private boolean turnoJugador;

    public BatallaNavalFacade() {
        this.tableroJugador = new Tablero();
        this.tableroMaquina = new Tablero();
        this.turnoJugador = true;
    }

    public void iniciarPartida() {

        tableroJugador.reiniciar();
        tableroMaquina.reiniciar();

        tableroJugador.colocarFlotaAleatoria();
        tableroMaquina.colocarFlotaAleatoria();

        turnoJugador = true;
    }

    public void colocarBarcoJugador(Barco barco, int fila, int columna, Orientacion orientacion)
            throws PosicionInvalidaException {
        tableroJugador.colocarBarco(barco, fila, columna, orientacion);
    }

    public EstadoDisparo disparaJugador(int fila, int columna) {

        EstadoDisparo resultado = tableroMaquina.disparar(fila, columna);

        if (resultado == EstadoDisparo.AGUA) {
            turnoJugador = false;
        } else {
            turnoJugador = true;
        }

        return resultado;
    }

    public EstadoDisparo disparaMaquina(int fila, int columna) {

        EstadoDisparo resultado = tableroJugador.disparar(fila, columna);

        if (resultado == EstadoDisparo.AGUA) {
            turnoJugador = true;
        } else {
            turnoJugador = false;
        }

        return resultado;
    }

    public boolean esTurnoJugador() {
        return turnoJugador;
    }

    public Tablero getTableroJugador() {
        return tableroJugador;
    }

    public Tablero getTableroMaquina() {
        return tableroMaquina;
    }

    public boolean juegoTerminado() {
        return tableroJugador.getFlota().estaCompletamenteHundida()
                || tableroMaquina.getFlota().estaCompletamenteHundida();
    }
}