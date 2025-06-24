package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
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

import java.text.SimpleDateFormat;
import java.util.*;

public class FilterActivity extends AppCompatActivity {

    private final Map<CheckBox, String> checkboxTagMap = new HashMap<>();
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private int defaultColor;
    private int colorBrown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        MaterialButton todayfltr = findViewById(R.id.todayfltr);
        MaterialButton tomorrowfltr = findViewById(R.id.tomorrowfltr);
        MaterialButton filterbtn = findViewById(R.id.filterbtn);
        ImageView arrowback = findViewById(R.id.arrowback);
        TextView resetbtn = findViewById(R.id.resetbtn);

        defaultColor = getResources().getColor(R.color.hint_color);
        colorBrown = getResources().getColor(R.color.brown);
        int defaultColorWhite = getResources().getColor(R.color.white);

        // Slider
        RangeSlider timeSlider = findViewById(R.id.timeRangeSlider);
        timeSlider.setTrackActiveTintList(ContextCompat.getColorStateList(this, R.color.dark_green));
        timeSlider.setTrackInactiveTintList(ColorStateList.valueOf(Color.LTGRAY));
        timeSlider.setThumbTintList(ContextCompat.getColorStateList(this, R.color.gold));
        timeSlider.setHaloTintList(ContextCompat.getColorStateList(this, R.color.light_green));
        timeSlider.setTickTintList(ColorStateList.valueOf(Color.YELLOW));
        timeSlider.setLabelFormatter(value -> String.format("%02d:%02d", (int)value / 60, (int)value % 60));

        // Wczytaj z prefs czas
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int startTime = prefs.getInt("startTimeMinutes", 0);
        int endTime = prefs.getInt("endTimeMinutes", 1439);
        timeSlider.setValues((float) startTime, (float) endTime);

        // Tymczasowe zmienne do przechowania wybranych wartości
        final int[] tempStartTime = {startTime};
        final int[] tempEndTime = {endTime};

        timeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            tempStartTime[0] = Math.round(values.get(0));
            tempEndTime[0] = Math.round(values.get(1));
        });

        // Checkboxy
        addCheckbox(R.id.sweets, "Sweets");
        addCheckbox(R.id.fastfoods, "Fast food");
        addCheckbox(R.id.meat, "Meat");
        addCheckbox(R.id.meals, "Meals");
        addCheckbox(R.id.vegetables, "Vegetables");
        addCheckbox(R.id.fruits, "Fruits");
        addCheckbox(R.id.mixed, "Mixed");
        addCheckbox(R.id.vegetarian, "Vegetarian");
        addCheckbox(R.id.dairy, "Dairy");
        addCheckbox(R.id.bakery, "Bakery");
        addCheckbox(R.id.liquids, "Liquids");
        addCheckbox(R.id.vegan, "Vegan");

        Set<String> savedTags = prefs.getStringSet("selectedChips", new HashSet<>());
        for (Map.Entry<CheckBox, String> entry : checkboxTagMap.entrySet()) {
            CheckBox cb = entry.getKey();
            String tag = entry.getValue();

            cb.setChecked(savedTags.contains(tag));
            cb.setButtonTintList(ColorStateList.valueOf(cb.isChecked() ? colorBrown : defaultColor));

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                cb.setButtonTintList(ColorStateList.valueOf(isChecked ? colorBrown : defaultColor));
              //  saveCheckedTagsToPrefs();
            });
        }

        // Back
        arrowback.setOnClickListener(v -> onBackPressed());

        // Data
        final boolean[] todaySelected = {false};
        final boolean[] tomorrowSelected = {false};

        String selectedDate = prefs.getString("selectedDate", null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String todayDate = sdf.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrowDate = sdf.format(calendar.getTime());

        if (selectedDate == null || selectedDate.equals(todayDate)) {
            todaySelected[0] = true;
            setFilterButtonActive(todayfltr, colorBrown, defaultColorWhite);
            setFilterButtonInactive(tomorrowfltr, defaultColorWhite, colorBrown);
        } else if (selectedDate.equals(tomorrowDate)) {
            tomorrowSelected[0] = true;
            setFilterButtonActive(tomorrowfltr, colorBrown, defaultColorWhite);
            setFilterButtonInactive(todayfltr, defaultColorWhite, colorBrown);
        }

        // Reset
        resetbtn.setOnClickListener(v -> {
            // Odznacz wszystkie checkboxy i zmień kolor
            for (CheckBox cb : checkBoxes) {
                cb.setChecked(false);
                cb.setButtonTintList(ColorStateList.valueOf(defaultColor));
            }

            // Reset godzin na domyślne (00:00 - 23:59)
            timeSlider.setValues(0f, 1439f);
            tempStartTime[0] = 0;
            tempEndTime[0] = 1439;

            // Ustaw dzień na "today"
            todaySelected[0] = true;
            tomorrowSelected[0] = false;
            setFilterButtonActive(todayfltr, colorBrown, defaultColorWhite);
            setFilterButtonInactive(tomorrowfltr, defaultColorWhite, colorBrown);
        });

        todayfltr.setOnClickListener(v -> {
            todaySelected[0] = true;
            tomorrowSelected[0] = false;
            setFilterButtonActive(todayfltr, colorBrown, defaultColorWhite);
            setFilterButtonInactive(tomorrowfltr, defaultColorWhite, colorBrown);
        });

        tomorrowfltr.setOnClickListener(v -> {
            todaySelected[0] = false;
            tomorrowSelected[0] = true;
            setFilterButtonActive(tomorrowfltr, colorBrown, defaultColorWhite);
            setFilterButtonInactive(todayfltr, defaultColorWhite, colorBrown);
        });

        // Zapisz filtry i wróć do HomeActivity
        filterbtn.setOnClickListener(v -> {
            // Zapisz datę
            if (todaySelected[0]) {
                prefs.edit().putString("selectedDate", todayDate).apply();
            } else if (tomorrowSelected[0]) {
                prefs.edit().putString("selectedDate", tomorrowDate).apply();
            } else {
                prefs.edit().remove("selectedDate").apply();
            }

            // Zapisz tagi
            saveCheckedTagsToPrefs();

            // Zapisz godzinę
            prefs.edit()
                    .putInt("startTimeMinutes", tempStartTime[0])
                    .putInt("endTimeMinutes", tempEndTime[0])
                    .apply();

            // Powrót
            startActivity(new Intent(FilterActivity.this, HomeActivity.class));
            finish();
        });
    }

    private void addCheckbox(int id, String tag) {
        CheckBox cb = findViewById(id);
        checkboxTagMap.put(cb, tag);
        checkBoxes.add(cb);
    }

    private void saveCheckedTagsToPrefs() {
        Set<String> selected = new HashSet<>();
        for (Map.Entry<CheckBox, String> entry : checkboxTagMap.entrySet()) {
            if (entry.getKey().isChecked()) {
                selected.add(entry.getValue());
            }
        }
        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .edit()
                .putStringSet("selectedChips", selected)
                .apply();
        Log.d("selectedtags", "Zapisano tagi: " + selected);
    }

    private void setFilterButtonActive(MaterialButton button, int bgColor, int textColor) {
        button.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        button.setTextColor(textColor);
    }

    private void setFilterButtonInactive(MaterialButton button, int bgColor, int textColor) {
        button.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        button.setTextColor(textColor);
    }
}
