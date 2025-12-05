package com.example.replate.service;

import com.example.replate.model.Parcel;
import java.util.ArrayList;
import java.util.List;

public class ParcelFilterService {

    public static class FilterCriteria {
        public String currentUserId;
        public List<String> selectedChips;
        public String selectedDate;
        public int startTimeMinutes;
        public int endTimeMinutes;
        public int selectedDistanceKm;
        public Double filterLat;
        public Double filterLng;

        public FilterCriteria(String currentUserId, List<String> selectedChips,
                              String selectedDate, int startTimeMinutes, int endTimeMinutes) {
            this.currentUserId = currentUserId;
            this.selectedChips = selectedChips != null ? selectedChips : new ArrayList<>();
            this.selectedDate = selectedDate;
            this.startTimeMinutes = startTimeMinutes;
            this.endTimeMinutes = endTimeMinutes;
            this.selectedDistanceKm = 0;
            this.filterLat = 0.0;
            this.filterLng = 0.0;
        }

        public FilterCriteria(String currentUserId, List<String> selectedChips,
                              String selectedDate, int startTimeMinutes, int endTimeMinutes,
                              int selectedDistanceKm, Double filterLat, Double filterLng) {
            this.currentUserId = currentUserId;
            this.selectedChips = selectedChips != null ? selectedChips : new ArrayList<>();
            this.selectedDate = selectedDate;
            this.startTimeMinutes = startTimeMinutes;
            this.endTimeMinutes = endTimeMinutes;
            this.selectedDistanceKm = selectedDistanceKm;
            this.filterLat = filterLat;
            this.filterLng = filterLng;
        }
    }

    public List<Parcel> filterParcels(List<Parcel> parcels, FilterCriteria criteria) {
        List<Parcel> filtered = new ArrayList<>();

        for (Parcel parcel : parcels) {
            if (shouldShowParcel(parcel, criteria)) {
                filtered.add(parcel);
            }
        }

        return filtered;
    }

    private boolean shouldShowParcel(Parcel parcel, FilterCriteria criteria) {
        // Pomijamy paczki użytkownika
        if (parcel.getUserId() != null && parcel.getUserId().equals(criteria.currentUserId)) {
            return false;
        }

        // Filtrowanie po chipach
        if (!passesChipFilter(parcel, criteria.selectedChips)) {
            return false;
        }

        // Filtrowanie po dacie
        if (!passesDateFilter(parcel, criteria.selectedDate)) {
            return false;
        }

        // Filtrowanie po czasie
        if (!passesTimeFilter(parcel, criteria.startTimeMinutes, criteria.endTimeMinutes)) {
            return false;
        }

        return true;
    }

    private boolean passesChipFilter(Parcel parcel, List<String> selectedChips) {
        // Jeśli nie ma filtrów, pokaż wszystko
        if (selectedChips == null || selectedChips.isEmpty()) {
            return true;
        }

        List<String> tags = parcel.getTags();
        if (tags == null) {
            return false;
        }

        // Sprawdź czy któryś tag pasuje
        for (String tag : tags) {
            if (selectedChips.contains(tag)) {
                return true;
            }
        }

        return false;
    }

    private boolean passesDateFilter(Parcel parcel, String selectedDate) {
        String parcelDate = parcel.getDate();

        if (parcelDate == null || selectedDate == null) {
            return false;
        }

        return selectedDate.equals(parcelDate);
    }

    private boolean passesTimeFilter(Parcel parcel, int filterStart, int filterEnd) {
        String parcelStartStr = parcel.getStartTime();
        String parcelEndStr = parcel.getEndTime();

        if (parcelStartStr == null || parcelEndStr == null) {
            return false;
        }

        if (!parcelStartStr.matches("\\d{2}:\\d{2}") || !parcelEndStr.matches("\\d{2}:\\d{2}")) {
            return false;
        }

        int parcelStartMin = timeStringToMinutes(parcelStartStr);
        int parcelEndMin = timeStringToMinutes(parcelEndStr);

        // Paczka jest poza zakresem jeśli kończy się przed filtrem lub zaczyna po filtrze
        return !(parcelEndMin < filterStart || parcelStartMin > filterEnd);
    }

    private int timeStringToMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return hour * 60 + minute;
    }
}