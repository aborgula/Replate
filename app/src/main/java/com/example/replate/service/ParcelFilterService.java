package com.example.replate.service;

import com.example.replate.Model.Parcel;

import java.util.ArrayList;
import java.util.List;

public class ParcelFilterService {

    public static class FilterCriteria {
        public String currentUserId;
        public List<String> selectedChips;
        public String selectedDate;
        public int startTimeMinutes;
        public int endTimeMinutes;

        public FilterCriteria(String currentUserId, List<String> selectedChips,
                              String selectedDate, int startTimeMinutes, int endTimeMinutes) {
            this.currentUserId = currentUserId;
            this.selectedChips = selectedChips != null ? selectedChips : new ArrayList<>();
            this.selectedDate = selectedDate;
            this.startTimeMinutes = startTimeMinutes;
            this.endTimeMinutes = endTimeMinutes;
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
        if (parcel.getUserId() != null && parcel.getUserId().equals(criteria.currentUserId)) {
            return false;
        }

        if (!passesChipFilter(parcel, criteria.selectedChips)) {
            return false;
        }

        if (!passesDateFilter(parcel, criteria.selectedDate)) {
            return false;
        }

        if (!passesTimeFilter(parcel, criteria.startTimeMinutes, criteria.endTimeMinutes)) {
            return false;
        }

        return true;
    }

    private boolean passesChipFilter(Parcel parcel, List<String> selectedChips) {
        if (selectedChips == null || selectedChips.isEmpty()) {
            return true;
        }

        List<String> tags = parcel.getTags();
        if (tags == null) {
            return false;
        }

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

        return !(parcelEndMin < filterStart || parcelStartMin > filterEnd);
    }

    private int timeStringToMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return hour * 60 + minute;
    }
}