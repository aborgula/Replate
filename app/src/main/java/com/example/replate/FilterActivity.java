package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;
import java.util.List;

public class FilterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        MaterialButton todayfltr = findViewById(R.id.todayfltr);
        MaterialButton tomorrowfltr = findViewById(R.id.tomorrowfltr);
        MaterialButton filterbtn = findViewById(R.id.filterbtn);

        final boolean[] todaySelected = {false};
        final boolean[] tomorrowSelected = {false};
        int defaultColor = getResources().getColor(R.color.hint_color);
        int colorBrown = getResources().getColor(R.color.brown);
        int defaultColorWhite = getResources().getColor(R.color.white);

        RangeSlider timeSlider = findViewById(R.id.timeRangeSlider);

        timeSlider.setValues(480f, 1200f);
        timeSlider.setLabelBehavior(LabelFormatter.LABEL_FLOATING);

        timeSlider.setTrackActiveTintList(ContextCompat.getColorStateList(this, R.color.dark_green));
        timeSlider.setTrackInactiveTintList(ColorStateList.valueOf(Color.LTGRAY));
        timeSlider.setThumbTintList(ContextCompat.getColorStateList(this, R.color.gold));
        timeSlider.setHaloTintList(ContextCompat.getColorStateList(this, R.color.light_green));
        timeSlider.setTickTintList(ColorStateList.valueOf(Color.YELLOW));
        timeSlider.setLabelFormatter(value -> {
            int totalMinutes = Math.round(value);
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            return String.format("%02d:%02d", hours, minutes);
        });

        timeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int startMin = Math.round(values.get(0));
            int endMin = Math.round(values.get(1));

            String start = String.format("%02d:%02d", startMin / 60, startMin % 60);
            String end = String.format("%02d:%02d", endMin / 60, endMin % 60);

            Log.d("TimeRange", "Selected: " + start + " - " + end);
        });

        todayfltr.setOnClickListener(v -> {
            todaySelected[0] = !todaySelected[0]; // toggle state
            if (todaySelected[0]) {
                setFilterButtonActive(todayfltr, colorBrown, getResources().getColor(android.R.color.white));
                tomorrowSelected[0] = false;
                setFilterButtonInactive(tomorrowfltr, defaultColorWhite, colorBrown);
            } else {
                setFilterButtonInactive(todayfltr, defaultColorWhite, colorBrown);
            }
        });

        tomorrowfltr.setOnClickListener(v -> {
            tomorrowSelected[0] = !tomorrowSelected[0]; // toggle state
            if (tomorrowSelected[0]) {
                setFilterButtonActive(tomorrowfltr, colorBrown, getResources().getColor(android.R.color.white));
                todaySelected[0] = false;
                setFilterButtonInactive(todayfltr, defaultColorWhite, colorBrown);
            } else {
                setFilterButtonInactive(tomorrowfltr, defaultColorWhite, colorBrown);
            }
        });

        filterbtn.setOnClickListener(v -> {
            // Zapisz datę tylko jeśli któryś jest zaznaczony
            if (todaySelected[0]) {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayDate = sdf.format(calendar.getTime());
                saveSelectedDateToPrefs("selectedDate", todayDate);
            } else if (tomorrowSelected[0]) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String tomorrowDate = sdf.format(calendar.getTime());
                saveSelectedDateToPrefs("selectedDate", tomorrowDate);
            } else {
                // Jeśli nic nie zaznaczone – usuń filtr daty
                saveSelectedDateToPrefs("selectedDate", "");
            }

            // Start HomeActivity z informacją, żeby pokazać PickupFragment
            Intent intent = new Intent(FilterActivity.this, HomeActivity.class);
           // intent.putExtra("showPickupFragment", true);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });


        ImageView arrowback = findViewById(R.id.arrowback);
        TextView resetbtn = findViewById(R.id.resetbtn);

        CheckBox[] checkBoxes = new CheckBox[]{
                findViewById(R.id.sweets),
                findViewById(R.id.fastfoods),
                findViewById(R.id.meat),
                findViewById(R.id.meals),
                findViewById(R.id.vegetables),
                findViewById(R.id.fruits),
                findViewById(R.id.mixed),
                findViewById(R.id.vegetarian),
                findViewById(R.id.dairy),
                findViewById(R.id.bakery),
                findViewById(R.id.liquids),
                findViewById(R.id.vegan)
        };

        for (CheckBox cb : checkBoxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    cb.setButtonTintList(ColorStateList.valueOf(colorBrown));
                }else{
                    cb.setButtonTintList(ColorStateList.valueOf(defaultColor));

                }
            });
        }

        arrowback.setOnClickListener(v -> {
            onBackPressed();
        });


        resetbtn.setOnClickListener(v -> {
            for (CheckBox cb : checkBoxes) {
                cb.setChecked(false);
                cb.setButtonTintList(ColorStateList.valueOf(defaultColor));
            }
        });


    }

    private void setFilterButtonActive(MaterialButton button, int bgColor, int textColor) {
        button.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        button.setTextColor(textColor);
    }

    private void setFilterButtonInactive(MaterialButton button, int bgColor, int textColor) {
        button.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        button.setTextColor(textColor);
    }

    private void saveSelectedDateToPrefs(String key, String value) {
        Log.d("rrr", "value : "+ value);
        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .edit()
                .putString(key, value)
                .apply();
    }


}