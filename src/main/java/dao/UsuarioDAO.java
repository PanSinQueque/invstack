package dao;

import conexion.ConexionBD;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Operaciones de acceso a datos para la tabla 'usuario'.
 */
public class UsuarioDAO {

    public void crearUsuario(String nombre, String contrasena, String nivelAcceso) throws SQLException {
        if (existeUsuario(nombre)) {
            throw new SQLException("Ya existe un usuario con el nombre '" + nombre + "'.");
        }

        String sql = "INSERT INTO usuario (nombre, contrasena, nivelAcceso, fechaCreacion) " + "VALUES (?, ?, ?, ?)";

        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, contrasena);
            ps.setString(3, nivelAcceso);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
    }

    private boolean existeUsuario(String nombre) throws SQLException {
        String sql = "SELECT id FROM usuario WHERE nombre = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}