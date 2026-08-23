package ar.edu.utn.frba.greendesign.clientes.repository;

import ar.edu.utn.frba.greendesign.domain.model.Cliente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Cliente> MAPPER = (rs, rowNum) -> new Cliente(
            rs.getLong("id"),
            rs.getString("nombre"),
            rs.getString("email"),
            rs.getString("direccion")
    );

    public ClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Cliente> findAll() {
        return jdbc.query("SELECT id, nombre, email, direccion FROM clientes ORDER BY id", MAPPER);
    }

    public Optional<Cliente> findById(Long id) {
        var list = jdbc.query("SELECT id, nombre, email, direccion FROM clientes WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    public List<Cliente> findByIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        String placeholders = String.join(",", ids.stream().map(x -> "?").toList());
        String sql = "SELECT id, nombre, email, direccion FROM clientes WHERE id IN (" + placeholders + ")";
        return jdbc.query(sql, MAPPER, ids.toArray());
    }

    public Cliente save(String nombre, String email, String direccion) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO clientes (nombre, email, direccion) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, nombre);
            ps.setString(2, email);
            ps.setString(3, direccion);
            return ps;
        }, keyHolder);
        Long id = (Long) keyHolder.getKeys().get("id");
        return new Cliente(id, nombre, email, direccion);
    }

    public Optional<Cliente> update(Long id, String nombre, String email, String direccion) {
        int rows = jdbc.update(
                "UPDATE clientes SET nombre = ?, email = ?, direccion = ? WHERE id = ?",
                nombre, email, direccion, id);
        return rows > 0 ? Optional.of(new Cliente(id, nombre, email, direccion)) : Optional.empty();
    }

    public boolean delete(Long id) {
        return jdbc.update("DELETE FROM clientes WHERE id = ?", id) > 0;
    }
}
