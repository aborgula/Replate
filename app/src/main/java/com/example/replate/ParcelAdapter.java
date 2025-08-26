package com.example.replate;

import android.app.AlertDialog;
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

import com.google.firebase.firestore.FirebaseFirestore;
import com.ncorti.slidetoact.SlideToActView;

import java.util.List;

public class ParcelAdapter extends RecyclerView.Adapter<ParcelAdapter.ParcelViewHolder> {

    private Context context;
    private List<Parcel> parcelList;
    private OnDeleteClickListener deleteClickListener;
    private OnParcelClickListener parcelClickListener;
    private OnCancelClickListener cancelClickListener;
    private String currentUserId;
    private boolean isOwnerProfile; // 🔑 nowa flaga

    public interface OnDeleteClickListener {
        void onDeleteClick(Parcel parcel);
    }

    public interface OnParcelClickListener {
        void onParcelClick(Parcel parcel);
    }

    public interface OnCancelClickListener {
        void onCancelClick(Parcel parcel);
    }

    public ParcelAdapter(Context context, List<Parcel> parcelList, String currentUserId,
                         boolean isOwnerProfile,
                         OnDeleteClickListener deleteClickListener,
                         OnCancelClickListener cancelClickListener,
                         OnParcelClickListener parcelClickListener) {
        this.context = context;
        this.parcelList = parcelList;
        this.currentUserId = currentUserId;
        this.isOwnerProfile = isOwnerProfile; // 🔑
        this.deleteClickListener = deleteClickListener;
        this.cancelClickListener = cancelClickListener;
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
                if (base64.contains(",")) base64 = base64.substring(base64.indexOf(",") + 1);
                byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.image.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.image.setImageResource(R.drawable.photo_mixed);
            }
        } else {
            holder.image.setImageResource(R.drawable.photo_mixed);
        }

        // Logika slidów
        if (isOwnerProfile) {
            // Właściciel paczki -> pokazujemy slidery
            if ("inactive".equals(parcel.getStatus())) {
                holder.pickupSlide.setVisibility(View.GONE);
                holder.pickedupSlide.setVisibility(View.VISIBLE);

                if ("yes".equals(parcel.getOwnerConfirmed())) {
                    holder.pickedupSlide.setCompleted(true, false);
                    holder.pickedupSlide.setEnabled(false);
                } else {
                    holder.pickedupSlide.setCompleted(false, false);
                    holder.pickedupSlide.setEnabled(true);
                    holder.pickedupSlide.setOnSlideCompleteListener(slide -> {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("parcels").document(parcel.getId())
                                .update("ownerconfirmed", "yes")
                                .addOnSuccessListener(aVoid -> {
                                    holder.pickedupSlide.setCompleted(true, false);
                                    holder.pickedupSlide.setEnabled(false);

                                    AlertDialog.Builder innerBuilder = new AlertDialog.Builder(context);
                                    View innerView = LayoutInflater.from(context)
                                            .inflate(R.layout.marked_pickup_window, null);
                                    innerBuilder.setView(innerView);

                                    AlertDialog innerDialog = innerBuilder.create();
                                    innerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                                    Button btnOk = innerView.findViewById(R.id.btnOk);
                                    btnOk.setOnClickListener(v2 -> innerDialog.dismiss());

                                    innerDialog.show();
                                })
                                .addOnFailureListener(e -> holder.pickedupSlide.resetSlider());
                    });
                }
            } else {
                holder.pickupSlide.setVisibility(View.GONE);
                holder.pickedupSlide.setVisibility(View.GONE);
            }
        } else {
            // Cudzy profil -> ukrywamy wszystkie slidery
            holder.pickupSlide.setVisibility(View.GONE);
            holder.pickedupSlide.setVisibility(View.GONE);
        }

        // Delete / Cancel w zależności od właściciela paczki i czy to właściciel profilu
        if (isOwnerProfile && parcel.getUserId().equals(currentUserId)) {
            holder.deleteButton.setText("Delete");
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> deleteClickListener.onDeleteClick(parcel));
        } else if (isOwnerProfile && !parcel.getUserId().equals(currentUserId)) {
            holder.deleteButton.setText("Cancel");
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> cancelClickListener.onCancelClick(parcel));
        } else {
            // Cudzy profil -> ukrywamy przycisk
            holder.deleteButton.setVisibility(View.GONE);
        }

        // Kliknięcie w cały item
        holder.itemView.setOnClickListener(v -> parcelClickListener.onParcelClick(parcel));
    }

    @Override
    public int getItemCount() {
        return parcelList.size();
    }

    static class ParcelViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView image;
        Button deleteButton;
        SlideToActView pickupSlide, pickedupSlide;

        public ParcelViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.itemTitle);
            image = itemView.findViewById(R.id.itemImage);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            pickupSlide = itemView.findViewById(R.id.pickupSlide);
            pickedupSlide = itemView.findViewById(R.id.pickedupSlide);
        }
    }
}
