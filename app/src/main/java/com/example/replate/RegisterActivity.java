package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;

public class RegisterActivity extends AppCompatActivity {

    private CardView registerbtn, signinbtn;
    private EditText login, mail, password;
    private LinearLayout slideup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        LinearLayout slideup = findViewById(R.id.slideup);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideup.setVisibility(View.VISIBLE);
        slideup.startAnimation(slideUp);


        registerbtn = findViewById(R.id.registerbtn);
        signinbtn = findViewById(R.id.signinbtn);
        login = findViewById(R.id.login);
        mail = findViewById(R.id.mail);
        password = findViewById(R.id.password);

        registerbtn.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        signinbtn.setOnClickListener(v -> {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            slideup.startAnimation(slideDown);
            slideup.setVisibility(View.GONE);


            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        });


    }
}