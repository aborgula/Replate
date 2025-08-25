package com.example.replate;

import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.replate.databinding.ActivityHomeBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HomeActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri cameraImageUri;
    private Uri selectedParcelImageUri = null;
    private String phoneNumber = "";
    private double price = 0.0;
    ActivityHomeBinding binding;
    TextView cancel, back, addparcel;
    MaterialButton next;
    String title = "";
    String description = "";
    private String startTime = "";
    private String endTime = "";
    private String selectedFullAddress = "";
    private String selectedPlaceId = "";
    private final Map<String, String> addressToPlaceIdMap = new HashMap<>();
    private ImageView parcelImageView;
    private TextInputEditText addphonenumber, addprice;
    private String selectedDay = "today";
    List<String> selectedTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));
        }
        // Ustaw czarne ikony na białym tle (dostępne od Android 6.0 - API 23)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.bottomNavigationView.setBackground(null);

        if (getIntent().getBooleanExtra("openProfileFragment", false)) {
            String userId = getIntent().getStringExtra("showUserId");
            if (userId != null) {
                openUserProfile(userId);
            }
        }else if(getIntent().getBooleanExtra("openOrdersFragment", false)){
            replaceFragment(new OrdersFragment());
            binding.bottomNavigationView.setSelectedItemId(R.id.orders);
        }else{
            replaceFragment(new PickupFragment());
        }

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAwKhbg7_QX7gYKwuLRuaLYf6GXz2lAu0E");
        }

        FloatingActionButton addParcelBtn = findViewById(R.id.addparcel);
        addParcelBtn.setOnClickListener(v -> {
            showAddParcelStep1Dialog();
        });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedParcelImageUri = result.getData().getData();
                        parcelImageView.setImageURI(selectedParcelImageUri);
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        selectedParcelImageUri = cameraImageUri;
                        parcelImageView.setImageURI(selectedParcelImageUri);
                    }
                });


        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.pickup) {
                replaceFragment(new PickupFragment());
            } else if (itemId == R.id.favourites) {
                replaceFragment(new FavouritesFragment());
            } else if (itemId == R.id.orders) {
                replaceFragment(new OrdersFragment());
            } else if (itemId == R.id.profile) {
                replaceFragment(new ProfileFragment());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        Log.d("qqq", " w replacefragment");
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    private void showAddParcelStep1Dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_parcel_step1, null);

        builder.setView(dialogView);
        EditText editTextTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextDescription);
        cancel = dialogView.findViewById(R.id.cancel);
        next = dialogView.findViewById(R.id.next);

        editTextTitle.setText(title);
        editTextDescription.setText(description);

        builder.setTitle("New Parcel");
        AlertDialog dialog = builder.create();

        cancel.setOnClickListener(v -> {
            title = "";
            description = "";
            selectedTags.clear();
            startTime = "";
            endTime = "";
            selectedFullAddress = "";
            selectedPlaceId = "";
            addressToPlaceIdMap.clear();
            selectedParcelImageUri = null;
            phoneNumber = "";
            selectedDay = "today";
            dialog.dismiss();
        });

        next.setOnClickListener(v->{
            title = editTextTitle.getText().toString();
            description = editTextDescription.getText().toString();
            if(title.isEmpty() || description.isEmpty()){
                Toast.makeText(HomeActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }else{
                dialog.dismiss();
                showAddParcelStep2Dialog();
            }
        });

        dialog.show();

        dialog.setOnCancelListener(dialogInterface -> {
            title = "";
            description = "";
            selectedTags.clear();
            startTime = "";
            endTime = "";
            selectedFullAddress = "";
            selectedPlaceId = "";
            addressToPlaceIdMap.clear();
            selectedParcelImageUri = null;
            phoneNumber = "";
            price = 0.0;
        });
    }

    private void showAddParcelStep2Dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_parcel_step2, null);
        builder.setView(dialogView);

        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroup);

        back = dialogView.findViewById(R.id.back);
        next = dialogView.findViewById(R.id.next);

        builder.setTitle("Choose tags");
        AlertDialog dialog = builder.create();

        back.setOnClickListener(v -> {
            dialog.dismiss();
            showAddParcelStep1Dialog();
        });

        next.setOnClickListener(v -> {
            selectedTags.clear();

            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                View chipView = chipGroup.getChildAt(i);
                if (chipView instanceof Chip) {
                    Chip chip = (Chip) chipView;
                    if (chip.isChecked()) {
                        selectedTags.add(chip.getText().toString());
                    }
                }
            }

            if (selectedTags.isEmpty()) {
                Toast.makeText(HomeActivity.this, "Choose at least one tag", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("SelectedTags", selectedTags.toString());
                dialog.dismiss();
                showAddParcelStep3Dialog();
            }
        });

        dialog.show();

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View chipView = chipGroup.getChildAt(i);
            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;
                String chipText = chip.getText().toString();
                chip.setChecked(selectedTags.contains(chipText));

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (!selectedTags.contains(chipText)) {
                            selectedTags.add(chipText);
                        }
                    } else {
                        selectedTags.remove(chipText);
                    }
                });
            }
        }

        dialog.setOnCancelListener(dialogInterface -> {
            title = "";
            description = "";
            selectedTags.clear();
            startTime = "";
            endTime = "";
            selectedFullAddress = "";
            selectedPlaceId = "";
            addressToPlaceIdMap.clear();
            selectedParcelImageUri = null;
            phoneNumber = "";
            price = 0.0;
            selectedDay = "today";
        });
    }

    /**private void showAddParcelStep3Dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_parcel_step3, null);
        builder.setView(dialogView);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LinearLayout mylocation;
        mylocation = dialogView.findViewById(R.id.mylocation);

        back = dialogView.findViewById(R.id.back);
        next = dialogView.findViewById(R.id.next);

        builder.setTitle("Choose time and location");
        AlertDialog dialog = builder.create();

        MaterialButton todayBtn = dialogView.findViewById(R.id.todayfltr);
        MaterialButton tomorrowBtn = dialogView.findViewById(R.id.tomorrowfltr);

        highlightButton(todayBtn, true);
        highlightButton(tomorrowBtn, false);

        todayBtn.setOnClickListener(v -> {
            highlightButton(todayBtn, true);
            highlightButton(tomorrowBtn, false);
            selectedDay = "today";

        });

        tomorrowBtn.setOnClickListener(v -> {
            highlightButton(todayBtn, false);
            highlightButton(tomorrowBtn, true);
            selectedDay = "tomorrow";
        });

        back.setOnClickListener(v -> {
            dialog.dismiss();
            showAddParcelStep2Dialog();
        });

        next.setOnClickListener(v -> {
            if (selectedFullAddress.isEmpty() || selectedPlaceId == null) {
                Toast.makeText(this, "Please select a valid address from suggestions", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            showAddParcelStep4Dialog();
        });

        dialog.setOnCancelListener(dialogInterface -> {
            title = "";
            description = "";
            selectedTags.clear();
            startTime = "";
            endTime = "";
            selectedFullAddress = "";
            selectedPlaceId = "";
            addressToPlaceIdMap.clear();
            selectedParcelImageUri = null;
            phoneNumber = "";
            price = 0.0;
            selectedDay = "today";
        });


        TextView startTimeText = dialogView.findViewById(R.id.startTimeText);
        TextView endTimeText = dialogView.findViewById(R.id.endTimeText);

        final Calendar startCalendar = Calendar.getInstance();
        final Calendar endCalendar = Calendar.getInstance();

        startCalendar.setTimeInMillis(System.currentTimeMillis());
        endCalendar.setTimeInMillis(System.currentTimeMillis() + 2 * 60 * 60 * 1000); // 2 godziny

        if (startTime.isEmpty() || endTime.isEmpty()) {
            Calendar now = Calendar.getInstance();
            startTime = String.format(Locale.getDefault(), "%02d:%02d",
                    now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));

            now.add(Calendar.HOUR_OF_DAY, 2);
            endTime = String.format(Locale.getDefault(), "%02d:%02d",
                    now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        }

        startTimeText.setText(startTime);
        endTimeText.setText(endTime);

        startTimeText.setOnClickListener(v -> {
            showTimePicker((hour, minute) -> {
                startCalendar.set(Calendar.HOUR_OF_DAY, hour);
                startCalendar.set(Calendar.MINUTE, minute);

                startTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                startTimeText.setText(startTime);

                long diffMillis = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
                long diffMinutes = diffMillis / (1000 * 60);

                if (diffMinutes < 30) {
                    Calendar newEnd = (Calendar) startCalendar.clone();
                    newEnd.add(Calendar.MINUTE, 30);

                    endCalendar.setTimeInMillis(newEnd.getTimeInMillis());
                    int endHour = newEnd.get(Calendar.HOUR_OF_DAY);
                    int endMinute = newEnd.get(Calendar.MINUTE);

                    endTime = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute);
                    endTimeText.setText(endTime);
                }
            });
        });

        endTimeText.setOnClickListener(v -> {
            showTimePicker((hour, minute) -> {
                endCalendar.set(Calendar.HOUR_OF_DAY, hour);
                endCalendar.set(Calendar.MINUTE, minute);

                long diffMillis = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
                long diffMinutes = diffMillis / (1000 * 60);

                if (diffMinutes >= 30) {
                    endTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    endTimeText.setText(endTime);
                } else {
                    Toast.makeText(this, "End time must be at least 30 minutes after start time", Toast.LENGTH_SHORT).show();
                }
            });
        });

        AutoCompleteTextView locationInput = dialogView.findViewById(R.id.locationAutoComplete);

        PlacesClient placesClient = Places.createClient(this);
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        if (selectedFullAddress != null && !selectedFullAddress.isEmpty()) {
            locationInput.setText(selectedFullAddress);
            // Opcjonalnie przesun kursor na koniec tekstu:
            locationInput.setSelection(selectedFullAddress.length());
        }

        locationInput.addTextChangedListener(new TextWatcher() {
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
                                    addressToPlaceIdMap.put(fullAddress, placeId);  // zapisz
                                }

                                ArrayAdapter<String> adapter = new ArrayAdapter<>(dialogView.getContext(),
                                        android.R.layout.simple_dropdown_item_1line, suggestions);
                                locationInput.setAdapter(adapter);
                                locationInput.showDropDown();
                                locationInput.setOnItemClickListener((parent, view, position, id) -> {
                                    selectedFullAddress = (String) parent.getItemAtPosition(position);
                                    selectedPlaceId = addressToPlaceIdMap.get(selectedFullAddress);

                                    if (!selectedFullAddress.matches(".*\\d+.*")) {
                                        locationInput.setError("Please select a full address with a house number");
                                        selectedPlaceId = null;
                                    } else {
                                        locationInput.setError(null); // poprawny wybór
                                    }
                                });
                            });
                }
            }
        });

        mylocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    // Reverse geocoding (zamiana lat/lng na adres)
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            String addressLine = addresses.get(0).getAddressLine(0);
                            locationInput.setText(addressLine);
                            locationInput.setSelection(addressLine.length());
                            selectedFullAddress = addressLine;

                            // Uzyskaj placeId dla adresu
                            FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                                    .setTypeFilter(TypeFilter.ADDRESS)
                                    .setSessionToken(token)
                                    .setQuery(addressLine)
                                    .build();

                            placesClient.findAutocompletePredictions(request)
                                    .addOnSuccessListener(response -> {
                                        for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                            if (prediction.getFullText(null).toString().equals(addressLine)) {
                                                selectedPlaceId = prediction.getPlaceId();
                                                break;
                                            }
                                        }

                                        if (selectedPlaceId == null) {
                                            locationInput.setError("Could not find place ID for this location");
                                        } else {
                                            locationInput.setError(null);
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Could not determine address", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Geocoder failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();

        dialog.setOnCancelListener(dialogInterface -> {
            title = "";
            description = "";
            selectedTags.clear();
            startTime = "";
            endTime = "";
            selectedFullAddress = "";
            selectedPlaceId = "";
            addressToPlaceIdMap.clear();
            selectedParcelImageUri = null;
        });
    }**/
    private void showAddParcelStep3Dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_parcel_step3, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setTitle("Choose time and location");

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LinearLayout mylocation = dialogView.findViewById(R.id.mylocation);
        back = dialogView.findViewById(R.id.back);
        next = dialogView.findViewById(R.id.next);

        MaterialButton todayBtn = dialogView.findViewById(R.id.todayfltr);
        MaterialButton tomorrowBtn = dialogView.findViewById(R.id.tomorrowfltr);

        highlightButton(todayBtn, true);
        highlightButton(tomorrowBtn, false);

        todayBtn.setOnClickListener(v -> {
            highlightButton(todayBtn, true);
            highlightButton(tomorrowBtn, false);
            selectedDay = "today";
        });

        tomorrowBtn.setOnClickListener(v -> {
            highlightButton(todayBtn, false);
            highlightButton(tomorrowBtn, true);
            selectedDay = "tomorrow";
        });

        back.setOnClickListener(v -> {
            dialog.dismiss();
            showAddParcelStep2Dialog();
        });

        next.setOnClickListener(v -> {
            if (selectedFullAddress.isEmpty() || selectedPlaceId == null) {
                Toast.makeText(this, "Please select a valid address from suggestions", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            showAddParcelStep4Dialog();
        });

        dialog.setOnCancelListener(dialogInterface -> {
            title = "";
            description = "";
            selectedTags.clear();
            startTime = "";
            endTime = "";
            selectedFullAddress = "";
            selectedPlaceId = "";
            addressToPlaceIdMap.clear();
            selectedParcelImageUri = null;
            phoneNumber = "";
            price = 0.0;
            selectedDay = "today";
        });

        TextView startTimeText = dialogView.findViewById(R.id.startTimeText);
        TextView endTimeText = dialogView.findViewById(R.id.endTimeText);

        final Calendar startCalendar = Calendar.getInstance();
        final Calendar endCalendar = Calendar.getInstance();

        if (startTime.isEmpty() || endTime.isEmpty()) {
            Calendar now = Calendar.getInstance();
            startTime = String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
            now.add(Calendar.HOUR_OF_DAY, 2);
            endTime = String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        }

        startTimeText.setText(startTime);
        endTimeText.setText(endTime);

        startTimeText.setOnClickListener(v -> {
            showTimePicker((hour, minute) -> {
                startCalendar.set(Calendar.HOUR_OF_DAY, hour);
                startCalendar.set(Calendar.MINUTE, minute);
                startTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                startTimeText.setText(startTime);

                long diff = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
                if (diff < 30 * 60 * 1000) {
                    Calendar newEnd = (Calendar) startCalendar.clone();
                    newEnd.add(Calendar.MINUTE, 30);
                    endCalendar.setTimeInMillis(newEnd.getTimeInMillis());
                    endTime = String.format(Locale.getDefault(), "%02d:%02d", newEnd.get(Calendar.HOUR_OF_DAY), newEnd.get(Calendar.MINUTE));
                    endTimeText.setText(endTime);
                }
            });
        });

        endTimeText.setOnClickListener(v -> {
            showTimePicker((hour, minute) -> {
                endCalendar.set(Calendar.HOUR_OF_DAY, hour);
                endCalendar.set(Calendar.MINUTE, minute);

                long diff = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
                if (diff >= 30 * 60 * 1000) {
                    endTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    endTimeText.setText(endTime);
                } else {
                    Toast.makeText(this, "End time must be at least 30 minutes after start time", Toast.LENGTH_SHORT).show();
                }
            });
        });

        AutoCompleteTextView locationInput = dialogView.findViewById(R.id.locationAutoComplete);
        PlacesClient placesClient = Places.createClient(this);
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        // 🔄 Jeśli adres zapisany wcześniej w Firebase – pokaż go
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String savedAddress = documentSnapshot.getString("address");
                            if (savedAddress != null && !savedAddress.isEmpty()) {
                                locationInput.setText(savedAddress);
                                locationInput.setSelection(savedAddress.length());
                                selectedFullAddress = savedAddress;

                                FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                                        .setTypeFilter(TypeFilter.ADDRESS)
                                        .setSessionToken(token)
                                        .setQuery(savedAddress)
                                        .build();

                                placesClient.findAutocompletePredictions(request)
                                        .addOnSuccessListener(response -> {
                                            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                                if (prediction.getFullText(null).toString().equals(savedAddress)) {
                                                    selectedPlaceId = prediction.getPlaceId();
                                                    break;
                                                }
                                            }

                                            if (selectedPlaceId == null) {
                                                locationInput.setError("Could not find place ID for this address");
                                            } else {
                                                locationInput.setError(null);
                                            }
                                        });
                            }
                        }
                    });
        }

        locationInput.addTextChangedListener(new TextWatcher() {
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

                                ArrayAdapter<String> adapter = new ArrayAdapter<>(dialogView.getContext(),
                                        android.R.layout.simple_dropdown_item_1line, suggestions);
                                locationInput.setAdapter(adapter);
                                locationInput.showDropDown();

                                locationInput.setOnItemClickListener((parent, view, position, id) -> {
                                    selectedFullAddress = (String) parent.getItemAtPosition(position);
                                    selectedPlaceId = addressToPlaceIdMap.get(selectedFullAddress);

                                    if (!selectedFullAddress.matches(".*\\d+.*")) {
                                        locationInput.setError("Please select a full address with a house number");
                                        selectedPlaceId = null;
                                    } else {
                                        locationInput.setError(null);
                                    }
                                });
                            });
                }
            }
        });

        mylocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            String addressLine = addresses.get(0).getAddressLine(0);
                            locationInput.setText(addressLine);
                            locationInput.setSelection(addressLine.length());
                            selectedFullAddress = addressLine;

                            FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                                    .setTypeFilter(TypeFilter.ADDRESS)
                                    .setSessionToken(token)
                                    .setQuery(addressLine)
                                    .build();

                            placesClient.findAutocompletePredictions(request)
                                    .addOnSuccessListener(response -> {
                                        for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                            if (prediction.getFullText(null).toString().equals(addressLine)) {
                                                selectedPlaceId = prediction.getPlaceId();
                                                break;
                                            }
                                        }

                                        if (selectedPlaceId == null) {
                                            locationInput.setError("Could not find place ID for this location");
                                        } else {
                                            locationInput.setError(null);
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Could not determine address", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Geocoder failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showAddParcelStep4Dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_parcel_step4, null);
        builder.setView(dialogView);

        back = dialogView.findViewById(R.id.back);
        next = dialogView.findViewById(R.id.next);
        addprice = dialogView.findViewById(R.id.addprice);
        addphonenumber = dialogView.findViewById(R.id.addphonenumber);

        builder.setTitle("Add phone number and price");
        AlertDialog dialog = builder.create();

        // Ustaw cenę
        //addprice.setText(String.format("%.2f", price));

        // Jeśli phoneNumber już jest ustawiony – wpisz
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            addphonenumber.setText(phoneNumber);
        } else {
            // Pobierz z Firestore
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (currentUser != null) {
                db.collection("users").document(currentUser.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String fetchedPhone = documentSnapshot.getString("phone");
                                if (fetchedPhone != null && !fetchedPhone.isEmpty()) {
                                    addphonenumber.setText(fetchedPhone);
                                    phoneNumber = fetchedPhone; // zapamiętaj na przyszłość
                                }
                            }
                        });
            }
        }

        back.setOnClickListener(v -> {
            dialog.dismiss();
            showAddParcelStep3Dialog();
        });

        next.setOnClickListener(v -> {
            String priceText = addprice.getText().toString().trim().replace(",", ".");
            String phoneText = addphonenumber.getText().toString().trim();

            if (priceText.isEmpty()) {
                Toast.makeText(this, "Please enter a price", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phoneText.isEmpty()) {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                price = Double.parseDouble(priceText);
                price = Math.round(price * 100.0) / 100.0;
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
                return;
            }

            phoneNumber = phoneText;
            dialog.dismiss();
            showAddParcelStep5Dialog();
        });

        dialog.setOnCancelListener(dialogInterface -> {
            title = "";
            description = "";
            selectedTags.clear();
            startTime = "";
            endTime = "";
            selectedFullAddress = "";
            selectedPlaceId = "";
            price = 0.0;
            addressToPlaceIdMap.clear();
            selectedParcelImageUri = null;
            phoneNumber = "";
            selectedDay = "today";
        });

        dialog.show();
    }

    private void showAddParcelStep5Dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_parcel_step5, null);
        builder.setView(dialogView);

        back = dialogView.findViewById(R.id.back);
        addparcel = dialogView.findViewById(R.id.addparcel);

        builder.setTitle("Add photo (optional)");
        AlertDialog dialog = builder.create();

        parcelImageView = dialogView.findViewById(R.id.parcelImage);
        if (selectedParcelImageUri != null) {
            parcelImageView.setImageURI(selectedParcelImageUri);
        }

        if(selectedParcelImageUri == null){
            if (selectedTags.size() > 1) {
                parcelImageView.setImageResource(R.drawable.photo_mixed);
            } else if (selectedTags.size() == 1) {
                String tag = selectedTags.get(0).toLowerCase(Locale.ROOT);
                switch (tag) {
                    case "sweets":
                        parcelImageView.setImageResource(R.drawable.photo_sweets);
                        break;
                    case "fast food":
                        parcelImageView.setImageResource(R.drawable.photo_fastfood);
                        break;
                    case "bakery":
                        parcelImageView.setImageResource(R.drawable.photo_bakery);
                        break;
                    case "dairy":
                        parcelImageView.setImageResource(R.drawable.photo_dairy);
                        break;
                    case "vegetables":
                        parcelImageView.setImageResource(R.drawable.photo_vegetables);
                        break;
                    case "vegetarian":
                        parcelImageView.setImageResource(R.drawable.photo_vegetarian);
                        break;
                    case "vegan":
                        parcelImageView.setImageResource(R.drawable.photo_vegan);
                        break;
                    case "fruit":
                        parcelImageView.setImageResource(R.drawable.photo_fruits);
                        break;
                    case "liquids":
                        parcelImageView.setImageResource(R.drawable.photo_liquids);
                        break;
                    case "meals":
                        parcelImageView.setImageResource(R.drawable.photo_meal);
                        break;
                    case "meat":
                        parcelImageView.setImageResource(R.drawable.photo_meat);
                        break;
                    default:
                        parcelImageView.setImageResource(R.drawable.photo_mixed);
                        break;
                }
            }
        }


        parcelImageView.setOnClickListener(v -> {
            String[] options = {"Choose from Gallery", "Take Photo"};
            new AlertDialog.Builder(this)
                    .setTitle("Add Photo")
                    .setItems(options, (whichDialog, which) -> {
                        if (which == 0) {
                            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            galleryLauncher.launch(galleryIntent);
                        } else {
                            requestCameraPermission();
                        }
                    })
                    .show();
        });

        back.setOnClickListener(v -> {
            dialog.dismiss();
            showAddParcelStep4Dialog();
        });

        addparcel.setOnClickListener(v -> {
            int fallbackDrawableResId = R.drawable.photo_mixed;

            if (selectedTags.size() > 1) {
                fallbackDrawableResId = R.drawable.photo_mixed;
            } else if (selectedTags.size() == 1) {
                String tag = selectedTags.get(0).toLowerCase(Locale.ROOT);
                switch (tag) {
                    case "sweets": fallbackDrawableResId = R.drawable.photo_sweets; break;
                    case "fast food": fallbackDrawableResId = R.drawable.photo_fastfood; break;
                    case "bakery": fallbackDrawableResId = R.drawable.photo_bakery; break;
                    case "dairy": fallbackDrawableResId = R.drawable.photo_dairy; break;
                    case "vegetables": fallbackDrawableResId = R.drawable.photo_vegetables; break;
                    case "vegetarian": fallbackDrawableResId = R.drawable.photo_vegetarian; break;
                    case "vegan": fallbackDrawableResId = R.drawable.photo_vegan; break;
                    case "fruits": fallbackDrawableResId = R.drawable.photo_fruits; break;
                    case "liquids": fallbackDrawableResId = R.drawable.photo_liquids; break;
                    case "meals": fallbackDrawableResId = R.drawable.photo_meal; break;
                    case "meat": fallbackDrawableResId = R.drawable.photo_meat; break;
                    default: fallbackDrawableResId = R.drawable.photo_mixed; break;
                }
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = user.getUid();
            String imageFileName = "parcels/" + UUID.randomUUID() + ".jpg";
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(imageFileName);


            if (user == null) {
                Log.e("AUTH", "User is not logged in!");
                return;
            }


            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Uploading...");
            progressDialog.show();

// 🔁 konwersja Drawable do byte[]
/*
Drawable drawable = ContextCompat.getDrawable(this, fallbackDrawableResId);
Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
ByteArrayOutputStream baos = new ByteArrayOutputStream();
bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
byte[] imageBytes = baos.toByteArray();

imageRef.putBytes(imageBytes)
        .continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.e("upload", "Upload failed in continueWithTask: " + e.getMessage(), e);
                throw e;
            }
            return imageRef.getDownloadUrl();
        })
        .addOnSuccessListener(downloadUri -> {
            // Dodaj dane do Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> parcelData = new HashMap<>();
            parcelData.put("title", title);
            parcelData.put("description", description);
            parcelData.put("tags", selectedTags);
            parcelData.put("startTime", startTime);
            parcelData.put("endTime", endTime);
            parcelData.put("day", selectedDay);
            parcelData.put("address", selectedFullAddress);
            parcelData.put("placeId", selectedPlaceId);
            // parcelData.put("imageUrl", downloadUri.toString());
            parcelData.put("userId", userId);
            parcelData.put("phone", phoneNumber);
            parcelData.put("timestamp", FieldValue.serverTimestamp());

            db.collection("parcels")
                    .add(parcelData)
                    .addOnSuccessListener(documentReference -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Parcel added!", Toast.LENGTH_SHORT).show();
                        // Resetuj dane
                        title = "";
                        description = "";
                        selectedTags.clear();
                        startTime = "";
                        endTime = "";
                        selectedFullAddress = "";
                        selectedPlaceId = "";
                        addressToPlaceIdMap.clear();
                        selectedParcelImageUri = null;
                        phoneNumber = "";
                        selectedDay = "today";
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to save parcel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        })
        .addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
*/

// 🟢 Zamiast uploadu zdjęcia — od razu zapisz dane bez pola imageUrl:
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> parcelData = new HashMap<>();
            String dateToSave;
            Calendar calendar = Calendar.getInstance();

            if ("tomorrow".equalsIgnoreCase(selectedDay)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            dateToSave = sdf.format(calendar.getTime());
            parcelData.put("date", dateToSave);
            parcelData.put("title", capitalizeFirstLetter(title.trim()));
            parcelData.put("description", capitalizeFirstLetter(description.trim()));
            parcelData.put("tags", selectedTags);
            parcelData.put("startTime", startTime);
            parcelData.put("endTime", endTime);
            parcelData.put("address", selectedFullAddress);
            parcelData.put("placeId", selectedPlaceId);
// parcelData.put("imageUrl", downloadUri.toString()); // brak zdjęcia
            parcelData.put("userId", userId);
            parcelData.put("phone", phoneNumber);
            parcelData.put("price", price);
            parcelData.put("timestamp", FieldValue.serverTimestamp());
            parcelData.put("status", "active");
            parcelData.put("ownerconfirmed", "no");
            parcelData.put("finished", false);

            /**db.collection("parcels")
                    .add(parcelData)
                    .addOnSuccessListener(documentReference -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Parcel added!", Toast.LENGTH_SHORT).show();
                        String parcelId = documentReference.getId();

                        db.collection("users").document(userId)
                                .update("parcelIds", FieldValue.arrayUnion(parcelId))
                                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Parcel ID added to user"))
                                .addOnFailureListener(e -> Log.e("Firestore", "Failed to update user: " + e.getMessage()));

                        // Resetuj dane
                        title = "";
                        description = "";
                        selectedTags.clear();
                        startTime = "";
                        endTime = "";
                        selectedFullAddress = "";
                        selectedPlaceId = "";
                        addressToPlaceIdMap.clear();
                        selectedParcelImageUri = null;
                        phoneNumber = "";
                        price = 0.0;
                        selectedDay = "today";
                        dialog.dismiss();



                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to save parcel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });**/

            new Handler().postDelayed(() -> {
                db.collection("parcels")
                        .add(parcelData)
                        .addOnSuccessListener(documentReference -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Parcel added!", Toast.LENGTH_SHORT).show();
                            String parcelId = documentReference.getId();

                            db.collection("users").document(userId)
                                    .update("parcelIds", FieldValue.arrayUnion(parcelId))
                                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Parcel ID added to user"))
                                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to update user: " + e.getMessage()));

                            // Resetuj dane
                            title = "";
                            description = "";
                            selectedTags.clear();
                            startTime = "";
                            endTime = "";
                            selectedFullAddress = "";
                            selectedPlaceId = "";
                            addressToPlaceIdMap.clear();
                            selectedParcelImageUri = null;
                            phoneNumber = "";
                            price = 0.0;
                            selectedDay = "today";
                            dialog.dismiss();

                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Failed to save parcel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            },  1000);//60 * 1000); // minuta


        });

        dialog.show();
        dialog.setOnCancelListener(dialogInterface -> {
            title = "";
            description = "";
            selectedTags.clear();
            startTime = "";
            endTime = "";
            selectedFullAddress = "";
            selectedPlaceId = "";
            addressToPlaceIdMap.clear();
            selectedParcelImageUri = null;
            phoneNumber = "";
            price = 0.0;
            selectedDay = "today";
        });
    }

    private void showTimePicker(OnTimeSetListener listener) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

       TimePickerDialog dialog = new TimePickerDialog(
                this,
                R.style.YourTimePickerTheme,
                (view, selectedHour, selectedMinute) -> listener.onTimeSet(selectedHour, selectedMinute),
                hour,
                minute,
                true // 24-hour format
        );
        dialog.show();
    }

    interface OnTimeSetListener {
        void onTimeSet(int hour, int minute);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(cameraIntent);
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(cameraIntent);
    }

    private Uri getImageUriFromDrawable(@DrawableRes int drawableResId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableResId);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        File file = new File(getCacheDir(), "default_parcel_image.png");

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
    }

    private byte[] getBytesFromDrawable(@DrawableRes int drawableResId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableResId);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    private void highlightButton(MaterialButton button, boolean selected) {
        if (selected) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.black)); // ← kolor zaznaczenia
            button.setTextColor(ContextCompat.getColor(this, R.color.green));
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            button.setTextColor(ContextCompat.getColor(this, R.color.black));
        }
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void openUserProfile(String userId) {
        ProfileFragment profileFragment = new ProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putString("userId", userId);
        profileFragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, profileFragment)
                .addToBackStack(null)
                .commit();

        binding.bottomNavigationView.setSelectedItemId(R.id.profile);

    }

}