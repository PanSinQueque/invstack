package conexion;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Maneja la conexión a la Base de Datos InvStack. 
 */
public class ConexionBD {

    //Definir ruta fija al archivo de configuración de la BD
    private static final String RUTA_CONFIG = "config/db.properties";
    //Definir Usuario y Contraseña de la BD 
    private static final String USUARIO = "invstack_db";
    private static final String CONTRASENA = "invstack_db";

    private static ConexionBD instancia;
    private Connection conexion;

    private ConexionBD() {
    }

    public static ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    // Abre (o reutiliza) la conexión a la BD usando los datos del archivo de configuración externa.
    public Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            Properties props = cargarConfiguracion();

            String host = props.getProperty("db.host", "localhost");
            String port = props.getProperty("db.port", "3306");
            String nombreBD = props.getProperty("db.name", "invstack");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + nombreBD + "?useSSL=false&serverTimezone=America/Mexico_City";

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("No se encontró el driver de MySQL (mysql-connector-j). ", e);
            }

            conexion = DriverManager.getConnection(url, USUARIO, CONTRASENA);
        }
        return conexion;
    }

    private Properties cargarConfiguracion() throws SQLException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(RUTA_CONFIG)) {
            props.load(fis);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer el archivo de configuración '" + RUTA_CONFIG + "'. Verifica que exista junto al ejecutable.", e);
        }
        return props;
    }

    public void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}