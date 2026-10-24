package com.lunorlabs.bank.i18n;

import com.lunorlabs.bank.action.LocalizedSummary;
import com.lunorlabs.bank.action.Summary;
import com.lunorlabs.bank.common.Lang;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads i18n_messages once (small table) and formats "{placeholder}" values.
 * Fallback order: requested language -> EN -> the key itself.
 */
@Service
public class MessageService {
    private final JdbcClient jdbc;
    private volatile Map<String, String> cache;

    public MessageService(JdbcClient jdbc) { this.jdbc = jdbc; }

    public String text(String key, Lang lang, Map<String, String> params) {
        Map<String, String> c = cache();
        String t = c.get(key + "|" + lang.name());
        if (t == null) t = c.get(key + "|EN");
        if (t == null) t = key;
        for (Map.Entry<String, String> e : params.entrySet()) {
            t = t.replace("{" + e.getKey() + "}", e.getValue());
        }
        return t;
    }

    public LocalizedSummary localize(Summary s, Lang lang) {
        List<LocalizedSummary.Line> lines = s.lines().stream()
                .map(l -> new LocalizedSummary.Line(text(l.labelKey(), lang, Map.of()), l.value()))
                .toList();
        return new LocalizedSummary(text(s.titleKey(), lang, s.titleParams()), lines);
    }

    /** Call after staff edit i18n_messages. */
    public void refresh() { cache = null; }

    private Map<String, String> cache() {
        Map<String, String> c = cache;
        if (c == null) {
            synchronized (this) {
                if (cache == null) cache = load();
                c = cache;
            }
        }
        return c;
    }

    private Map<String, String> load() {
        Map<String, String> m = new HashMap<>();
        jdbc.sql("SELECT msg_key, lang, text FROM i18n_messages")
                .query((rs, i) -> {
                    m.put(rs.getString(1) + "|" + rs.getString(2).trim(), rs.getString(3));
                    return 1;
                }).list();
        return m;
    }
}
