package com.dxunvrs.auth_service.repository;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

public interface UserDao {

    @SqlUpdate("INSERT INTO users (username, password_hash) VALUES (:username, :hash)")
    @GetGeneratedKeys("id")
    int register(@Bind("username") String username, @Bind("hash") String hash);

    @SqlQuery("SELECT password_hash FROM users WHERE username = :username")
    Optional<String> getUserHashByUsername(@Bind("username") String username);

    @SqlQuery("SELECT id FROM users WHERE username = :username")
    Optional<Integer> getIdByUsername(@Bind("username") String username);
}
