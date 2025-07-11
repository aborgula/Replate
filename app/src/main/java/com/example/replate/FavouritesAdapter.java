package com.example.replate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class FavouritesAdapter extends RecyclerView.Adapter<FavouritesAdapter.ViewHolder> {

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
        holder.usernameView.setText(user.getUsername());

        Double rating = user.getRating();
        if (rating == 0.0) {
            holder.ratingView.setText("No ratings");
        } else {
            holder.ratingView.setText(String.format("%.1f", rating));
        }

        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(context).load(user.getPhotoUrl()).into(holder.imageView);
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


