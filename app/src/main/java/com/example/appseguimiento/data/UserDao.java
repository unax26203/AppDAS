package com.example.appseguimiento.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Insert
    void insertUser(User user);

    // Consulta para validar el login
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    User login(String username, String password);

    // Consulta para verificar si el usuario ya existe (para el registro)
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);
}
