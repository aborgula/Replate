package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class SummaryActivity extends AppCompatActivity {

    String parcelId = getIntent().getStringExtra("parcelId");
    String title = getIntent().getStringExtra("title");
    String description = getIntent().getStringExtra("description");
    String startTime = getIntent().getStringExtra("startTime");
    String endTime = getIntent().getStringExtra("endTime");
    String day = getIntent().getStringExtra("day");
    String address = getIntent().getStringExtra("address");
    double price = getIntent().getDoubleExtra("price", 0.0);
    String creatorId = getIntent().getStringExtra("creatorId");

// Teraz możesz wyświetlić dane w UI, np. w TextView-ach

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
    }
}