package com.example.replate;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
public class FavouritesFragment extends Fragment {

    private boolean shouldShowSwipeHint = false;
    private RecyclerView recyclerView;
    private FavouritesAdapter adapter;
    private List<User> favouriteUsers = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LottieAnimationView emptyAnim;
    private LinearLayout layoutAnimation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favourites, container, false);
        emptyAnim = view.findViewById(R.id.emptyAnimation);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        layoutAnimation = view.findViewById(R.id.layoutanimation);

        recyclerView = view.findViewById(R.id.favouritesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FavouritesAdapter(
                favouriteUsers,
                getContext(),
                (user, position) -> removeUserFromLikedList(user), // swipe
                user -> openUserProfile(user.getUid())
        );
        recyclerView.setAdapter(adapter);

        attachSwipeToDelete();
        loadLikedUsers();

        return view;
    }

    private void loadLikedUsers() {

        favouriteUsers.clear();

        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            List<String> likedUserIds = (List<String>) documentSnapshot.get("likedUsers");

            //if (likedUserIds == null || likedUserIds.isEmpty()) return;

            if (likedUserIds == null || likedUserIds.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                layoutAnimation.setVisibility(View.VISIBLE);
                emptyAnim.playAnimation();
                return;
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                layoutAnimation.setVisibility(View.GONE);
                emptyAnim.cancelAnimation();
            }


            int[] loadCounter = {0}; // pomocnicza zmienna

            for (String uid : likedUserIds) {
                db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        Double rating = userDoc.getDouble("rating");
                        if (rating == null) {
                            rating = 0.0;
                        }

                        String profileImageBase64 = userDoc.getString("profileImageBase64");

                        User user = new User(
                                uid,
                                userDoc.getString("login"),
                                userDoc.getString("photoUrl"),
                                profileImageBase64,
                                rating
                        );
                        favouriteUsers.add(user);
                        adapter.notifyItemInserted(favouriteUsers.size() - 1);

                        loadCounter[0]++;
                        if (loadCounter[0] == likedUserIds.size()) {
                            recyclerView.postDelayed(this::animateFirstItemSwipeHint, 600); // animacja po załadowaniu
                        }
                    }
                });
            }
        });
    }



    private void removeUserFromLikedList(User user) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            List<String> likedUserIds = (List<String>) documentSnapshot.get("likedUsers");

            if (likedUserIds != null && likedUserIds.contains(user.getUid())) {
                likedUserIds.remove(user.getUid());

                db.collection("users").document(currentUserId)
                        .update("likedUsers", likedUserIds);
            }
        });
    }



    private void attachSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        User user = favouriteUsers.get(position);

                        // 1. Usuń lokalnie
                        favouriteUsers.remove(position);
                        adapter.notifyItemRemoved(position);

                        // 2. Snackbar z "Cofnij"
                        Snackbar.make(recyclerView, "Deleted " + user.getUsername(), Snackbar.LENGTH_LONG)
                                .setAction("Cancel", v -> {
                                    // Przywróć użytkownika
                                    favouriteUsers.add(position, user);
                                    adapter.notifyItemInserted(position);
                                    recyclerView.scrollToPosition(position);
                                })
                                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        // 3. Jeśli nie cofnięto — usuń z Firestore
                                        if (event != DISMISS_EVENT_ACTION) {
                                            removeUserFromLikedList(user);
                                        }
                                    }
                                }).show();
                    }


                    @Override
                    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

                        View itemView = viewHolder.itemView;

                        Paint paint = new Paint();
                        paint.setColor(ContextCompat.getColor(getContext(), R.color.green));

                        Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_bin);
                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;

                        if (dX > 0) {
                            // Swipe w prawo
                            c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(),
                                    dX, (float) itemView.getBottom(), paint);

                            int iconLeft = itemView.getLeft() + iconMargin;
                            int iconRight = iconLeft + icon.getIntrinsicWidth();
                            int iconTop = itemView.getTop() + iconMargin;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();

                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        } else if (dX < 0) {
                            // Swipe w lewo
                            c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                                    (float) itemView.getRight(), (float) itemView.getBottom(), paint);

                            int iconRight = itemView.getRight() - iconMargin;
                            int iconLeft = iconRight - icon.getIntrinsicWidth();
                            int iconTop = itemView.getTop() + iconMargin;
                            int iconBottom = iconTop + icon.getIntrinsicHeight();

                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }

                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }
                };

        new ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(recyclerView);
    }

    private void animateFirstItemSwipeHint() {
        if (recyclerView != null && recyclerView.getChildCount() > 0) {
            View firstItemView = recyclerView.getChildAt(0);

            if (firstItemView != null) {
                firstItemView.animate()
                        .translationX(-100f) // lekki ruch w lewo
                        .setDuration(300)
                        .withEndAction(() -> firstItemView.animate()
                                .translationX(0f) // wraca na miejsce
                                .setDuration(300)
                                .start())
                        .start();
            }
        }
    }

    private void openUserProfile(String userId) {
        ProfileFragment profileFragment = new ProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putString("userId", userId);
        profileFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, profileFragment)
                .addToBackStack(null)
                .commit();
    }


}
