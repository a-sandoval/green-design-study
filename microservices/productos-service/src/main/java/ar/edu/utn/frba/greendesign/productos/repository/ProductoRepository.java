package ar.edu.utn.frba.greendesign.productos.repository;

import ar.edu.utn.frba.greendesign.domain.model.Producto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductoRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Producto> MAPPER = (rs, rowNum) -> new Producto(
            rs.getLong("id"),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getBigDecimal("precio"),
            rs.getInt("stock")
    );

    public ProductoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Producto> findAll() {
        return jdbc.query("SELECT id, nombre, descripcion, precio, stock FROM productos ORDER BY id", MAPPER);
    }

    public Optional<Producto> findById(Long id) {
        var list = jdbc.query("SELECT id, nombre, descripcion, precio, stock FROM productos WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    public List<Producto> findByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        String placeholders = String.join(",", ids.stream().map(x -> "?").toList());
        String sql = "SELECT id, nombre, descripcion, precio, stock FROM productos WHERE id IN (" + placeholders + ")";
        return jdbc.query(sql, MAPPER, ids.toArray());
    }

    public Producto save(String nombre, String descripcion, BigDecimal precio, Integer stock) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO productos (nombre, descripcion, precio, stock) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, nombre);
            ps.setString(2, descripcion);
            ps.setBigDecimal(3, precio);
            ps.setInt(4, stock);
            return ps;
        }, keyHolder);
        Long id = (Long) keyHolder.getKeys().get("id");
        return new Producto(id, nombre, descripcion, precio, stock);
    }

    public Optional<Producto> update(Long id, String nombre, String descripcion, BigDecimal precio, Integer stock) {
        int rows = jdbc.update(
                "UPDATE productos SET nombre = ?, descripcion = ?, precio = ?, stock = ? WHERE id = ?",
                nombre, descripcion, precio, stock, id);
        return rows > 0 ? Optional.of(new Producto(id, nombre, descripcion, precio, stock)) : Optional.empty();
    }

    public void decrementStock(Long productoId, int cantidad) {
        int rows = jdbc.update(
                "UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?",
                cantidad, productoId, cantidad);
        if (rows == 0) {
            throw new IllegalStateException("Stock insuficiente para producto " + productoId);
        }
    }

    public boolean delete(Long id) {
        return jdbc.update("DELETE FROM productos WHERE id = ?", id) > 0;
    }
}
