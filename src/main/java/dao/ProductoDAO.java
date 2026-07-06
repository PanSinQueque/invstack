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
 * Operaciones de acceso a datos para las tablas 'marca' 'modelo' 'pieza' 'inventario'.
 */
public class ProductoDAO {

    //Registra un producto completo
    public void registrarProducto(String marca, String modelo, String pieza, String sku,
            int stock, int stockMinimo, String calidad,
            BigDecimal precioMayoreo, BigDecimal precioMenudeo) throws SQLException {

        if (stock < 0 || stockMinimo < 0) {
            throw new SQLException("El stock y el stock mínimo no pueden ser negativos.");
        }
        if (precioMayoreo.signum() < 0 || precioMenudeo.signum() < 0) {
            throw new SQLException("Los precios no pueden ser negativos.");
        }
        if (existeSku(sku)) {
            throw new SQLException("Ya existe un producto con el SKU '" + sku + "'.");
        }
        
        int idMarca = obtenerOCrearMarca(marca);
        int idModelo = obtenerOCrearModelo(modelo, idMarca);
        int idPieza = obtenerOCrearPieza(pieza, idModelo);
        
        String sql = "INSERT INTO inventario "
                + "(sku, stock, stockMinimo, calidad, precioMayoreo, precioMenudeo, fechaModif, idPieza) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection con = ConexionBD.getInstancia().getConexion();
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.setInt(2, stock);
            ps.setInt(3, stockMinimo);
            ps.setString(4, calidad);
            ps.setBigDecimal(5, precioMayoreo);
            ps.setBigDecimal(6, precioMenudeo);
            ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            ps.setInt(8, idPieza);
            ps.executeUpdate();
        }
    }

    private boolean existeSku(String sku) throws SQLException {
        String sql = "SELECT id FROM inventario WHERE sku = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


    //Busca una marca por nombre; si no existe, la crea. Devuelve su id.
    private int obtenerOCrearMarca(String nombre) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConexion();
        
        String sqlBuscar = "SELECT id FROM marca WHERE nombre = ?";
        
        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        
        String sqlInsertar = "INSERT INTO marca (nombre) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(sqlInsertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // Busca un modelo por nombre dentro de una marca     
    private int obtenerOCrearModelo(String nombre, int idMarca) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConexion();
        
        String sqlBuscar = "SELECT id FROM modelo WHERE nombre = ? AND idMarca = ?";
        
        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, nombre);
            ps.setInt(2, idMarca);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        
        String sqlInsertar = "INSERT INTO modelo (nombre, idMarca) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlInsertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setInt(2, idMarca);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    //Busca una pieza por nombre dentro de un modelo; si no existe, la crea. 
    private int obtenerOCrearPieza(String nombre, int idModelo) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConexion();
        String sqlBuscar = "SELECT id FROM pieza WHERE nombre = ? AND idModelo = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
            ps.setString(1, nombre);
            ps.setInt(2, idModelo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        String sqlInsertar = "INSERT INTO pieza (nombre, idModelo) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlInsertar, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setInt(2, idModelo);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /**
     * Devuelve los renglones para poblar la tabla de la pantalla
     * Inventario: Marca, Modelo, Pieza, SKU, Stock, Precio Mayoreo, Precio Menudeo, Estado.
     */
    public List<Object[]> listarInventario() throws SQLException {
        String sql = "SELECT ma.nombre AS marca, mo.nombre AS modelo, pz.nombre AS pieza, "
                + "inv.sku, inv.stock, inv.precioMayoreo, inv.precioMenudeo, inv.calidad "
                + "FROM inventario inv "
                + "JOIN pieza pz ON inv.idPieza = pz.id "
                + "JOIN modelo mo ON pz.idModelo = mo.id "
                + "JOIN marca ma ON mo.idMarca = ma.id "
                + "ORDER BY ma.nombre, mo.nombre, pz.nombre";
        List<Object[]> filas = new ArrayList<>();
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    rs.getString("pieza"),
                    rs.getString("sku"),
                    rs.getInt("stock"),
                    rs.getBigDecimal("precioMayoreo"),
                    rs.getBigDecimal("precioMenudeo"),
                    rs.getString("calidad")
                });
            }
        }
        return filas;
    }
    
    //Busqueda por nombre, devuelve las filas con dicho nombre
    public List<Object[]> buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT ma.nombre AS marca, mo.nombre AS modelo, "
            + "pz.nombre AS pieza, inv.sku, inv.stock, "
            + "inv.precioMayoreo, inv.precioMenudeo, inv.calidad "
            + "FROM inventario inv "
            + "JOIN pieza pz ON inv.idPieza = pz.id "
            + "JOIN modelo mo ON pz.idModelo = mo.id "
            + "JOIN marca ma ON mo.idMarca = ma.id "
            + "WHERE pz.nombre LIKE ? "
            + "ORDER BY ma.nombre, mo.nombre, pz.nombre";
        List<Object[]> filas = new ArrayList<>();
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{
                        rs.getString("marca"),
                        rs.getString("modelo"),
                        rs.getString("pieza"),
                        rs.getString("sku"),
                        rs.getInt("stock"),
                        rs.getBigDecimal("precioMayoreo"),
                        rs.getBigDecimal("precioMenudeo"),
                        rs.getString("calidad")
                    });
                }
            }
        }
        return filas;
    }
}
