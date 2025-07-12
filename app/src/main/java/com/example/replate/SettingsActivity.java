package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.Button;
import androidx.core.content.ContextCompat;


public class SettingsActivity extends AppCompatActivity {

    ImageView arrowBack;
    EditText addressEditText, phoneEditText, usernameEditText;
    SwitchCompat notificationsSwitch;
    TextView saveBtn;
    CardView deleteBtn;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseUser currentUser;
    private PlacesClient placesClient;
    private AutocompleteSessionToken token;
    private Map<String, String> addressToPlaceIdMap = new HashMap<>();
    private String selectedFullAddress = "";
    private String selectedPlaceId = null;



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
            AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                    .setPositiveButton("Yes, delete", (dialogInterface, which) -> {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String userId = currentUser.getUid();
                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            // Najpierw usuń paczki użytkownika
                            db.collection("parcels")
                                    .whereEqualTo("userId", userId)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        WriteBatch batch = db.batch();

                                        for (QueryDocumentSnapshot doc : querySnapshot) {
                                            batch.delete(doc.getReference());
                                        }

                                        batch.commit()
                                                .addOnSuccessListener(aVoid -> {
                                                    // Teraz usuń dokument użytkownika
                                                    db.collection("users").document(userId)
                                                            .delete()
                                                            .addOnSuccessListener(unused -> {
                                                                // I w końcu usuń konto Firebase Auth
                                                                currentUser.delete()
                                                                        .addOnSuccessListener(aVoid2 -> {
                                                                            Toast.makeText(SettingsActivity.this, "Account deleted successfully", Toast.LENGTH_LONG).show();
                                                                            FirebaseAuth.getInstance().signOut();
                                                                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                            startActivity(intent);
                                                                            finish();
                                                                        })
                                                                        .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                                            })
                                                            .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to delete Firestore user data: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to delete parcels: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to fetch parcels: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    })
                    .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                // Zmień kolory tekstu
                positiveButton.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.brown));
                negativeButton.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.green));
            });

            dialog.show();
        });


        // SharedPreferences
        SharedPreferences sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        boolean notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true);
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isNotificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled");
                            if (isNotificationsEnabled != null) {
                                notificationsSwitch.setChecked(isNotificationsEnabled);
                            }
                        }
                    });
        }

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Zapis do lokalnych SharedPreferences (opcjonalnie – możesz zostawić)
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean("notifications_enabled", isChecked);
            editor.apply();

            // Zapis do Firestore
            if (currentUser != null) {
                db.collection("users").document(currentUser.getUid())
                        .update("notificationsEnabled", isChecked)
                        .addOnSuccessListener(aVoid -> {
                            String msg = isChecked ? "Notifications enabled" : "Notifications disabled";
                            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(SettingsActivity.this, "Failed to update notification setting", Toast.LENGTH_SHORT).show());
            }
        });

        // Inicjalizacja Places API
        Places.initialize(getApplicationContext(), "AIzaSyAJo3H3Jy6QIlPC2JCJHNzhQTSNBZgPcnE"); // <-- użyj własnego klucza
        placesClient = Places.createClient(this);
        token = AutocompleteSessionToken.newInstance();

        if (addressEditText instanceof AutoCompleteTextView) {
            AutoCompleteTextView autoAddress = (AutoCompleteTextView) addressEditText;

            autoAddress.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (!s.toString().isEmpty()) {
                        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                                .setTypeFilter(TypeFilter.ADDRESS)
                                .setSessionToken(token)
                                .setQuery(s.toString())
                                .build();

                        placesClient.findAutocompletePredictions(request)
                                .addOnSuccessListener(response -> {
                                    List<String> suggestions = new ArrayList<>();
                                    addressToPlaceIdMap.clear();

                                    for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                        String fullAddress = prediction.getFullText(null).toString();
                                        String placeId = prediction.getPlaceId();

                                        suggestions.add(fullAddress);
                                        addressToPlaceIdMap.put(fullAddress, placeId);
                                    }

                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(SettingsActivity.this,
                                            android.R.layout.simple_dropdown_item_1line, suggestions);
                                    autoAddress.setAdapter(adapter);
                                    autoAddress.showDropDown();

                                    autoAddress.setOnItemClickListener((parent, view, position, id) -> {
                                        selectedFullAddress = (String) parent.getItemAtPosition(position);
                                        selectedPlaceId = addressToPlaceIdMap.get(selectedFullAddress);

                                        if (!selectedFullAddress.matches(".*\\d+.*")) {
                                            autoAddress.setError("Please select a full address with a house number");
                                            selectedPlaceId = null;
                                        } else {
                                            autoAddress.setError(null); // OK
                                        }
                                    });
                                });
                    }
                }
            });
        }
    }
}