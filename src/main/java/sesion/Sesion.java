package sesion;

/**
 * Guarda quién inició sesión (id, nombre, nivelAcceso) mientras la app
 * está abierta. Se llena en Login al autenticar con éxito, se limpia al
 * cerrar sesión.
 */
public class Sesion {

    private static Integer idUsuario;
    private static String nombreUsuario;
    private static String nivelAcceso;

    private Sesion() {}

    public static void iniciarSesion(int id, String nombre, String nivelAcceso) {
        Sesion.idUsuario = id;
        Sesion.nombreUsuario = nombre;
        Sesion.nivelAcceso = nivelAcceso;
    }

    public static void cerrarSesion() {
        idUsuario = null;
        nombreUsuario = null;
        nivelAcceso = null;
    }

    public static boolean haySesionActiva() {
        return idUsuario != null;
    }

    public static int getIdUsuario() {
        if (idUsuario == null) {
            throw new IllegalStateException("No hay una sesión activa.");
        }
        return idUsuario;
    }

    public static String getNombreUsuario() {
        return nombreUsuario;
    }

    public static String getNivelAcceso() {
        return nivelAcceso;
    }
}
