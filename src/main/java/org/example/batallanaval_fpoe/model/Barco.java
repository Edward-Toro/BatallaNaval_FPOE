package org.example.batallanaval_fpoe.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un barco de la flota, con sus casillas ocupadas
 * y el conteo de impactos recibidos.
 *
 * @author Daniel
 */
public class Barco {

    private final TipoBarco tipo;
    private final List<Casilla> casillas;
    private int impactos;

    public Barco(TipoBarco tipo) {
        this.tipo = tipo;
        this.casillas = new ArrayList<>();
        this.impactos = 0;
    }

    public TipoBarco getTipo() {
        return tipo;
    }

    public List<Casilla> getCasillas() {
        return casillas;
    }

    public void agregarCasilla(Casilla casilla) {
        casillas.add(casilla);
    }

    /**
     * Registra un impacto en el barco.
     * @return true si con este impacto el barco queda hundido.
     */
    public boolean recibirImpacto() {
        impactos++;
        return estaHundido();
    }

    public boolean estaHundido() {
        return impactos >= tipo.getTamano();
    }
}