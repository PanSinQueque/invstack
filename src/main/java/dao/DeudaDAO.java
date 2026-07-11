package dao;

import conexion.ConexionBD;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de acceso a datos para la tabla 'deuda'.
 */
public class DeudaDAO {

    // Registra una deuda nueva, con estado inicial PENDIENTE.
    public int crearDeuda(BigDecimal deuda, int idDetalleVenta, int idTecnico) throws SQLException {
        if (deuda.signum() <= 0) {
            throw new SQLException("El monto de la deuda debe ser mayor a cero.");
        }

        String sql = "INSERT INTO deuda (deuda, fechaCreacion, estado, idDetalleVenta, idTecnico) "
                + "VALUES (?, ?, 'PENDIENTE', ?, ?)";

        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, deuda);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setInt(3, idDetalleVenta);
            ps.setInt(4, idTecnico);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // Marca una deuda como PAGADA.
    public void marcarComoPagada(int id) throws SQLException {
        String sql = "UPDATE deuda SET estado = 'PAGADA' WHERE id = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // Lista las deudas PENDIENTES con el nombre y teléfono del técnico,
    public List<Object[]> listarDeudasPendientes() throws SQLException {
        String sql = "SELECT CONCAT(t.nombre, ' ', t.apellidoPaterno, ' ', t.apellidoMaterno) AS nombreCompleto, "
                + "t.numTel, d.deuda, d.fechaCreacion "
                + "FROM deuda d "
                + "JOIN tecnico t ON d.idTecnico = t.id "
                + "WHERE d.estado = 'PENDIENTE' "
                + "ORDER BY d.fechaCreacion DESC";

        List<Object[]> filas = new ArrayList<>();
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{
                    rs.getString("nombreCompleto"),
                    rs.getString("numTel"),
                    rs.getBigDecimal("deuda"),
                    rs.getTimestamp("fechaCreacion")
                });
            }
        }
        return filas;
    }

    // Lista todas las deudas (pendientes y pagadas) de un técnico específico, 
    public List<Object[]> listarDeudasPorTecnico(int idTecnico) throws SQLException {
        String sql = "SELECT id, deuda, fechaCreacion, estado FROM deuda "
                + "WHERE idTecnico = ? ORDER BY fechaCreacion DESC";

        List<Object[]> filas = new ArrayList<>();
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTecnico);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                        rs.getInt("id"),
                        rs.getBigDecimal("deuda"),
                        rs.getTimestamp("fechaCreacion"),
                        rs.getString("estado")
                    });
                }
            }
        }
        return filas;
    }
}
