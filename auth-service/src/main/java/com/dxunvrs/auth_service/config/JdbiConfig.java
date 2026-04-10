package com.dxunvrs.auth_service.config;

import com.dxunvrs.auth_service.repository.UserDao;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class JdbiConfig {
    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        return Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin()).installPlugin(new PostgresPlugin());
    }

    @Bean
    public UserDao userDao(Jdbi jdbi) {
        return jdbi.onDemand(UserDao.class);
    }
}
