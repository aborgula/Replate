package com.example.replate;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.HashMap;
import java.util.Map;
import android.content.Intent;
import android.net.Uri;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import android.content.Intent;
import android.widget.Toast;
import com.stripe.android.PaymentConfiguration;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.HashMap;
import java.util.Map;

public class SummaryActivity extends AppCompatActivity {
    private String selectedPaymentMethod = "cash";
    String parcelId, title, description, startTime, endTime, date, address, creatorId;
    double price;
    ImageView parcel_image, arrowBack, paymentImage;
    TextView titleParcel, pickupLabel, timeLabel, payment, priceText, reservebuybtn;
    LinearLayout paymentMethod;
    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        PaymentConfiguration.init(getApplicationContext(), "pk_test_51SPlUkEROr8YYhc1Bs9MH36gpNPQQznGJlRtsDIT2R8kabRWuBOhBsHsPZ22rF8vV6O1UbHOJrwP1gjuIIcZ1hNF00dVRncrQE");
        // Inicjalizuj Payment Sheet
        paymentSheet = new PaymentSheet(this, paymentSheetResult -> {
            onPaymentSheetResult(paymentSheetResult);
        });
        arrowBack = findViewById(R.id.arrowback);
        parcel_image = findViewById(R.id.parcel_image);
        titleParcel = findViewById(R.id.titleParcel);
        pickupLabel = findViewById(R.id.pickupLabel);
        timeLabel = findViewById(R.id.timeLabel);
        paymentImage = findViewById(R.id.paymentImage);
        payment = findViewById(R.id.payment);
        priceText = findViewById(R.id.priceText);
        reservebuybtn = findViewById(R.id.reservebuybtn);
        paymentMethod = findViewById(R.id.paymentMethod);

        arrowBack.setOnClickListener(v -> finish());
        parcelId = getIntent().getStringExtra("parcelId");
        title = getIntent().getStringExtra("title");
        description = getIntent().getStringExtra("description");
        startTime = getIntent().getStringExtra("startTime");
        endTime = getIntent().getStringExtra("endTime");
        date = getIntent().getStringExtra("date");
        address = getIntent().getStringExtra("address");
        price = getIntent().getDoubleExtra("price", 0.0);
        creatorId = getIntent().getStringExtra("creatorId");
        titleParcel.setText(title != null ? title : "No title");
        paymentMethod.setOnClickListener(v -> showPaymentBottomSheet());

        // Sprawdź, czy autor ma Stripe Account
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(creatorId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String stripeAccountId = snapshot.getString("stripeAccountId");
                        if (stripeAccountId == null || stripeAccountId.isEmpty()) {
                            // Brak konta Stripe — ukryj możliwość wyboru Przelewy24
                            paymentMethod.setTag("noStripeAccount");
                            Log.d("SummaryActivity", "Creator has no Stripe account — disabling Przelewy24.");
                        } else {
                            paymentMethod.setTag("hasStripeAccount");
                            Log.d("SummaryActivity", "Creator has Stripe account — Przelewy24 enabled.");
                        }
                    } else {
                        paymentMethod.setTag("noStripeAccount");
                        Log.w("SummaryActivity", "Creator document not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    paymentMethod.setTag("noStripeAccount");
                    Log.e("SummaryActivity", "Error checking Stripe account: " + e.getMessage());
                });


        db.collection("parcels").document(parcelId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(SummaryActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.photo_mixed)
                                    .error(R.drawable.photo_mixed)
                                    .centerCrop()
                                    .into(parcel_image);
                        } else {
                            parcel_image.setImageResource(R.drawable.photo_mixed);
                        }
                    } else {
                        Log.e("SummaryActivity", "Parcel not found in Firestore");
                        parcel_image.setImageResource(R.drawable.photo_mixed);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SummaryActivity", "Error loading parcel image: " + e.getMessage());
                    parcel_image.setImageResource(R.drawable.photo_mixed);
                });


        String pickupText = "Pick up today";
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(Calendar.getInstance().getTime());
            if (!date.equals(today)) {
                pickupText = "Pick up tomorrow";
            }
        }
        pickupLabel.setText(pickupText);

        if (startTime != null && endTime != null) {
            timeLabel.setText(startTime + " - " + endTime);
        } else {
            timeLabel.setText("No time set");
        }

        if (price == 0.0) {
            priceText.setText("Free");
            paymentMethod.setVisibility(View.GONE);
        } else {
            priceText.setText(String.format(Locale.getDefault(), "%.0f zł", price));
        }

        reservebuybtn.setOnClickListener(v -> {
            if (price == 0.0) {
                reserveParcel(); // Paczka darmowa – od razu rezerwujemy
            } else {
                if (selectedPaymentMethod == null) {
                    Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedPaymentMethod.equals("cash")) {
                    reserveParcel(); // Gotówka – też od razu rezerwujemy
                } else if (selectedPaymentMethod.equals("przelewy24")) {
                    createPaymentIntent();
                }
            }
        });
    }

    private void showPaymentBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(SummaryActivity.this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_payment, null);
        bottomSheetDialog.setContentView(sheetView);

        // Znajdź widoki z bottom_sheet_payment.xml
        LinearLayout transferLayout = sheetView.findViewById(R.id.transfer);
        LinearLayout cashLayout = sheetView.findViewById(R.id.cash);

        if ("noStripeAccount".equals(paymentMethod.getTag())) {
            transferLayout.setVisibility(View.GONE);
            Toast.makeText(this, "This user does not support online payments.", Toast.LENGTH_SHORT).show();
        }

        transferLayout.setOnClickListener(v -> {
            selectedPaymentMethod = "przelewy24";
            payment.setText("Przelewy24");
            paymentImage.setImageResource(R.drawable.ic_przelewy24);
            setImageSize(paymentImage, 44); // 44dp
            bottomSheetDialog.dismiss();
        });

        cashLayout.setOnClickListener(v -> {
            selectedPaymentMethod = "cash";
            payment.setText("Cash");
            paymentImage.setImageResource(R.drawable.ic_cash);
            setImageSize(paymentImage, 35); // 35dp
            bottomSheetDialog.dismiss();
        });
        bottomSheetDialog.show();
    }

    // Metoda pomocnicza:
    private void setImageSize(ImageView imageView, int dp) {
        float scale = imageView.getContext().getResources().getDisplayMetrics().density;
        int px = (int) (dp * scale + 0.5f);
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.width = px;
        params.height = px;
        imageView.setLayoutParams(params);
    }

    private void reserveParcel() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        String paymentMethodToSave;
        if (price == 0.0) {
            paymentMethodToSave = "free";
        } else if ("cash".equals(selectedPaymentMethod)) {
            paymentMethodToSave = "cash";
        } else {
            // Rezerwacja dla innych metod płatności nieobsługiwana
            Toast.makeText(this, "Metoda płatności nieobsługiwana", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Zmień status paczki na "inactive"
        db.collection("parcels").document(parcelId)
                .update(
                        "status", "inactive",
                        "payment", paymentMethodToSave
                )
                .addOnSuccessListener(aVoid -> {

                    // 2. Dodaj parcelId do "orders" użytkownika
                    db.collection("users").document(currentUserId)
                            .update("orders", FieldValue.arrayUnion(parcelId))
                            .addOnSuccessListener(unused -> {

                                // 3. Przejdź do HomeActivity z OrdersFragment
                                Intent intent = new Intent(SummaryActivity.this, HomeActivity.class);
                                intent.putExtra("openOrdersFragment", true);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> Log.e("Summary", "Błąd dodawania do orders: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e("Summary", "Błąd zmiany statusu: " + e.getMessage()));
    }

    private void createPaymentIntent() {
        Toast.makeText(this, "Preparing payment...", Toast.LENGTH_SHORT).show();
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("parcelId", parcelId);
        data.put("amount", price);

        functions.getHttpsCallable("createPaymentIntent")
                .call(data)
                .addOnSuccessListener(result -> {

                    if (result == null || result.getData() == null) {
                        Toast.makeText(this, "Error: Empty response from server.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    try {
                        Map<String, Object> response = (Map<String, Object>) result.getData();
                        paymentIntentClientSecret = (String) response.get("clientSecret");

                        if (paymentIntentClientSecret == null || paymentIntentClientSecret.isEmpty()) {
                            Toast.makeText(this, "Error: Missing clientSecret.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        presentPaymentSheet();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Toast.makeText(this, "Error processing server response.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


    private void presentPaymentSheet() {
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Replate")
                .build();

        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration);
    }

    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            // Płatność zakończona sukcesem
            Toast.makeText(this, "Payment finished!", Toast.LENGTH_SHORT).show();
            reserveParcel();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            PaymentSheetResult.Failed failed = (PaymentSheetResult.Failed) paymentSheetResult;
            Toast.makeText(this, "Payment error: " + failed.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
