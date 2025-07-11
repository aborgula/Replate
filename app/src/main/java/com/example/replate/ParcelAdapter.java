package com.example.replate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ParcelAdapter extends RecyclerView.Adapter<ParcelAdapter.ParcelViewHolder> {

    private Context context;
    private List<Parcel> parcelList;
    private OnDeleteClickListener deleteClickListener;
    private String currentUserId;
    private OnParcelClickListener parcelClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Parcel parcel);
    }

    public interface OnParcelClickListener {
        void onParcelClick(Parcel parcel);
    }

    public ParcelAdapter(Context context, List<Parcel> parcelList, String currentUserId,
                         OnDeleteClickListener deleteClickListener,
                         OnParcelClickListener parcelClickListener) {
        this.context = context;
        this.parcelList = parcelList;
        this.currentUserId = currentUserId;
        this.deleteClickListener = deleteClickListener;
        this.parcelClickListener = parcelClickListener;
    }

    @NonNull
    @Override
    public ParcelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_order, parent, false);
        return new ParcelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParcelViewHolder holder, int position) {
        Parcel parcel = parcelList.get(position);
        holder.title.setText(parcel.getTitle());

        // Dekodowanie zdjęcia
        if (parcel.getImageBase64() != null && !parcel.getImageBase64().isEmpty()) {
            try {
                String base64 = parcel.getImageBase64();
                if (base64.contains(",")) {
                    base64 = base64.substring(base64.indexOf(",") + 1);
                }
                byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.image.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.image.setImageResource(R.drawable.photo_mixed);
            }
        } else {
            holder.image.setImageResource(R.drawable.photo_mixed);
        }

        // Ukryj "Delete", jeśli paczka nie należy do aktualnego użytkownika
        if (!parcel.getUserId().equals(currentUserId)) {
            holder.deleteButton.setVisibility(View.GONE);
        } else {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> deleteClickListener.onDeleteClick(parcel));
        }

        // Obsługa kliknięcia paczki
// Kliknięcie działa tylko, jeśli to nie Twoja paczka (czyli nie ma przycisku Delete)
        if (!parcel.getUserId().equals(currentUserId)) {
            holder.itemView.setOnClickListener(v -> {
                parcelClickListener.onParcelClick(parcel);
            });
        } else {
            holder.itemView.setOnClickListener(null); // lub brak reakcji
        }

    }

    @Override
    public int getItemCount() {
        return parcelList.size();
    }

    static class ParcelViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView image;
        Button deleteButton;

        public ParcelViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.itemTitle);
            image = itemView.findViewById(R.id.itemImage);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}

