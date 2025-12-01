package com.example.replate.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapMarkerService {

    private final Context context;
    private final GoogleMap googleMap;

    public MapMarkerService(Context context, GoogleMap googleMap) {
        this.context = context;
        this.googleMap = googleMap;
    }

    public void addMarkerWithImage(LatLng latLng, String title, String parcelId, String imageUrl) {
        if (googleMap == null || latLng == null) {
            return;
        }

        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .circleCrop()
                .into(new CustomTarget<Bitmap>(70, 70) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(latLng)
                                .title(title)
                                .icon(BitmapDescriptorFactory.fromBitmap(resource));

                        Marker marker = googleMap.addMarker(markerOptions);
                        if (marker != null) {
                            marker.setTag(parcelId);
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }
}