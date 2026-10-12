package com.chatdemo;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
public class QueryController {
    private final JdbcTemplate db;
    public QueryController(JdbcTemplate db) { this.db = db; }
    @GetMapping("/health")
    public Map<String, String> health() { db.queryForObject("SELECT 1", Integer.class); return Map.of("status", "UP"); }
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return db.queryForMap("SELECT COUNT(*) AS total, COALESCE(SUM(amount),0) AS revenue, COUNT(CASE WHEN status='待处理' THEN 1 END) AS pending, COUNT(DISTINCT customer) AS customers FROM orders");
    }
    @GetMapping("/orders")
    public Map<String, Object> orders(@RequestParam(defaultValue="") String keyword,
            @RequestParam(defaultValue="") String status, @RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="8") int size) {
        if (page < 1 || page > 100000 || size < 1 || size > 100 || keyword.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "查询参数超出范围");
        if (!status.isEmpty() && !List.of("待处理", "处理中", "已完成", "已取消").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的订单状态");
        String where = " WHERE (LOWER(order_no) LIKE ? ESCAPE '!' OR LOWER(customer) LIKE ? ESCAPE '!' OR LOWER(product) LIKE ? ESCAPE '!')";
        String term = "%" + keyword.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_") + "%";
        List<Object> args = new ArrayList<>(List.of(term, term, term));
        if (!status.isEmpty()) { where += " AND status=?"; args.add(status); }
        Long total = db.queryForObject("SELECT COUNT(*) FROM orders" + where, Long.class, args.toArray());
        args.add(size); args.add((page-1)*size);
        return Map.of("items", db.queryForList("SELECT * FROM orders" + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?", args.toArray()), "total", total, "page", page, "size", size);
    }
    @GetMapping("/orders/{id}")
    public Map<String, Object> detail(@PathVariable long id) {
        var rows = db.queryForList("SELECT * FROM orders WHERE id=?", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        return rows.getFirst();
    }
}
