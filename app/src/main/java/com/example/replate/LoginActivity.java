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

public class LoginActivity extends AppCompatActivity {

    private CardView registerbtn, signinbtn;
    private EditText  mail, password;
    private LinearLayout slidein;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        LinearLayout slidein = findViewById(R.id.slidein);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slidein.setVisibility(View.VISIBLE);
        slidein.startAnimation(slideUp);

        registerbtn = findViewById(R.id.registerbtn);
        signinbtn = findViewById(R.id.signinbtn);
        mail = findViewById(R.id.mail);
        password = findViewById(R.id.password);

        registerbtn.setOnClickListener(v -> {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            slidein.startAnimation(slideDown);
            slidein.setVisibility(View.GONE);


            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
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