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
    
    /**
     * Igual que existeUsuario(nombre), pero ignora al propio usuario que se
     * está editando (para no rechazar la edición de alguien por su propio
     * nombre actual).
     */
    private boolean existeOtroUsuarioConNombre(String nombre, int idExcluir) throws SQLException {
        String sql = "SELECT id FROM usuario WHERE nombre = ? AND id != ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    public static class ResultadoLogin {
        public final int id;
        public final String nivelAcceso;
 
        public ResultadoLogin(int id, String nivelAcceso) {
            this.id = id;
            this.nivelAcceso = nivelAcceso;
        }
    }    
    
    //Autentica usuario por nombre y contrasena, retorna nivelAcceso
    public ResultadoLogin autenticarUsuario(String nombre, String contrasena) throws SQLException {
        String sql = "SELECT id, nivelAcceso FROM usuario WHERE nombre = ? AND contrasena = ?";
 
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, contrasena);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ResultadoLogin(rs.getInt("id"), rs.getString("nivelAcceso"));
                }
            }
        }
        return null;
    }
 
    //Actualiza nombre, contraseña y nivelAcceso de un usuario existente.
    public void actualizarUsuario(int id, String nombre, String contrasena, String nivelAcceso) throws SQLException {
        if (existeOtroUsuarioConNombre(nombre, id)) {
            throw new SQLException("Ya existe otro usuario con el nombre '" + nombre + "'.");
        }
 
        String sql = "UPDATE usuario SET nombre = ?, contrasena = ?, nivelAcceso = ? WHERE id = ?";
 
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, contrasena);
            ps.setString(3, nivelAcceso);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }
}