package com.wrenchlog.wrenchlog;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource catalogDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:sqlite:file:src/main/resources/catalog.db")
                .driverClassName("org.sqlite.JDBC")
                .build();
    }
}