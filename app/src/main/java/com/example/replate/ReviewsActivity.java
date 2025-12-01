package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ReviewsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private List<Review> reviewList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;
    private ImageView arrowBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        recyclerView = findViewById(R.id.reviewsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        arrowBack = findViewById(R.id.arrowback);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        userId = getIntent().getStringExtra("userId");
        if (userId == null && currentUser != null) {
            userId = currentUser.getUid();
        }

        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new ReviewAdapter(reviewList);
        recyclerView.setAdapter(adapter);
        arrowBack.setOnClickListener(v -> finish());

        loadReviewsForUser(userId);
    }

    private void loadReviewsForUser(String userId) {
        db.collection("ratings")
                .whereEqualTo("authorId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reviewList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Long valueLong = doc.getLong("value");
                        String description = doc.getString("description");
                        String reviewUserId = doc.getString("userId");
                        String parcelId = doc.getString("parcelId");
                        String parcelTitle = doc.getString("parcelTitle"); // z dokumentu rating

                        int value = (valueLong != null) ? valueLong.intValue() : 0;

                        Review review = new Review(value, description, reviewUserId, null, parcelId, parcelTitle);
                        review.setParcelId(parcelId);

                        reviewList.add(review);
                        // Pobranie loginu autora z users
                        db.collection("users").document(reviewUserId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String login = userDoc.getString("login");
                                        review.setAuthorLogin(login);
                                        adapter.notifyDataSetChanged();
                                    }
                                });


                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading reviews", Toast.LENGTH_SHORT).show();
                });
    }
}
