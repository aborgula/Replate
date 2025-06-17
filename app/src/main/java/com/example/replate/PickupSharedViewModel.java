package com.example.replate;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import java.util.ArrayList;

public class PickupSharedViewModel extends ViewModel {
    private final MutableLiveData<List<String>> selectedChips = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<String>> getSelectedChips() {
        return selectedChips;
    }

    public void setSelectedChips(List<String> chips) {
        selectedChips.setValue(chips);
    }
}

