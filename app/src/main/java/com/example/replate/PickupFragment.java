package com.example.replate;

import android.graphics.Color;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.MapFragment;

public class PickupFragment extends Fragment {

    private TextView listButton, mapButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_pickup, container, false);

        listButton = root.findViewById(R.id.list);
        mapButton = root.findViewById(R.id.map);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.listormap, new FragmentList())
                .commit();

        listButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.listormap, new FragmentList())
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


        return root;
    }
}
