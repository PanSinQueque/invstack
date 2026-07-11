package dao;

import conexion.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de acceso a datos para la tabla 'garantia'.
 */
public class GarantiaDAO {

    // Registra una garantía nueva a partir de hoy, con duración en días.
    public int crearGarantia(String descripcion, int mesesDuracion, int idDetalleVenta) throws SQLException {
        if (mesesDuracion <= 0) {
            throw new SQLException("La duración de la garantía debe ser mayor a cero meses.");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new SQLException("La descripción de la garantía no puede estar vacía.");
        }

        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        java.time.LocalDateTime fin = ahora.plusMonths(mesesDuracion);

        String sql = "INSERT INTO garantia (fechaInicio, fechaFin, descripcion, estado, idDetalleVenta) "
                    + "VALUES (?, ?, ?, 'VIGENTE', ?)";

        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(ahora));
            ps.setTimestamp(2, Timestamp.valueOf(fin));
            ps.setString(3, descripcion.trim());
            ps.setInt(4, idDetalleVenta);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }
    

    // Cambia el estado de una garantía
    public void actualizarEstado(int id, String nuevoEstado) throws SQLException {
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            throw new SQLException("El estado no puede estar vacío.");
        }

        String sql = "UPDATE garantia SET estado = ? WHERE id = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.trim());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // Indica si una garantía sigue vigente por fecha
    public boolean estaVigentePorFecha(int id) throws SQLException {
        String sql = "SELECT fechaFin FROM garantia WHERE id = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe una garantía con id " + id + ".");
                }
                java.time.LocalDateTime fechaFin = rs.getTimestamp("fechaFin").toLocalDateTime();
                return fechaFin.isAfter(java.time.LocalDateTime.now());
            }
        }
    }

    // Lista las garantías asociadas a una línea de venta específica (por total, no unidad)
    public List<Object[]> listarPorDetalleVenta(int idDetalleVenta) throws SQLException {
        String sql = "SELECT id, fechaInicio, fechaFin, descripcion, estado FROM garantia "
                + "WHERE idDetalleVenta = ? ORDER BY fechaInicio DESC";

        List<Object[]> filas = new ArrayList<>();
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDetalleVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                        rs.getInt("id"),
                        rs.getTimestamp("fechaInicio"),
                        rs.getTimestamp("fechaFin"),
                        rs.getString("descripcion"),
                        rs.getString("estado")
                    });
                }
            }
        }
        return filas;
    }

    // Lista garantías vigentes (estado = 'ACTIVA' y fechaFin no vencida)
    public List<Object[]> listarVigentes() throws SQLException {
        String sql = "SELECT g.id, g.fechaInicio, g.fechaFin, g.descripcion, "
                + "CONCAT(t.nombre, ' ', t.apellidoPaterno, ' ', t.apellidoMaterno) AS tecnico, "
                + "inv.sku "
                + "FROM garantia g "
                + "JOIN detalleVenta dv ON g.idDetalleVenta = dv.id "
                + "JOIN venta v ON dv.idVenta = v.id "
                + "JOIN inventario inv ON dv.idInventario = inv.id "
                + "LEFT JOIN tecnico t ON v.idTecnico = t.id "
                + "WHERE g.estado = 'VIGENTE' AND g.fechaFin >= ? "
                + "ORDER BY g.fechaFin";

        List<Object[]> filas = new ArrayList<>();
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(java.time.LocalDateTime.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                        rs.getInt("id"),
                        rs.getTimestamp("fechaInicio"),
                        rs.getTimestamp("fechaFin"),
                        rs.getString("descripcion"),
                        rs.getString("tecnico"),
                        rs.getString("sku")
                    });
                }
            }
        }
        return filas;
    }
}
