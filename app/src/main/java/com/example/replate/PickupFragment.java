package com.example.replate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

        // 🔽 ZMIANA: przekazujemy parcelLauncher do FragmentList
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
            // 🔽 ZMIANA: nowy fragment + przekazanie parcelLauncher
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

        within.setText("Within " + selectedDistanceKm + " km");

        if (lastPlaceName != null) {
            chosenLocation.setText(lastPlaceName);
        } else {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        chosenLocation.setText(String.format(Locale.getDefault(), "%.4f, %.4f",
                                location.getLatitude(), location.getLongitude()));
                    } else {
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
            Log.d("selectedtags", "w pickupfragment ");
        }
    }

    private void deleteExpiredParcels() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        db.collection("parcels")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String parcelDate = doc.getString("date");

                        if (parcelDate != null && parcelDate.compareTo(today) < 0) {
                            db.collection("parcels").document(doc.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> Log.d("DeleteParcel", "Deleted expired parcel: " + doc.getId()))
                                    .addOnFailureListener(e -> Log.e("DeleteParcel", "Failed to delete: " + doc.getId(), e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("DeleteParcel", "Failed to fetch parcels", e));
    }
}
