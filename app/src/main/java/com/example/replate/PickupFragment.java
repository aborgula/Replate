package com.example.replate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;

// ... (importy bez zmian)

public class PickupFragment extends Fragment {

    private TextView listButton, mapButton, chosenLocation, within;
    private LinearLayout filters, chooseRange;

    private final androidx.activity.result.ActivityResultLauncher<Intent> parcelLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                            boolean wasDeleted = result.getData() != null &&
                                    result.getData().getBooleanExtra("deleted", false);

                            if (wasDeleted) {
                                Fragment current = requireActivity()
                                        .getSupportFragmentManager().findFragmentById(R.id.listormap);
                                if (current instanceof FragmentList) {
                                    ((FragmentList) current).loadFilteredParcels();
                                } else if (current instanceof FragmentMap) {
                                    ((FragmentMap) current).reloadMarkers();
                                }
                            }
                        }
                    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_pickup, container, false);

        deleteExpiredParcels();

        TextView textView = root.findViewById(R.id.your_textview_id);
        textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        listButton = root.findViewById(R.id.list);
        mapButton = root.findViewById(R.id.map);
        filters = root.findViewById(R.id.filters);
        chooseRange = root.findViewById(R.id.chooseRange);
        chosenLocation = root.findViewById(R.id.your_textview_id);
        within = root.findViewById(R.id.within);

        filters.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FilterActivity.class);
            startActivity(intent);
        });

        chooseRange.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ChooseLocationRangeActivity.class);
            startActivity(intent);
        });

        FragmentList initialListFragment = new FragmentList();
        initialListFragment.setParcelLauncher(parcelLauncher);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.listormap, initialListFragment)
                .commit();

        ChipGroup chipGroup = root.findViewById(R.id.chipGroup);
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View chipView = chipGroup.getChildAt(i);
            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;
                chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                    updateSelectedChips(chipGroup);
                });
            }
        }

        listButton.setOnClickListener(v -> {
            FragmentList fragmentList = new FragmentList();
            fragmentList.setParcelLauncher(parcelLauncher);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.listormap, fragmentList)
                    .commit();

            listButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.yellow));
            listButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            mapButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.lightgrey));
            mapButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow));
        });

        mapButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.listormap, new FragmentMap())
                    .commit();

            mapButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.yellow));
            mapButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            listButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.lightgrey));
            listButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow));
        });

        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Set<String> savedChips = prefs.getStringSet("selectedChips", new HashSet<>());

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View chipView = chipGroup.getChildAt(i);
            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;
                if (savedChips.contains(chip.getText().toString())) {
                    chip.setChecked(true);
                }
            }
        }

        return root;
    }

    private void updateSelectedChips(ChipGroup chipGroup) {
        List<String> selectedChips = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View chipView = chipGroup.getChildAt(i);
            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;
                if (chip.isChecked()) {
                    selectedChips.add(chip.getText().toString());
                }
            }
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("selectedChips", new HashSet<>(selectedChips));
        editor.apply();

        Fragment currentFragment = requireActivity().getSupportFragmentManager().findFragmentById(R.id.listormap);
        if (currentFragment instanceof FragmentMap) {
            ((FragmentMap) currentFragment).reloadMarkers();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs", getContext().MODE_PRIVATE);
        String lastPlaceName = prefs.getString("lastPlaceName", null);
        int selectedDistanceKm = prefs.getInt("selected_distance_km", 1);
Log.d("rrr", lastPlaceName);
        within.setText("Within " + selectedDistanceKm + " km");

        if (lastPlaceName != null) {
            chosenLocation.setText(lastPlaceName);
            Log.d("rrr", "2: " + lastPlaceName);

        } else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        try {
                            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                            if (addresses != null && !addresses.isEmpty()) {
                                String city = addresses.get(0).getLocality(); // nazwa miejscowości
                                if (city == null) {
                                    city = addresses.get(0).getSubAdminArea(); // np. dzielnica lub gmina
                                }
                                if (city == null) {
                                    city = "Unknown location";
                                }
                                chosenLocation.setText(city);
                            } else {
                                chosenLocation.setText("Unknown location");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            chosenLocation.setText("Unknown location");
                        }
                    }
else {
                        chosenLocation.setText("Unable to fetch location.");
                    }
                });
            } else {
                chosenLocation.setText("App doesn't have permission to access your location.");
            }
        }

        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (fragment instanceof FragmentMap) {
            ((FragmentMap) fragment).reloadMarkers();
        }
    }

    private void deleteExpiredParcels() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        String now = fullFormat.format(Calendar.getInstance().getTime());
        Log.d("DeleteParcel", "Aktualny czas: " + now);

        db.collection("parcels")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String parcelDate = doc.getString("date");       // np. 2025-08-08
                        String parcelTime = doc.getString("endTime");    // np. 00:30
                        String startTime = doc.getString("startTime");   // np. 22:30
                        String parcelId = doc.getId();
                        String userId = doc.getString("userId");

                        if (parcelDate != null && parcelTime != null) {
                            String parcelDateTime = parcelDate + " " + parcelTime;
                            Log.d("DeleteParcel", "Paczka " + parcelId + ": data i godzina końca = " + parcelDateTime);

                            try {
                                Date nowDate = fullFormat.parse(now);
                                Date parcelEndDate = fullFormat.parse(parcelDateTime);

                                // Sprawdzenie, czy koniec jest po północy (endTime < startTime)
                                if (startTime != null) {
                                    Date endOnly = timeFormat.parse(parcelTime);
                                    Date startOnly = timeFormat.parse(startTime);

                                    if (endOnly != null && startOnly != null && endOnly.before(startOnly)) {
                                        Calendar cal = Calendar.getInstance();
                                        cal.setTime(parcelEndDate);
                                        cal.add(Calendar.DAY_OF_MONTH, 1);
                                        parcelEndDate = cal.getTime();
                                        Log.d("DeleteParcel", "Godzina końcowa po północy -> przesunięto na: " + fullFormat.format(parcelEndDate));
                                    }
                                }

                                if (parcelEndDate != null && nowDate != null) {
                                    if (parcelEndDate.before(nowDate)) {
                                        Log.d("DeleteParcel", "Paczka " + parcelId + " jest przeterminowana i zostanie usunięta.");

                                        // Usuń parcelId z users
                                        if (userId != null) {
                                            db.collection("users").document(userId)
                                                    .update("parcelIds", FieldValue.arrayRemove(parcelId))
                                                    .addOnSuccessListener(aVoid -> Log.d("DeleteParcel", "Usunięto parcelId z usera: " + parcelId))
                                                    .addOnFailureListener(e -> Log.e("DeleteParcel", "Błąd przy aktualizacji usera: " + parcelId, e));
                                        }

                                        // Usuń paczkę
                                        db.collection("parcels").document(parcelId)
                                                .delete()
                                                .addOnSuccessListener(aVoid -> Log.d("DeleteParcel", "Usunięto paczkę: " + parcelId))
                                                .addOnFailureListener(e -> Log.e("DeleteParcel", "Błąd przy usuwaniu paczki: " + parcelId, e));
                                    } else {
                                        Log.d("DeleteParcel", "Paczka " + parcelId + " NIE jest przeterminowana.");
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("DeleteParcel", "Błąd parsowania daty dla paczki: " + parcelId, e);
                            }
                        } else {
                            Log.w("DeleteParcel", "Brak daty lub godziny w paczce: " + parcelId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("DeleteParcel", "Nie udało się pobrać paczek", e));
    }

}
