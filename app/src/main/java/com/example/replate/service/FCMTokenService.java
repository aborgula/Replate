package com.example.replate.service;


import com.google.firebase.messaging.FirebaseMessaging;

public class FCMTokenService {

    public interface OnTokenReceivedListener {
        void onSuccess(String token);
        void onFailure(Exception e);
    }

    public void getToken(OnTokenReceivedListener listener) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        listener.onSuccess(token);
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }
}