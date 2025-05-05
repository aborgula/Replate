package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

public class IntroductionActivity extends AppCompatActivity {
    private CardView nextButton;
    private CircleImageView foodImage;
    private int currentSlide = 1;
    private ImageView arrows;
    private TextView pickupTitle, pickupDescription, firstdot, seconddot, lastdot, getstarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduction);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        nextButton = findViewById(R.id.nextButton);
        foodImage = findViewById(R.id.foodImage);
        pickupTitle = findViewById(R.id.pickupTitle);
        pickupDescription = findViewById(R.id.pickupDescription);
        firstdot = findViewById(R.id.firstdot);
        seconddot = findViewById(R.id.seconddot);
        lastdot = findViewById(R.id.lastdot);
        arrows = findViewById(R.id.arrows);
        getstarted = findViewById(R.id.getstarted);

        nextButton.setOnClickListener(v -> {
            if(currentSlide == 1){
                currentSlide = 2;
                foodImage.setImageResource(R.drawable.plate3);
                pickupTitle.setText("Give what you won’t eat");
                pickupDescription.setText("List food you won’t use and let someone else enjoy it instead of throwing it away.");
                firstdot.setTextColor(Color.parseColor("#45000000"));
                seconddot.setTextColor(Color.BLACK);
            }else if(currentSlide == 2){
                currentSlide = 3;
                foodImage.setImageResource(R.drawable.plate2);
                pickupTitle.setText("Waste less. Help more.");
                pickupDescription.setText("Together we fight food waste and build a community around sharing.");
                seconddot.setTextColor(Color.parseColor("#45000000"));
                lastdot.setTextColor(Color.BLACK);
                getstarted.setVisibility(View.VISIBLE);
                arrows.setVisibility(View.GONE);
            }else if(currentSlide == 3){
                Intent intent;
                intent = new Intent(IntroductionActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();

            }
        });
    }
}