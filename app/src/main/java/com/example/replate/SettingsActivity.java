package com.example.replate;
/**
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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.HashMap;
import java.util.Map;
import android.content.Intent;
import android.net.Uri;
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
    TextView saveBtn, addBankBtn;
    CardView deleteBtn;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseUser currentUser;
    private PlacesClient placesClient;
    private AutocompleteSessionToken token;
    private Map<String, String> addressToPlaceIdMap = new HashMap<>();
    private String selectedFullAddress = "";
    private String selectedPlaceId = null;
    private boolean hasStripeAccount = false;
    private String currentStripeAccountId = null;


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
        addBankBtn = findViewById(R.id.addBankBtn);

        // Init Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        handleStripeReturn(getIntent());



        if (currentUser != null) {
            String userId = currentUser.getUid();


            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String address = documentSnapshot.getString("address");
                    String phone = documentSnapshot.getString("phone");
                    String login = documentSnapshot.getString("login");
                    String stripeAccountId = documentSnapshot.getString("stripeAccountId");

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

                    // Sprawdź czy użytkownik ma konto Stripe
                    if (stripeAccountId != null && !stripeAccountId.isEmpty()) {
                        hasStripeAccount = true;
                        currentStripeAccountId = stripeAccountId;
                        updateBankButton(true);
                    } else {
                        hasStripeAccount = false;
                        currentStripeAccountId = null;
                        updateBankButton(false);
                    }
                }
            });
        }

        addBankBtn.setOnClickListener(v -> {
            if (hasStripeAccount) {
                // Pokaż dialog potwierdzenia usunięcia
                showDeleteStripeAccountDialog();
            } else {
                // Utwórz nowe konto
                createStripeAccount();
            }
        });

        arrowBack.setOnClickListener(v -> finish()); // back button

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

    private void createStripeAccount() {
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = currentUser.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No email found for this user", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFunctions functions = FirebaseFunctions.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);

        functions
                .getHttpsCallable("createStripeAccount")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    Boolean success = (Boolean) response.get("success");
                    String onboardingUrl = (String) response.get("onboardingUrl");

                    if (success != null && success && onboardingUrl != null) {
                        // Otwieramy przeglądarkę lub WebView z linkiem Stripe
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(onboardingUrl));
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Stripe account creation failed", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
    }

    private void handleStripeReturn(Intent intent) {
        Uri data = intent.getData();
        if (data != null && "replateapp".equals(data.getScheme())) {
            String path = data.getPath();
            if ("/stripe/return".equals(path)) {
                Toast.makeText(this, "Stripe setup completed!", Toast.LENGTH_LONG).show();
                // Możesz tutaj zaktualizować UI lub sprawdzić status konta
            } else if ("/stripe/reauth".equals(path)) {
                Toast.makeText(this, "Please complete Stripe setup", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateBankButton(boolean hasAccount) {
        if (hasAccount) {
            addBankBtn.setText("Delete Bank Account");
            addBankBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
            addBankBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red));
        } else {
            addBankBtn.setText("Add Bank Account");
            addBankBtn.setTextColor(ContextCompat.getColor(this, R.color.brown));
            addBankBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.yellow));
        }
    }

    private void showDeleteStripeAccountDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Stripe Account")
                .setMessage("Are you sure you want to delete your Stripe account? This will remove your payment capabilities.")
                .setPositiveButton("Yes, delete", (dialogInterface, which) -> {
                    deleteStripeAccount();
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.red));
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.green));
        });

        dialog.show();
    }

    private void deleteStripeAccount() {
        if (currentUser == null || currentStripeAccountId == null) {
            Toast.makeText(this, "No Stripe account to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pokaż progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Deleting Stripe Account")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Przygotuj dane
        Map<String, Object> data = new HashMap<>();
        data.put("accountId", currentStripeAccountId);

        // Wywołaj Cloud Function
        FirebaseFunctions.getInstance()
                .getHttpsCallable("deleteStripeAccount")
                .call(data)
                .addOnSuccessListener(result -> {
                    progressDialog.dismiss();

                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    Boolean success = (Boolean) response.get("success");

                    if (success != null && success) {
                        // Zaktualizuj lokalny stan
                        hasStripeAccount = false;
                        currentStripeAccountId = null;
                        updateBankButton(false);

                        Toast.makeText(this, "Stripe account deleted successfully", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to delete Stripe account", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}**/


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

import com.example.replate.repository.ParcelRepository;
import com.example.replate.repository.UserRepository;
import com.example.replate.service.PlacesService;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;

import android.content.Intent;
import android.net.Uri;
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
    TextView saveBtn, addBankBtn;
    CardView deleteBtn;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    // Repositories & Services
    private UserRepository userRepository;
    private ParcelRepository parcelRepository;
    private PlacesService placesService;

    private String selectedPlaceId = null;
    private boolean hasStripeAccount = false;
    private String currentStripeAccountId = null;


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
        addBankBtn = findViewById(R.id.addBankBtn);

        // Init Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Init Repositories & Services
        userRepository = new UserRepository();
        parcelRepository = new ParcelRepository();

        Places.initialize(getApplicationContext(), "AIzaSyAJo3H3Jy6QIlPC2JCJHNzhQTSNBZgPcnE");
        PlacesClient placesClient = Places.createClient(this);
        placesService = new PlacesService(placesClient);

        handleStripeReturn(getIntent());

        // Load user data
        loadUserData();

        // Setup listeners
        setupListeners();
        setupAddressAutocomplete();
    }

    private void loadUserData() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        userRepository.getUserData(userId, new UserRepository.OnUserDataLoadedListener() {
            @Override
            public void onSuccess(UserRepository.UserData userData) {
                // Ustaw dane w polach
                if (userData.address != null && !userData.address.isEmpty()) {
                    addressEditText.setText(userData.address);
                } else {
                    addressEditText.setHint("Burzyn 23");
                }

                if (userData.phone != null && !userData.phone.isEmpty()) {
                    phoneEditText.setText(userData.phone);
                } else {
                    phoneEditText.setHint("+48 555 777 555");
                }

                if (userData.login != null && !userData.login.isEmpty()) {
                    usernameEditText.setText(userData.login);
                } else {
                    usernameEditText.setHint("Enter your username");
                }

                // Sprawdź Stripe account
                if (userData.stripeAccountId != null && !userData.stripeAccountId.isEmpty()) {
                    hasStripeAccount = true;
                    currentStripeAccountId = userData.stripeAccountId;
                    updateBankButton(true);
                } else {
                    hasStripeAccount = false;
                    currentStripeAccountId = null;
                    updateBankButton(false);
                }

                // Ustaw notyfikacje
                if (userData.notificationsEnabled != null) {
                    notificationsSwitch.setChecked(userData.notificationsEnabled);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SettingsActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        arrowBack.setOnClickListener(v -> finish());

        saveBtn.setOnClickListener(v -> saveUserData());

        deleteBtn.setOnClickListener(v -> showDeleteAccountDialog());

        addBankBtn.setOnClickListener(v -> {
            if (hasStripeAccount) {
                showDeleteStripeAccountDialog();
            } else {
                createStripeAccount();
            }
        });

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentUser == null) return;

            userRepository.updateNotificationSettings(currentUser.getUid(), isChecked,
                    new UserRepository.OnUserUpdateListener() {
                        @Override
                        public void onSuccess() {
                            String msg = isChecked ? "Notifications enabled" : "Notifications disabled";
                            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(SettingsActivity.this, "Failed to update notification setting", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void saveUserData() {
        if (currentUser == null) return;

        String newAddress = addressEditText.getText().toString().trim();
        String newPhone = phoneEditText.getText().toString().trim();
        String newLogin = usernameEditText.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        if (!newAddress.isEmpty()) updates.put("address", newAddress);
        if (!newPhone.isEmpty()) updates.put("phone", newPhone);
        if (!newLogin.isEmpty()) updates.put("login", newLogin);

        if (updates.isEmpty()) {
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            return;
        }

        userRepository.updateUserData(currentUser.getUid(), updates, new UserRepository.OnUserUpdateListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(SettingsActivity.this, "Changes saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SettingsActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAddressAutocomplete() {
        if (!(addressEditText instanceof AutoCompleteTextView)) return;

        AutoCompleteTextView autoAddress = (AutoCompleteTextView) addressEditText;

        autoAddress.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString();
                if (query.isEmpty()) return;

                placesService.searchAddresses(query, new PlacesService.OnAddressSuggestionsListener() {
                    @Override
                    public void onSuccess(List<PlacesService.AddressSuggestion> suggestions) {
                        List<String> addresses = new ArrayList<>();
                        final Map<String, String> addressToPlaceIdMap = new HashMap<>();

                        for (PlacesService.AddressSuggestion suggestion : suggestions) {
                            addresses.add(suggestion.fullAddress);
                            addressToPlaceIdMap.put(suggestion.fullAddress, suggestion.placeId);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(SettingsActivity.this,
                                android.R.layout.simple_dropdown_item_1line, addresses);
                        autoAddress.setAdapter(adapter);
                        autoAddress.showDropDown();

                        autoAddress.setOnItemClickListener((parent, view, position, id) -> {
                            String selectedAddress = (String) parent.getItemAtPosition(position);
                            selectedPlaceId = addressToPlaceIdMap.get(selectedAddress);

                            if (!placesService.validateAddress(selectedAddress)) {
                                autoAddress.setError("Please select a full address with a house number");
                                selectedPlaceId = null;
                            } else {
                                autoAddress.setError(null);
                                placesService.renewSessionToken();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SettingsActivity.this, "Failed to load addresses", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showDeleteAccountDialog() {
        AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Yes, delete", (dialogInterface, which) -> deleteAccount())
                .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.brown));
            negativeButton.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.green));
        });

        dialog.show();
    }

    private void deleteAccount() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Krok 1: Usuń paczki użytkownika
        parcelRepository.deleteUserParcels(userId, new ParcelRepository.OnParcelDeletedListener() {
            @Override
            public void onSuccess() {
                // Krok 2: Usuń dokument użytkownika z Firestore
                userRepository.deleteUserDocument(userId, new UserRepository.OnUserUpdateListener() {
                    @Override
                    public void onSuccess() {
                        // Krok 3: Usuń konto Firebase Auth
                        currentUser.delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SettingsActivity.this, "Account deleted successfully", Toast.LENGTH_LONG).show();
                                    FirebaseAuth.getInstance().signOut();
                                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(SettingsActivity.this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SettingsActivity.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SettingsActivity.this, "Failed to delete parcels: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ========== STRIPE METHODS ==========

    private void createStripeAccount() {
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = currentUser.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No email found for this user", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);

        functions
                .getHttpsCallable("createStripeAccount")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    Boolean success = (Boolean) response.get("success");
                    String onboardingUrl = (String) response.get("onboardingUrl");

                    if (success != null && success && onboardingUrl != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(onboardingUrl));
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Stripe account creation failed", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
    }

    private void handleStripeReturn(Intent intent) {
        Uri data = intent.getData();
        if (data != null && "replateapp".equals(data.getScheme())) {
            String path = data.getPath();
            if ("/stripe/return".equals(path)) {
                Toast.makeText(this, "Stripe setup completed!", Toast.LENGTH_LONG).show();
            } else if ("/stripe/reauth".equals(path)) {
                Toast.makeText(this, "Please complete Stripe setup", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateBankButton(boolean hasAccount) {
        if (hasAccount) {
            addBankBtn.setText("Delete Bank Account");
            addBankBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
            addBankBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red));
        } else {
            addBankBtn.setText("Add Bank Account");
            addBankBtn.setTextColor(ContextCompat.getColor(this, R.color.brown));
            addBankBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.yellow));
        }
    }

    private void showDeleteStripeAccountDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Stripe Account")
                .setMessage("Are you sure you want to delete your Stripe account? This will remove your payment capabilities.")
                .setPositiveButton("Yes, delete", (dialogInterface, which) -> deleteStripeAccount())
                .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.red));
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.green));
        });

        dialog.show();
    }

    private void deleteStripeAccount() {
        if (currentUser == null || currentStripeAccountId == null) {
            Toast.makeText(this, "No Stripe account to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Deleting Stripe Account")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        Map<String, Object> data = new HashMap<>();
        data.put("accountId", currentStripeAccountId);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("deleteStripeAccount")
                .call(data)
                .addOnSuccessListener(result -> {
                    progressDialog.dismiss();
                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    Boolean success = (Boolean) response.get("success");

                    if (success != null && success) {
                        hasStripeAccount = false;
                        currentStripeAccountId = null;
                        updateBankButton(false);
                        Toast.makeText(this, "Stripe account deleted successfully", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to delete Stripe account", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}