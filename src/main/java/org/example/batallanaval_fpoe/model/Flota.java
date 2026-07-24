package org.example.batallanaval_fpoe.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Conjunto completo de barcos de un jugador (1 portaaviones,
 * 2 submarinos, 3 destructores, 4 fragatas).
 *
 * @author Daniel
 */
public class Flota implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private final List<Barco> barcos;

    public Flota() {
        this.barcos = new ArrayList<>();
        for (TipoBarco tipo : TipoBarco.values()) {
            for (int i = 0; i < tipo.getCantidadPorFlota(); i++) {
                barcos.add(BarcoFactory.crear(tipo));
            }
        }
    }

    public List<Barco> getBarcos() {
        return barcos;
    }

    public boolean estaCompletamenteHundida() {
        return barcos.stream().allMatch(Barco::estaHundido);
    }

    public long contarHundidos() {
        return barcos.stream().filter(Barco::estaHundido).count();
    }

    public void reiniciar() {

        barcos.clear();

        for (TipoBarco tipo : TipoBarco.values()) {

            for (int i = 0; i < tipo.getCantidadPorFlota(); i++) {

                barcos.add(BarcoFactory.crear(tipo));

            }

        }

    }
}
