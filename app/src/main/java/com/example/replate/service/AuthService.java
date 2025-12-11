package com.example.replate.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthService {
    private final FirebaseAuth auth;

    public AuthService(FirebaseAuth auth) {
        this.auth = auth;
    }

    public interface AuthCallback {
        void onSuccess(String userId);
        void onFailure(Exception e);
    }

    public void registerWithEmail(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        callback.onSuccess(user.getUid());
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
}