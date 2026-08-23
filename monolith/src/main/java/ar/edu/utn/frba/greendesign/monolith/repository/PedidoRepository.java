package ar.edu.utn.frba.greendesign.monolith.repository;

import ar.edu.utn.frba.greendesign.domain.dto.PedidoDetailResponse;
import ar.edu.utn.frba.greendesign.domain.model.Cliente;
import ar.edu.utn.frba.greendesign.domain.model.ItemPedido;
import ar.edu.utn.frba.greendesign.domain.model.Pedido;
import ar.edu.utn.frba.greendesign.domain.model.Producto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PedidoRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Pedido> PEDIDO_MAPPER = (rs, _) -> new Pedido(
            rs.getLong("id"),
            rs.getLong("cliente_id"),
            rs.getTimestamp("fecha").toLocalDateTime(),
            rs.getString("estado"),
            rs.getBigDecimal("total"),
            List.of()
    );

    private static final RowMapper<ItemPedido> ITEM_MAPPER = (rs, _) -> new ItemPedido(
            rs.getLong("id"),
            rs.getLong("pedido_id"),
            rs.getLong("producto_id"),
            rs.getInt("cantidad"),
            rs.getBigDecimal("precio_unitario")
    );

    public PedidoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Pedido> findAll() {
        return jdbc.query("SELECT id, cliente_id, fecha, estado, total FROM pedidos ORDER BY id", PEDIDO_MAPPER);
    }

    public Optional<Pedido> findById(Long id) {
        var pedidos = jdbc.query(
                "SELECT id, cliente_id, fecha, estado, total FROM pedidos WHERE id = ?",
                PEDIDO_MAPPER, id);
        if (pedidos.isEmpty()) return Optional.empty();

        Pedido pedido = pedidos.getFirst();
        List<ItemPedido> items = jdbc.query(
                "SELECT id, pedido_id, producto_id, cantidad, precio_unitario FROM items_pedido WHERE pedido_id = ?",
                ITEM_MAPPER, id);
        return Optional.of(new Pedido(pedido.id(), pedido.clienteId(), pedido.fecha(),
                pedido.estado(), pedido.total(), items));
    }

    /**
     * Operación clave del experimento: obtener pedido con cliente y productos usando JOIN.
     * En el monolito esto es 1 query; en microservicios son 3 llamadas HTTP.
     */
    public Optional<PedidoDetailResponse> findDetailById(Long id) {
        String sql = """
                SELECT p.id AS p_id, p.cliente_id, p.fecha, p.estado, p.total,
                       c.id AS c_id, c.nombre AS c_nombre, c.email AS c_email, c.direccion AS c_direccion,
                       ip.id AS ip_id, ip.producto_id, ip.cantidad, ip.precio_unitario,
                       pr.id AS pr_id, pr.nombre AS pr_nombre, pr.descripcion AS pr_descripcion,
                       pr.precio AS pr_precio, pr.stock AS pr_stock
                FROM pedidos p
                JOIN clientes c ON c.id = p.cliente_id
                JOIN items_pedido ip ON ip.pedido_id = p.id
                JOIN productos pr ON pr.id = ip.producto_id
                WHERE p.id = ?
                ORDER BY ip.id
                """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, id);
        if (rows.isEmpty()) return Optional.empty();

        Map<String, Object> first = rows.getFirst();
        Pedido pedido = new Pedido(
                (Long) first.get("p_id"),
                (Long) first.get("cliente_id"),
                ((Timestamp) first.get("fecha")).toLocalDateTime(),
                (String) first.get("estado"),
                (BigDecimal) first.get("total"),
                List.of()
        );
        Cliente cliente = new Cliente(
                (Long) first.get("c_id"),
                (String) first.get("c_nombre"),
                (String) first.get("c_email"),
                (String) first.get("c_direccion")
        );

        List<PedidoDetailResponse.ProductoConCantidad> productos = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Producto producto = new Producto(
                    (Long) row.get("pr_id"),
                    (String) row.get("pr_nombre"),
                    (String) row.get("pr_descripcion"),
                    (BigDecimal) row.get("pr_precio"),
                    (Integer) row.get("pr_stock")
            );
            productos.add(new PedidoDetailResponse.ProductoConCantidad(
                    producto, (Integer) row.get("cantidad")));
        }

        return Optional.of(new PedidoDetailResponse(pedido, cliente, productos));
    }

    public Pedido save(Long clienteId, BigDecimal total, List<ItemPedido> items) {
        LocalDateTime fecha = LocalDateTime.now();
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO pedidos (cliente_id, fecha, estado, total) VALUES (?, ?, 'CREADO', ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, clienteId);
            ps.setTimestamp(2, Timestamp.valueOf(fecha));
            ps.setBigDecimal(3, total);
            return ps;
        }, keyHolder);
        Long pedidoId = (Long) keyHolder.getKeys().get("id");

        List<ItemPedido> savedItems = new ArrayList<>();
        for (ItemPedido item : items) {
            var itemKeyHolder = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO items_pedido (pedido_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, pedidoId);
                ps.setLong(2, item.productoId());
                ps.setInt(3, item.cantidad());
                ps.setBigDecimal(4, item.precioUnitario());
                return ps;
            }, itemKeyHolder);
            Long itemId = (Long) itemKeyHolder.getKeys().get("id");
            savedItems.add(new ItemPedido(itemId, pedidoId, item.productoId(), item.cantidad(), item.precioUnitario()));
        }

        return new Pedido(pedidoId, clienteId, fecha, "CREADO", total, savedItems);
    }

    public boolean updateEstado(Long id, String estado) {
        return jdbc.update("UPDATE pedidos SET estado = ? WHERE id = ?", estado, id) > 0;
    }

    public boolean delete(Long id) {
        jdbc.update("DELETE FROM items_pedido WHERE pedido_id = ?", id);
        return jdbc.update("DELETE FROM pedidos WHERE id = ?", id) > 0;
    }
}
