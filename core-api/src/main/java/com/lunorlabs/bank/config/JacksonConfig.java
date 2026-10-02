package com.lunorlabs.bank.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    /** Contract: money is always a JSON string ("30000.00"), never a float. */
    @Bean
    public com.fasterxml.jackson.databind.Module moneyModule() {
        SimpleModule m = new SimpleModule("money");
        m.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal v, JsonGenerator g, SerializerProvider p) throws IOException {
                g.writeString(v.toPlainString());
            }
        });
        return m;
    }
}
