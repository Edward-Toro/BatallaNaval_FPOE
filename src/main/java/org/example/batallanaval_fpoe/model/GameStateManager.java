package org.example.batallanaval_fpoe.model;

import java.io.*;

/**
 * Encargado de persistir y recuperar el estado de la partida.
 * Usa dos mecanismos distintos, tal como pide el enunciado:
 * <ul>
 *   <li>Serialización binaria para los tableros (flotas, disparos, estado completo).</li>
 *   <li>Archivo plano de texto para el nickname y la cantidad de barcos hundidos.</li>
 * </ul>
 *
 * @author Daniel, Nicolás, Robert
 */
public class GameStateManager {

    private static final String ARCHIVO_TABLEROS = "partida_guardada.dat";
    private static final String ARCHIVO_PLANO = "jugador_info.txt";

    /**
     * Guarda el estado completo de la partida: tableros serializados
     * y archivo plano con nickname y barcos hundidos.
     */
    public static void guardarPartida(BatallaNavalFacade facade, String nickname) {
        guardarTableros(facade);
        guardarArchivoPlano(facade, nickname);
    }

    private static void guardarTableros(BatallaNavalFacade facade) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(ARCHIVO_TABLEROS))) {
            oos.writeObject(facade);
        } catch (IOException e) {
            System.err.println("Error guardando tableros: " + e.getMessage());
        }
    }

    private static void guardarArchivoPlano(BatallaNavalFacade facade, String nickname) {
        long hundidosJugador = facade.getTableroJugador().getFlota().contarHundidos();
        long hundidosMaquina = facade.getTableroMaquina().getFlota().contarHundidos();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_PLANO))) {
            writer.write("nickname=" + nickname);
            writer.newLine();
            writer.write("barcosHundidosPorJugador=" + hundidosMaquina);
            writer.newLine();
            writer.write("barcosHundidosPropios=" + hundidosJugador);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error guardando archivo plano: " + e.getMessage());
        }
    }

    /**
     * Verifica si existe una partida guardada en disco.
     */
    public static boolean existePartidaGuardada() {
        File f = new File(ARCHIVO_TABLEROS);
        return f.exists();
    }

    /**
     * Carga la partida guardada. Devuelve null si no existe o si el archivo
     * está corrupto/no se puede leer.
     */
    public static BatallaNavalFacade cargarPartida() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(ARCHIVO_TABLEROS))) {
            return (BatallaNavalFacade) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error cargando partida: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lee el nickname guardado en el archivo plano, o un valor por defecto
     * si no existe todavía.
     */
    public static String leerNickname() {
        File f = new File(ARCHIVO_PLANO);
        if (!f.exists()) return "Jugador";

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.startsWith("nickname=")) {
                    return linea.substring("nickname=".length());
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo nickname: " + e.getMessage());
        }
        return "Jugador";
    }

    /**
     * Elimina los archivos de guardado (al terminar la partida o iniciar una nueva).
     */
    public static void borrarPartidaGuardada() {
        new File(ARCHIVO_TABLEROS).delete();
    }
}