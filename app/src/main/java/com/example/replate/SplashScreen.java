package com.example.replate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    private ImageView logoImage;
    private Handler handler = new Handler(); // Do opóźnienia dużej animacji

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Ukryj pasek statusu
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        SharedPreferences sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        boolean isFirstRun = sharedPref.getBoolean("isFirstRun", true);
        boolean isLoggedIn = sharedPref.getBoolean("isLoggedIn", false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoImage = findViewById(R.id.logoImage);

        // 1. Mała pulsacja
        ScaleAnimation pulseAnimation = new ScaleAnimation(
                1.0f, 1.2f,
                1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        pulseAnimation.setDuration(200);
        pulseAnimation.setRepeatMode(Animation.REVERSE);
        pulseAnimation.setRepeatCount(5); // 5 powtórzeń (pulsowanie 5 razy)

        logoImage.startAnimation(pulseAnimation);

        // 2. Po zakończeniu pulsowania — duże powiększenie
        handler.postDelayed(() -> {
            ScaleAnimation bigScaleAnimation = new ScaleAnimation(
                    1.0f, 300.0f, // z 1x do 8x rozmiaru!
                    1.0f, 300.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            bigScaleAnimation.setDuration(220); // szybkie powiększenie
            bigScaleAnimation.setFillAfter(true); // zatrzymaj na dużym rozmiarze

            logoImage.startAnimation(bigScaleAnimation);

            Intent intent;
            if (isFirstRun) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("isFirstRun", false);
                editor.apply();

                intent = new Intent(SplashScreen.this, IntroductionActivity.class);
            } else {
                if(isLoggedIn){
                    intent = new Intent(SplashScreen.this, HomeActivity.class);
                    //intent = new Intent(SplashScreen.this, LottieAttempt.class);
                }else{
                    intent = new Intent(SplashScreen.this, LoginActivity.class);
                }
            }
            startActivity(intent);
            finish();
        }, 1400); // czekaj 3 sekundy (czas 5 pulsacji x 0.5s + trochę zapasu)
    }
}
