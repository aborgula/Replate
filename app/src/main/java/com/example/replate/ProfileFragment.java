package com.example.replate;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;

public class ProfileFragment extends Fragment {

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    private ActivityResultLauncher<Intent> takePictureLauncher;

    private ImageView profileImage, star1, star2, star3, star4, star5;
    private Uri imageUri;
    private TextView noreviews;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private CardView logoutBtn, settingsbtn, packagesbtn, reviewsbtn;
    private TextView userLogin;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        profileImage = view.findViewById(R.id.profile_image);
        star1 = view.findViewById(R.id.star1);
        star2 = view.findViewById(R.id.star2);
        star3 = view.findViewById(R.id.star3);
        star4 = view.findViewById(R.id.star4);
        star5 = view.findViewById(R.id.star5);
        noreviews = view.findViewById(R.id.noreviews);
        logoutBtn = view.findViewById(R.id.logoutbtn);
        settingsbtn = view.findViewById(R.id.settingsbtn);
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        userLogin = view.findViewById(R.id.userLogin);
        packagesbtn = view.findViewById(R.id.packagesbtn);
        reviewsbtn = view.findViewById(R.id.reviewsbtn);

        String passedUserId = null;
        Bundle args = getArguments();
        if (args != null) {
            passedUserId = args.getString("userId"); // <-- z FavouritesFragment
        }

        String finalUserId;
        if (passedUserId != null) {
            finalUserId = passedUserId; // pokazujemy innego użytkownika
            logoutBtn.setVisibility(View.GONE);
            settingsbtn.setVisibility(View.GONE);
            profileImage.setClickable(false);     // nie pozwalaj zmieniać zdjęcia
        } else {
            currentUser = FirebaseAuth.getInstance().getCurrentUser();
            finalUserId = currentUser.getUid();   // pokazujemy własny profil
        }

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        profileImage.setImageBitmap(imageBitmap);
                        uploadImageAsBase64(imageBitmap);
                    }
                }
        );


        firestore.collection("users").document(finalUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String base64Image = documentSnapshot.getString("profileImageBase64");
                        String userLoginString = documentSnapshot.getString("login");
                        userLogin.setText(userLoginString);
                        if (base64Image != null) {
                            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            profileImage.setImageBitmap(decodedBitmap);
                        }
                    }

                    loadUserRating(finalUserId);
                });

        packagesbtn.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), PackagesActivity.class);

            // Sprawdź, czy to profil innego użytkownika (czyli czy przekazano userId w Bundle)
            if (getArguments() != null && getArguments().containsKey("userId")) {
                String userId = getArguments().getString("userId");
                intent.putExtra("userId", userId); // inny użytkownik
            } else if (currentUser != null) {
                intent.putExtra("userId", currentUser.getUid()); // własny profil
            }

            startActivity(intent);
        });

        reviewsbtn.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ReviewsActivity.class);

            // Sprawdź, czy to profil innego użytkownika (czyli czy przekazano userId w Bundle)
            if (getArguments() != null && getArguments().containsKey("userId")) {
                String userId = getArguments().getString("userId");
                intent.putExtra("userId", userId); // inny użytkownik
            } else if (currentUser != null) {
                intent.putExtra("userId", currentUser.getUid()); // własny profil
            }

            startActivity(intent);
        });


        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            // Wyczyść SharedPreferences, jeśli trzeba
            requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("isLoggedIn", false)
                    .apply();

            // Przejdź do ekranu logowania
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        settingsbtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        profileImage.setOnClickListener(v -> {
            String[] options = {"Choose from gallery", "Take a photo"};
            new AlertDialog.Builder(getContext())
                    .setTitle("Change profile picture")
                    .setItems(options, (dialogInterface, which) -> {
                        if (which == 0) {
                            pickImageFromGallery();
                        } else {
                            // Sprawdź, czy zgoda już jest
                            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED) {
                                openCamera();
                            } else {
                                // poproś o zgodę
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                            }
                        }
                    }).show();
        });


        if (imageUri != null) {
            uploadImageToFirebase(imageUri);
        }

        return view;
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void takePhotoWithCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else { Toast.makeText(getContext(),
                "No camera app found", Toast.LENGTH_SHORT).show(); } }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                imageUri = data.getData();
                profileImage.setImageURI(imageUri);

                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                    uploadImageAsBase64(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                profileImage.setImageBitmap(imageBitmap);

                uploadImageAsBase64(imageBitmap);
            }
        }
    }


    private void uploadImageToFirebase(Uri imageUri) {
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        StorageReference storageRef = storage.getReference().child("profile_pictures/" + userId + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();

                    // Zapisz URL do Firestore
                    firestore.collection("users").document(userId)
                            .update("profileImageUrl", downloadUrl)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating profile picture URL", e));
                }))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void uploadImageAsBase64(Bitmap bitmap) {
        if (currentUser == null) return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos); // zmniejszamy rozmiar
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        firestore.collection("users").document(currentUser.getUid())
                .update("profileImageBase64", encodedImage)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving image", e));
    }

   private void loadUserRating(String userId) {
       firestore.collection("users").document(userId)
               .get()
               .addOnSuccessListener(documentSnapshot -> {
                   if (documentSnapshot.exists()) {
                       Double rating = documentSnapshot.getDouble("rating");

                       if (rating == null) {
                           rating = 6.0; // domyślnie
                       }

                       displayStars(rating);
                   }
               })
               .addOnFailureListener(e -> {
                   Log.e("Firestore", "Error fetching rating", e);
                   displayStars(6.0); // domyślnie przy błędzie
               });
   }


    private void displayStars(double rating) {
        ImageView[] stars = {star1, star2, star3, star4, star5};


        if (rating == 6) {
            for (ImageView star : stars) {
                star.setImageResource(R.drawable.no_review);
                ViewGroup.LayoutParams params = star.getLayoutParams();
                params.width = dpToPx(40);
                params.height = dpToPx(40);
                star.setLayoutParams(params);
            }
            noreviews.setVisibility(View.VISIBLE);
        } else {
            noreviews.setVisibility(View.GONE);

            for (int i = 0; i < 5; i++) {
                ImageView star = stars[i];
                ViewGroup.LayoutParams params = star.getLayoutParams();

                if (rating >= i + 1) {
                    star.setImageResource(R.drawable.star_full);
                    params.width = dpToPx(40);
                    params.height = dpToPx(40);
                } else if (rating >= i + 0.5) {
                    star.setImageResource(R.drawable.star_half);
                    params.width = dpToPx(40);
                    params.height = dpToPx(40);
                } else {
                    star.setImageResource(R.drawable.star_empty);
                    params.width = dpToPx(45);
                    params.height = dpToPx(45);
                }

                star.setLayoutParams(params);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(getContext(), "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }


}