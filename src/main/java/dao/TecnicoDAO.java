package dao;

import conexion.ConexionBD;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de acceso a datos para la tabla 'tecnico'.
 */
public class TecnicoDAO {

    // Registra un técnico nuevo. No valida duplicados por nombre/teléfono
    public int crearTecnico(String apellidoPaterno, String apellidoMaterno, String nombre,
                String numTel, int creadoPor) throws SQLException {

        
        String sql = "INSERT INTO tecnico (apellidoPaterno, apellidoMaterno, nombre, numTel, fechaCreacion, creadoPor) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, apellidoPaterno);
            ps.setString(2, apellidoMaterno);
            ps.setString(3, nombre);
            ps.setString(4, numTel);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.setInt(6, creadoPor);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // Actualiza los datos de un técnico existente.
    public void actualizarTecnico(int id, String apellidoPaterno, String apellidoMaterno,
                String nombre, String numTel) throws SQLException {

        String sql = "UPDATE tecnico SET apellidoPaterno = ?, apellidoMaterno = ?, nombre = ?, numTel = ? "
                    + "WHERE id = ?";

        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, apellidoPaterno);
            ps.setString(2, apellidoMaterno);
            ps.setString(3, nombre);
            ps.setString(4, numTel);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    // Elimina un técnico por id. No elimina técnicos con ventas a su nombre
    public void eliminarTecnico(int id) throws SQLException {
        String sql = "DELETE FROM tecnico WHERE id = ?";

        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException ex) {
            throw new SQLException("No se puede eliminar: este técnico ya tiene ventas o deudas "
                    + "registradas a su nombre.", ex);
        }
    }

    // Lista todos los técnicos con su adeudo pendiente
    public List<Object[]> listarTecnicosConAdeudo() throws SQLException {
        String sql = "SELECT t.id, "
                    + "CONCAT(t.nombre, ' ', t.apellidoPaterno, ' ', t.apellidoMaterno) AS nombreCompleto, "
                    + "t.numTel, "
                    + "COALESCE(SUM(CASE WHEN d.estado = 'PENDIENTE' THEN d.deuda ELSE 0 END), 0) AS adeudo "
                    + "FROM tecnico t "
                    + "LEFT JOIN deuda d ON d.idTecnico = t.id "
                    + "GROUP BY t.id, nombreCompleto, t.numTel "
                    + "ORDER BY nombreCompleto";

        List<Object[]> filas = new ArrayList<>();
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombreCompleto"),
                    rs.getString("numTel"),
                    rs.getBigDecimal("adeudo")
                });
            }
        }
        return filas;
    }

    // Busca técnicos cuyo nombre completo o teléfono coincidan parcialmente con el filtro.
    public List<Object[]> buscarTecnicos(String filtro) throws SQLException {
        String sql = "SELECT id, CONCAT(nombre, ' ', apellidoPaterno, ' ', apellidoMaterno) AS nombreCompleto, numTel "
                    + "FROM tecnico "
                    + "WHERE CONCAT(nombre, ' ', apellidoPaterno, ' ', apellidoMaterno) LIKE ? OR numTel LIKE ? "
                    + "ORDER BY nombreCompleto";

        String comodin = "%" + filtro + "%";
        List<Object[]> filas = new ArrayList<>();
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, comodin);
            ps.setString(2, comodin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                        rs.getInt("id"),
                        rs.getString("nombreCompleto"),
                        rs.getString("numTel")
                    });
                }
            }
        }
        return filas;
    }
}
