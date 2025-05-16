package com.example.replate;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.List;

public class ChooseLocationRangeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_location_range);

        EditText searchcity = findViewById(R.id.searchcity);
        TextView usecurrentlocation = findViewById(R.id.usecurrentlocation);
        CardView showresultbtn = findViewById(R.id.showresultbtn);

        ImageButton closebtn = findViewById(R.id.closebtn);
        closebtn.setOnClickListener(v->{
            finish();
        });


        Slider distanceSlider = findViewById(R.id.distanceSlider);

        int savedKm = getSharedPreferences("replate_prefs", MODE_PRIVATE)
                .getInt("selected_distance_km", 5);
        distanceSlider.setValue(savedKm);

        distanceSlider.setTrackActiveTintList(ContextCompat.getColorStateList(this, R.color.green));
        distanceSlider.setTrackInactiveTintList(ColorStateList.valueOf(Color.LTGRAY));
        distanceSlider.setThumbTintList(ContextCompat.getColorStateList(this, R.color.black));
        distanceSlider.setHaloTintList(ContextCompat.getColorStateList(this, R.color.green));
        distanceSlider.setTickTintList(ColorStateList.valueOf(Color.YELLOW));

        // Label formatter, np. pokazuje "5 km"
        distanceSlider.setLabelFormatter(value -> ((int) value) + " km");

        distanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            int selectedKm = Math.round(value);

            getSharedPreferences("replate_prefs", MODE_PRIVATE)
                    .edit()
                    .putInt("selected_distance_km", selectedKm)
                    .apply();
        });



    }
}