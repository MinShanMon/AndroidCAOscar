package com.example.memorygame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.bumptech.glide.Glide;

import java.util.List;

public class GameImagesAdapter extends RecyclerView.Adapter<GameImagesAdapter.ViewHolder> {
    private boolean useGlide;
    private List<Image> gameImages;

    public GameImagesAdapter(List<Image> gameImages, boolean useGlide) {
        this.gameImages = gameImages;
        this.useGlide = useGlide;
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView gameImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            gameImageView = itemView.findViewById(R.id.gameImageView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        listener.onItemClick(itemView, position);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.game_row_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Image gameImage = gameImages.get(position);
        ImageView gameImageView = holder.gameImageView;

        if (useGlide) {
            Glide.with(gameImageView.getContext()).load(gameImage.getFilePath()).into(gameImageView);
        } else {
            Bitmap bitmap = BitmapFactory.decodeFile(gameImage.getFilePath());
            gameImageView.setImageBitmap(bitmap);
        }

    }

    @Override
    public int getItemCount() {
        return gameImages.size();
    }
}
