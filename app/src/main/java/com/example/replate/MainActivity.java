package com.example.replate;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.os.Handler;

import com.google.firebase.FirebaseApp;

//API AIzaSyAJo3H3Jy6QIlPC2JCJHNzhQTSNBZgPcnE
//SHA-1  A2:95:B8:0D:60:3D:3A:61:41:06:27:9D:45:24:3D:8E:CB:6F:01:45


public class MainActivity extends AppCompatActivity {

    private ImageView logoImage;
    private Handler handler = new Handler(); // Do opóźnienia dużej animacji

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Ukryj pasek statusu
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        FirebaseApp.initializeApp(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logoImage = findViewById(R.id.logoImage);

        // 1. Mała pulsacja
        ScaleAnimation pulseAnimation = new ScaleAnimation(
                1.0f, 1.2f,
                1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        pulseAnimation.setDuration(280);
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
        }, 3000); // czekaj 3 sekundy (czas 5 pulsacji x 0.5s + trochę zapasu)
    }
}
