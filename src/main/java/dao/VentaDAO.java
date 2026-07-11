package dao;

import conexion.ConexionBD;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

/**
 * Operaciones de acceso a datos para la tabla 'ventas' y 'detalleVentas'.
 */
public class VentaDAO {

    // Una línea de venta: idInventario, cantidad y precioUnitario
    public static class ItemVenta {
        public final int idInventario;
        public final int cantidad;
        public final BigDecimal precioUnitario;

        public ItemVenta(int idInventario, int cantidad, BigDecimal precioUnitario) {
            this.idInventario = idInventario;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }
    }

    /**
     * Registra una venta completa en una sola transacción: inserta
     * 'venta', un renglón de 'detalleVenta' por cada item, y descuenta el
     * stock vendido de 'inventario'. Si algo falla a mitad de camino, se
     * revierte todo.
     */
    public void registrarVenta(int idUsuario, Integer idTecnico, List<ItemVenta> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            throw new SQLException("La venta debe tener al menos un producto.");
        }

        BigDecimal totalVenta = BigDecimal.ZERO;
        for (ItemVenta item : items) {
            if (item.cantidad <= 0) {
                throw new SQLException("La cantidad de cada producto debe ser mayor a cero.");
            }
            if (item.precioUnitario.signum() < 0) {
                throw new SQLException("El precio unitario no puede ser negativo.");
            }
            totalVenta = totalVenta.add(item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad)));
        }

        Connection con = ConexionBD.getInstancia().getConexion();
        try {
            con.setAutoCommit(false);

            int idVenta = insertarVenta(con, totalVenta, idUsuario, idTecnico);

            for (ItemVenta item : items) {
                insertarDetalleVenta(con, item, idVenta);
                descontarStock(con, item.idInventario, item.cantidad);
            }

            con.commit();
        } catch (SQLException ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private int insertarVenta(Connection con, BigDecimal totalVenta, int idUsuario, Integer idTecnico) throws SQLException {
        String sql = "INSERT INTO venta (totalVenta, fechaCreacion, idUsuario, idTecnico) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, totalVenta);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setInt(3, idUsuario);
            if (idTecnico != null) {
                ps.setInt(4, idTecnico);
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void insertarDetalleVenta(Connection con, ItemVenta item, int idVenta) throws SQLException {
        String sql = "INSERT INTO detalleVenta (precioUnitario, cantidad, idInventario, idVenta) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, item.precioUnitario);
            ps.setInt(2, item.cantidad);
            ps.setInt(3, item.idInventario);
            ps.setInt(4, idVenta);
            ps.executeUpdate();
        }
    }

    // Descuenta stock vendido, rechaza la venta sí no hay suficiente en inventario
    private void descontarStock(Connection con, int idInventario, int cantidad) throws SQLException {
        String sql = "UPDATE inventario SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idInventario);
            ps.setInt(3, cantidad);
            int filasActualizadas = ps.executeUpdate();
            if (filasActualizadas == 0) {
                throw new SQLException("No hay stock suficiente para el producto con id " + idInventario + ".");
            }
        }
    }

    // Devuelve las ventas registradas
    public List<Object[]> listarVentas() throws SQLException {
        String sql = "SELECT v.id, v.totalVenta, v.fechaCreacion, u.nombre AS usuario, "
                + "t.nombre AS tecnicoNombre "
                + "FROM venta v "
                + "JOIN usuario u ON v.idUsuario = u.id "
                + "LEFT JOIN tecnico t ON v.idTecnico = t.id "
                + "ORDER BY v.fechaCreacion DESC";

        List<Object[]> filas = new java.util.ArrayList<>();
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{
                    rs.getInt("id"),
                    rs.getBigDecimal("totalVenta"),
                    rs.getTimestamp("fechaCreacion"),
                    rs.getString("usuario"),
                    rs.getString("tecnicoNombre")
                });
            }
        }
        return filas;
    }
}
