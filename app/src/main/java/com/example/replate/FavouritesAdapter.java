package com.example.replate;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.replate.Model.User;

import java.util.List;

public class FavouritesAdapter extends RecyclerView.Adapter<FavouritesAdapter.ViewHolder> {

    private static final String TAG = "FavouritesAdapter";
    private List<User> users;
    private Context context;
    private OnUserClickListener clickListener;


    public interface OnUserRemovedListener {
        void onUserRemoved(User user, int position);
    }

    private OnUserRemovedListener listener;

    public FavouritesAdapter(List<User> users, Context context,
                             OnUserRemovedListener listener,
                             OnUserClickListener clickListener) {
        this.users = users;
        this.context = context;
        this.listener = listener;
        this.clickListener = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameView;
        TextView ratingView;
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            usernameView = itemView.findViewById(R.id.userLogin);
            ratingView = itemView.findViewById(R.id.userRating);
            imageView = itemView.findViewById(R.id.userImage);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onUserClick(users.get(position));
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public FavouritesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_favourite_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavouritesAdapter.ViewHolder holder, int position) {
        User user = users.get(position);
        Log.d(TAG, "onBindViewHolder: position=" + position + ", username=" + user.getUsername());

        holder.usernameView.setText(user.getUsername());

        Double rating = user.getRating();
        if (rating == 0.0) {
            holder.ratingView.setText("No ratings");
        } else {
            holder.ratingView.setText(String.format("%.1f", rating));
        }

        // Załaduj zdjęcie z Base64
        loadUserImage(holder.imageView, user.getProfileImageBase64());
    }

    private void loadUserImage(ImageView imageView, String profileImageBase64) {
        Log.d(TAG, "loadUserImage: profileImageBase64 is " +
                (profileImageBase64 == null ? "null" :
                        profileImageBase64.isEmpty() ? "empty" :
                                "present (length: " + profileImageBase64.length() + ")"));

        if (profileImageBase64 == null || profileImageBase64.isEmpty()) {
            Log.d(TAG, "loadUserImage: Setting default profile image");
            imageView.setImageResource(R.drawable.photo_defaultprofile);
            return;
        }

        try {
            // Usuń prefix "data:image/jpeg;base64," jeśli istnieje
            String base64String = profileImageBase64;
            if (profileImageBase64.contains(",")) {
                Log.d(TAG, "loadUserImage: Removing base64 prefix");
                base64String = profileImageBase64.split(",")[1];
            }

            Log.d(TAG, "loadUserImage: Decoding base64 string (length: " + base64String.length() + ")");

            // Dekoduj Base64 do byte array
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

            Log.d(TAG, "loadUserImage: Decoded bytes length: " + decodedBytes.length);

            // Użyj Glide do załadowania obrazu z byte array
            Glide.with(context)
                    .load(decodedBytes)
                    .placeholder(R.drawable.photo_defaultprofile)
                    .error(R.drawable.photo_defaultprofile)
                    .centerCrop()
                    .into(imageView);

            Log.d(TAG, "loadUserImage: Glide load completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "loadUserImage: Error loading image", e);
            e.printStackTrace();
            imageView.setImageResource(R.drawable.photo_defaultprofile);
        }
    }


    @Override
    public int getItemCount() {
        return users.size();
    }

    public void removeItem(int position) {
        User user = users.get(position);
        users.remove(position);
        notifyItemRemoved(position);
        if (listener != null) listener.onUserRemoved(user, position);
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
    }
}