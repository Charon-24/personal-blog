package com.charon.personalblog.administration;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/site")
public class SiteController {
    private final JdbcClient jdbc;

    public SiteController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    Map<String, String> site() {
        Map<String, String> result = new LinkedHashMap<>();
        jdbc.sql("SELECT config_key,config_value FROM site_config WHERE public_value=true ORDER BY config_key")
                .query((rs, row) -> Map.entry(rs.getString(1), rs.getString(2))).list()
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    @GetMapping("/links")
    List<LinkView> links() {
        return jdbc.sql("""
                SELECT id,name,description,url,icon_url,sort_order
                FROM friend_link WHERE enabled=true ORDER BY sort_order,name
                """).query(LinkView.class).list();
    }

    public record LinkView(UUID id, String name, String description, String url, String iconUrl, int sortOrder) {}
}
