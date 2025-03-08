package com.example.appseguimiento.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appseguimiento.R;
import com.example.appseguimiento.data.MediaItem;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    private List<MediaItem> lista;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MediaItem item);
    }
    // Constructor
    public MediaAdapter(List<MediaItem> lista, OnItemClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }
    // Métodos de RecyclerView.Adapter
    @Override
    public MediaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MediaViewHolder holder, int position) {
        final MediaItem item = lista.get(position);
        holder.tvTitulo.setText(item.getTitulo());
        holder.tvDescripcion.setText(item.getDescripcion());
        holder.tvEstado.setText(
                item.isCompleted() ?
                        holder.itemView.getContext().getString(R.string.terminada) :
                        holder.itemView.getContext().getString(R.string.pendiente)
        );
        // Asigna el ícono según el tipo
        String tipo = item.getTipo();
        if (tipo != null) {
            if (tipo.equalsIgnoreCase("libro") || tipo.equalsIgnoreCase("book") || tipo.equalsIgnoreCase("liburu")) {
                holder.ivTipo.setImageResource(R.drawable.ic_book);
            } else if (tipo.equalsIgnoreCase("pelicula") || tipo.equalsIgnoreCase("movie") || tipo.equalsIgnoreCase("filma")) {
                holder.ivTipo.setImageResource(R.drawable.ic_movie);
            } else {
                // Opción por defecto, si no coincide
                holder.ivTipo.setImageResource(R.drawable.ic_default);
            }
        } else {
            // Si no hay tipo, asigna una imagen por defecto
            holder.ivTipo.setImageResource(R.drawable.ic_default);
        }


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void updateList(List<MediaItem> newList) {
        this.lista = newList;
        notifyDataSetChanged();
    }
    // Clase interna MediaViewHolder
    class MediaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvEstado;
        ImageView ivTipo;
        MediaViewHolder(View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            ivTipo = itemView.findViewById(R.id.ivTipo);
        }
    }
}
