package com.example.replate.service;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.replate.R;

import java.util.List;
import java.util.Locale;

public class ParcelImageService {

    private final Context context;

    public ParcelImageService(Context context) {
        this.context = context;
    }

    public void loadParcelImage(ImageView imageView, String imageUrl, List<String> tags) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.photo_bakery)
                    .error(R.drawable.photo_bakery)
                    .into(imageView);
        } else {
            // Jeśli brak zdjęcia, ustaw na podstawie tagu
            int imageResource = getImageResourceForTag(tags);
            imageView.setImageResource(imageResource);
        }
    }

    private int getImageResourceForTag(List<String> tags) {
        if (tags != null && tags.size() == 1) {
            switch (tags.get(0).toLowerCase(Locale.ROOT)) {
                case "sweets": return R.drawable.photo_sweets;
                case "fast food": return R.drawable.photo_fastfood;
                case "bakery": return R.drawable.photo_bakery;
                case "dairy": return R.drawable.photo_dairy;
                case "vegetables": return R.drawable.photo_vegetables;
                case "vegetarian": return R.drawable.photo_vegetarian;
                case "vegan": return R.drawable.photo_vegan;
                case "fruits": return R.drawable.photo_fruits;
                case "liquids": return R.drawable.photo_liquids;
                case "meals": return R.drawable.photo_meal;
                case "meat": return R.drawable.photo_meat;
                default: return R.drawable.photo_mixed;
            }
        } else {
            return R.drawable.photo_mixed;
        }
    }
}