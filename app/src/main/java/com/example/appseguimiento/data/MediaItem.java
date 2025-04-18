package com.example.appseguimiento.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "media_items")
public class MediaItem {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String titulo;
    private String descripcion;
    private boolean isCompleted;
    // Nuevo campo para diferenciar: "libro" o "pelicula"
    private String tipo;

    private String imagen;

    public MediaItem(String titulo, String descripcion, boolean isCompleted, String tipo, String imagen) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.isCompleted = isCompleted;
        this.tipo = tipo;
        this.imagen = imagen;
    }
    // Getters y setters para el nuevo campo:
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    // Getters y Setters
    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getImagen() { return imagen; }

    public void setImagen(String imagen) { this.imagen = imagen; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}

