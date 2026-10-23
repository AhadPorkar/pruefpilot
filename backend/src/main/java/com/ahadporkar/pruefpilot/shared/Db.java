package com.ahadporkar.pruefpilot.shared;

import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Small SQL boundary. All business queries MUST include an organization predicate.
 * Maps are explicitly shaped SQL projections, never unfiltered SELECT * responses. */
@Repository
public class Db {
    private final JdbcTemplate jdbc;
    public Db(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static Object parameter(Object value) {
        return value instanceof Instant i ? Timestamp.from(i) : value;
    }
    private static Object[] parameters(Object[] input) { return Arrays.stream(input).map(Db::parameter).toArray(); }
    public int update(String sql, Object... args) { return jdbc.update(sql, parameters(args)); }
    public List<Map<String,Object>> list(String sql, Object... args) { return jdbc.query(sql, mapper(), parameters(args)); }
    public Map<String,Object> one(String sql, Object... args) {
        List<Map<String,Object>> rows = list(sql,args);
        if (rows.isEmpty()) throw ApiException.missing();
        if (rows.size() != 1) throw new IllegalStateException("Expected one row.");
        return rows.getFirst();
    }
    public Optional<Map<String,Object>> optional(String sql, Object... args) {
        var rows=list(sql,args); return rows.isEmpty()? Optional.empty():Optional.of(rows.getFirst());
    }
    public long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, parameters(args)); return value == null ? 0 : value;
    }
    public byte[] bytes(String sql, Object... args) { return jdbc.queryForObject(sql, byte[].class, parameters(args)); }
    public static String str(Map<String,Object> row, String key) { return Objects.toString(row.get(key), ""); }
    public static UUID uuid(Map<String,Object> row, String key) {
        Object value=row.get(key); return value == null ? null : UUID.fromString(value.toString());
    }
    public static int number(Map<String,Object> row, String key) { return ((Number)row.get(key)).intValue(); }
    public static boolean bool(Map<String,Object> row, String key) { return Boolean.TRUE.equals(row.get(key)); }
    private RowMapper<Map<String,Object>> mapper() {
        return (rs,index) -> {
            var map = new LinkedHashMap<String,Object>();
            var metadata = rs.getMetaData();
            for (int column=1; column<=metadata.getColumnCount(); column++) {
                String name=metadata.getColumnLabel(column); StringBuilder camel=new StringBuilder(); boolean upper=false;
                for(char c:name.toCharArray()) { if(c=='_'){upper=true;continue;} camel.append(upper?Character.toUpperCase(c):c);upper=false; }
                Object value=rs.getObject(column);
                if(value instanceof Timestamp t) value=t.toInstant().toString();
                else if(value instanceof java.sql.Date d) value=d.toLocalDate().toString();
                else if(value instanceof UUID u) value=u.toString();
                map.put(camel.toString(),value);
            }
            return map;
        };
    }
}
