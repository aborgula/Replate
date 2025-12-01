package com.example.replate;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;

    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.reviewScoreCircle.setText(String.valueOf(review.getValue()));

        // Dynamiczne ustawienie koloru w zależności od wartości
        switch (review.getValue()) {
            case 1:
                holder.reviewScoreCircle.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#E31117"))); // czerwony
                break;
            case 2:
                holder.reviewScoreCircle.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.brown)));
                break;
            case 3:
                holder.reviewScoreCircle.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.yellow)));
                break;
            case 4:
                holder.reviewScoreCircle.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.gold)));
                break;
            case 5:
                holder.reviewScoreCircle.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.green)));
                break;
            default:
                holder.reviewScoreCircle.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.green)));
                break;
        }


        holder.reviewText.setText(review.getDescription());
        holder.reviewAuthorName.setText("- " + (review.getAuthorLogin() != null ? review.getAuthorLogin() : review.getUserId()));

        holder.parcelTitle.setText(review.getParcelTitle() != null ? review.getParcelTitle() : "No title");

    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView reviewScoreCircle, reviewText, reviewAuthorName, parcelTitle;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            reviewScoreCircle = itemView.findViewById(R.id.reviewScoreCircle);
            reviewText = itemView.findViewById(R.id.reviewText);
            reviewAuthorName = itemView.findViewById(R.id.reviewAuthorName);
            parcelTitle = itemView.findViewById(R.id.parcelTitle);
        }
    }
}
