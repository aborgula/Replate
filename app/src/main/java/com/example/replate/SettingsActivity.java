package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    ImageView arrowBack;
    EditText addressEditText, phoneEditText, usernameEditText;
    SwitchCompat notificationsSwitch;
    TextView saveBtn;
    CardView deleteBtn;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseUser currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Find all views
        arrowBack = findViewById(R.id.arrowback);
        addressEditText = findViewById(R.id.addressEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        saveBtn = findViewById(R.id.savebtn);
        deleteBtn = findViewById(R.id.deletebtn);

        // Init Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String address = documentSnapshot.getString("address");
                    String phone = documentSnapshot.getString("phone");
                    String login = documentSnapshot.getString("login");

                    if (address != null && !address.isEmpty()) {
                        addressEditText.setText(address);
                    } else {
                        addressEditText.setHint("Burzyn 23");
                    }

                    if (phone != null && !phone.isEmpty()) {
                        phoneEditText.setText(phone);
                    } else {
                        phoneEditText.setHint("+48 555 777 555");
                    }

                    if (login != null && !login.isEmpty()) {
                        usernameEditText.setText(login);
                    } else {
                        usernameEditText.setHint("Enter your username");
                    }
                }
            });

        }

        // Example usage
        arrowBack.setOnClickListener(v -> finish()); // back button
        // ZAPIS zmian po kliknięciu
        saveBtn.setOnClickListener(v -> {
            String newAddress = addressEditText.getText().toString().trim();
            String newPhone = phoneEditText.getText().toString().trim();
            String newLogin = usernameEditText.getText().toString().trim();

            // Używamy tylko pól, które nie są puste
            Map<String, Object> updates = new HashMap<>();
            if (!newAddress.isEmpty()) updates.put("address", newAddress);
            if (!newPhone.isEmpty()) updates.put("phone", newPhone);
            if (!newLogin.isEmpty()) updates.put("login", newLogin);

            if (!updates.isEmpty()) {
                db.collection("users").document(currentUser.getUid())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();

                            // Wyczyść pola i ustaw nowe hinty
                            if (updates.containsKey("address")) {
                                addressEditText.setText(newAddress);
                            }
                            if (updates.containsKey("phone")) {
                                phoneEditText.setText(newPhone);
                            }
                            if (updates.containsKey("login")) {
                                usernameEditText.setText(newLogin);
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            }
        });

        deleteBtn.setOnClickListener(v -> {
            // Handle account delete/save changes
        });
    }
}
