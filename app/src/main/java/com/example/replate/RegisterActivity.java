package com.example.replate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;


public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    private ImageButton google, facebook, phone;
    private CardView registerbtn, signinbtn;
    private EditText login, mail, password;
    private LinearLayout slideup, googleregistration, phoneregistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        mAuth = FirebaseAuth.getInstance();

        // Konfiguracja Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        google = findViewById(R.id.google);
        facebook = findViewById(R.id.facebook);
        phone = findViewById(R.id.phone);

        googleregistration = findViewById(R.id.googleregistration);
        phoneregistration = findViewById(R.id.phoneregistration);

        googleregistration.setOnClickListener(v->signInWithGoogle());

        LinearLayout slideup = findViewById(R.id.slideup);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideup.setVisibility(View.VISIBLE);
        slideup.startAnimation(slideUp);


        registerbtn = findViewById(R.id.registerbtn);
        signinbtn = findViewById(R.id.signinbtn);
        login = findViewById(R.id.login);
        mail = findViewById(R.id.mail);
        password = findViewById(R.id.password);

        phoneregistration.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_phone_input, null);

            EditText inputEditText = dialogView.findViewById(R.id.inputEditText);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Input your phone number");
            builder.setView(dialogView);
            builder.setPositiveButton("OK", null);  // null, bo nadpiszemy później listener
            builder.setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();

            dialog.setOnShowListener(dialogInterface -> {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                positiveButton.setTextColor(ContextCompat.getColor(this, R.color.brown));
                negativeButton.setTextColor(ContextCompat.getColor(this, R.color.green));

                positiveButton.setOnClickListener(v2 -> {
                    String phoneNumber = inputEditText.getText().toString().trim();

                    if (phoneNumber.isEmpty()) {
                        Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
                        // nie zamykamy dialogu
                        return;
                    }
                    if (!phoneNumber.startsWith("+")) {
                        Toast.makeText(this, "Phone number must start with + and country code", Toast.LENGTH_SHORT).show();
                        // nie zamykamy dialogu
                        return;
                    }

                    try {
                        // Uruchom weryfikację Firebase Phone Auth
                        PhoneAuthOptions options =
                                PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                                        .setPhoneNumber(phoneNumber)       // Numer z prefixem kraju
                                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout
                                        .setActivity(this)
                                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                            @Override
                                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                                FirebaseAuth.getInstance().signInWithCredential(credential)
                                                        .addOnCompleteListener(task -> {
                                                            if (task.isSuccessful()) {
                                                                Toast.makeText(RegisterActivity.this, "Verification successful", Toast.LENGTH_SHORT).show();
                                                                // przejście dalej
                                                            } else {
                                                                Toast.makeText(RegisterActivity.this, "Verification failed", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                            }

                                            @Override
                                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                                Toast.makeText(RegisterActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onCodeSent(@NonNull String verificationId,
                                                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                                super.onCodeSent(verificationId, token);

                                                LayoutInflater inflater = LayoutInflater.from(RegisterActivity.this);
                                                View dialogView = inflater.inflate(R.layout.dialog_code_input, null);
                                                EditText codeInput = dialogView.findViewById(R.id.codeEditText);

                                                AlertDialog.Builder codeBuilder = new AlertDialog.Builder(RegisterActivity.this);
                                                codeBuilder.setTitle("Enter verification code");
                                                codeBuilder.setView(dialogView);
                                                codeBuilder.setPositiveButton("Verify", (dialog1, which1) -> {
                                                    String code = codeInput.getText().toString().trim();
                                                    if (!code.isEmpty()) {
                                                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
                                                        FirebaseAuth.getInstance().signInWithCredential(credential)
                                                                .addOnCompleteListener(task -> {
                                                                    if (task.isSuccessful()) {
                                                                        Toast.makeText(RegisterActivity.this, "Verification successful", Toast.LENGTH_SHORT).show();
                                                                        // przejście dalej do HomeActivity
                                                                        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                                                                        startActivity(intent);
                                                                        finish();  // opcjonalnie zamknij aktualną aktywność
                                                                    } else {
                                                                        Toast.makeText(RegisterActivity.this, "Verification failed", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Please enter the code", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                codeBuilder.setNegativeButton("Cancel", null);

                                                AlertDialog codeDialog = codeBuilder.create();

// Zmiana koloru przycisków po pokazaniu dialogu
                                                codeDialog.setOnShowListener(dialog -> {
                                                    Button positiveButton = codeDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                                    Button negativeButton = codeDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                                                    positiveButton.setTextColor(ContextCompat.getColor(RegisterActivity.this, R.color.brown));
                                                    negativeButton.setTextColor(ContextCompat.getColor(RegisterActivity.this, R.color.green));
                                                });

                                                codeDialog.show();

                                            }

                                        })
                                        .build();

                        PhoneAuthProvider.verifyPhoneNumber(options);

                        dialog.dismiss();  // zamykamy dialog po poprawnym uruchomieniu weryfikacji

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error starting phone verification: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });

            dialog.show();
        });

        registerbtn.setOnClickListener(v -> {
            String email = mail.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Rejestracja udana
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(RegisterActivity.this, "Registered: " + user.getEmail(), Toast.LENGTH_SHORT).show();

                            // Przejście do HomeActivity
                            Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            // Błąd rejestracji
                            Toast.makeText(RegisterActivity.this, "Registration error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("FirebaseAuth", "Register error", task.getException());
                        }
                    });
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

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("GoogleSignIn", "Sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sukces – użytkownik zalogowany
                        FirebaseUser user = mAuth.getCurrentUser();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        // Błąd
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}