/**
 * package com.example.replate;
 * <p>
 * import androidx.appcompat.app.AppCompatActivity;
 * import androidx.cardview.widget.CardView;
 * <p>
 * import android.content.Intent;
 * import android.os.Bundle;
 * import android.view.View;
 * import android.view.animation.Animation;
 * import android.view.animation.AnimationUtils;
 * import android.widget.EditText;
 * import android.widget.LinearLayout;
 * <p>
 * public class LoginActivity extends AppCompatActivity {
 * <p>
 * private CardView registerbtn, signinbtn;
 * private EditText  mail, password;
 * private LinearLayout slidein, googleregistration, phoneregistration;
 *
 * @Override protected void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState);
 * setContentView(R.layout.activity_login);
 * getWindow().getDecorView().setSystemUiVisibility(
 * android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
 * );
 * <p>
 * LinearLayout slidein = findViewById(R.id.slidein);
 * Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
 * slidein.setVisibility(View.VISIBLE);
 * slidein.startAnimation(slideUp);
 * <p>
 * registerbtn = findViewById(R.id.registerbtn);
 * signinbtn = findViewById(R.id.signinbtn);
 * mail = findViewById(R.id.mail);
 * password = findViewById(R.id.password);
 * googleregistration = findViewById(R.id.googleregistration);
 * phoneregistration = findViewById(R.id.phoneregistration);
 * <p>
 * registerbtn.setOnClickListener(v -> {
 * Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
 * slidein.startAnimation(slideDown);
 * slidein.setVisibility(View.GONE);
 * <p>
 * <p>
 * slideDown.setAnimationListener(new Animation.AnimationListener() {
 * @Override public void onAnimationStart(Animation animation) {
 * }
 * @Override public void onAnimationEnd(Animation animation) {
 * Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
 * startActivity(intent);
 * overridePendingTransition(0, 0);
 * finish();
 * }
 * @Override public void onAnimationRepeat(Animation animation) {
 * }
 * });
 * });
 * }
 * }
 **/
package com.example.replate;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private CardView registerbtn, signinbtn;
    private EditText mail, password;
    private LinearLayout slidein, googleregistration, phoneregistration;

    private FirebaseAuth mAuth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        // Firebase auth instance
        mAuth = FirebaseAuth.getInstance();

        // UI initialization
        slidein = findViewById(R.id.slidein);
        registerbtn = findViewById(R.id.registerbtn);
        signinbtn = findViewById(R.id.signinbtn);
        mail = findViewById(R.id.mail);
        password = findViewById(R.id.password);
        googleregistration = findViewById(R.id.googleregistration);
        phoneregistration = findViewById(R.id.phoneregistration);

        // Slide up animation
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slidein.setVisibility(View.VISIBLE);
        slidein.startAnimation(slideUp);

        // Register button - slide down and switch activity
        registerbtn.setOnClickListener(v -> {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            slidein.startAnimation(slideDown);
            slidein.setVisibility(View.GONE);

            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        });

        // Sign-in button (email & password)
        signinbtn.setOnClickListener(v -> signInWithEmail());

        // Google login
        googleregistration.setOnClickListener(v -> startGoogleLogin());

        // Phone login
        phoneregistration.setOnClickListener(v -> startPhoneLogin());
    }

    // Email & password sign-in
    private void signInWithEmail() {
        String email = mail.getText().toString().trim();
        String pass = password.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            db.collection("users").document(userId)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String login = documentSnapshot.getString("login");
                                            if (login != null && !login.isEmpty()) {
                                                Toast.makeText(this, "Welcome, " + login + "!", Toast.LENGTH_LONG).show();
                                            }

                                            // 🔐 Pobierz FCM token i zaktualizuj w Firestore
                                            FirebaseMessaging.getInstance().getToken()
                                                    .addOnCompleteListener(tokenTask -> {
                                                        if (tokenTask.isSuccessful()) {
                                                            String token = tokenTask.getResult();
                                                            Map<String, Object> updates = new HashMap<>();
                                                            updates.put("fcmToken", token);

                                                            // Jeśli pole notificationsEnabled nie istnieje, ustaw na true
                                                            if (!documentSnapshot.contains("notificationsEnabled")) {
                                                                updates.put("notificationsEnabled", true);
                                                            }

                                                            db.collection("users").document(userId)
                                                                    .update(updates)
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        // Przejdź dalej po sukcesie
                                                                        goToMain();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(this, "Failed to update token", Toast.LENGTH_SHORT).show();
                                                                    });
                                                        } else {
                                                            Toast.makeText(this, "Couldn't get FCM token", Toast.LENGTH_SHORT).show();
                                                            goToMain();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                                        }

                                        // Zapisz lokalnie że zalogowany
                                        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                                                .edit()
                                                .putBoolean("isLoggedIn", true)
                                                .apply();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error reading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                           // goToMain(); // fallback
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


    // Google sign-in
    private void startGoogleLogin() {
        oneTapClient = Identity.getSignInClient(this);

        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id)) // from google-services.json
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .setAutoSelectEnabled(true) // Optional: auto-select if only one account
                .build();

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(result -> {
                    try {
                        IntentSenderRequest request = new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build();
                        googleLoginLauncher.launch(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Couldn't launch Google Sign-In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private final ActivityResultLauncher<IntentSenderRequest> googleLoginLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        SignInCredential credential = Identity.getSignInClient(this)
                                .getSignInCredentialFromIntent(result.getData());
                        String idToken = credential.getGoogleIdToken();

                        if (idToken != null) {
                            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                            FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(this, "Google sign-in successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(this, HomeActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(this, "Firebase sign-in failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "No ID token found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Sign-in error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });


    // Phone login
    private void startPhoneLogin() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_phone_input, null);

        EditText inputEditText = dialogView.findViewById(R.id.inputEditText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Login with your phone number");
        builder.setView(dialogView);
        builder.setPositiveButton("OK", null);
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
                    return;
                }
                if (!phoneNumber.startsWith("+")) {
                    Toast.makeText(this, "Phone number must start with + and country code", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    PhoneAuthOptions options =
                            PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                                    .setPhoneNumber(phoneNumber)
                                    .setTimeout(60L, TimeUnit.SECONDS)
                                    .setActivity(this)
                                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                        @Override
                                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                            FirebaseAuth.getInstance().signInWithCredential(credential)
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                                            startActivity(intent);
                                                            finish();

                                                            getSharedPreferences("MyPrefs", MODE_PRIVATE)
                                                                    .edit()
                                                                    .putBoolean("isLoggedIn", true)
                                                                    .apply();

                                                        } else {
                                                            Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }

                                        @Override
                                        public void onVerificationFailed(@NonNull FirebaseException e) {
                                            Toast.makeText(LoginActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }

                                        @Override
                                        public void onCodeSent(@NonNull String verificationId,
                                                               @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                            super.onCodeSent(verificationId, token);

                                            LayoutInflater inflater = LayoutInflater.from(LoginActivity.this);
                                            View dialogView = inflater.inflate(R.layout.dialog_code_input, null);
                                            EditText codeInput = dialogView.findViewById(R.id.codeEditText);

                                            AlertDialog.Builder codeBuilder = new AlertDialog.Builder(LoginActivity.this);
                                            codeBuilder.setTitle("Enter login code");
                                            codeBuilder.setView(dialogView);
                                            codeBuilder.setPositiveButton("Verify", (dialog1, which1) -> {
                                                String code = codeInput.getText().toString().trim();
                                                if (!code.isEmpty()) {
                                                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
                                                    FirebaseAuth.getInstance().signInWithCredential(credential)
                                                            .addOnCompleteListener(task -> {
                                                                if (task.isSuccessful()) {
                                                                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                                                    startActivity(intent);
                                                                    finish();
                                                                } else {
                                                                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                } else {
                                                    Toast.makeText(LoginActivity.this, "Please enter the code", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                            codeBuilder.setNegativeButton("Cancel", null);

                                            AlertDialog codeDialog = codeBuilder.create();
                                            codeDialog.setOnShowListener(dialog -> {
                                                Button positiveButton = codeDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                                Button negativeButton = codeDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                                                positiveButton.setTextColor(ContextCompat.getColor(LoginActivity.this, R.color.brown));
                                                negativeButton.setTextColor(ContextCompat.getColor(LoginActivity.this, R.color.green));
                                            });

                                            codeDialog.show();
                                        }

                                    })
                                    .build();

                    PhoneAuthProvider.verifyPhoneNumber(options);
                    dialog.dismiss();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error starting login verification: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void goToMain() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        }
    }
}

