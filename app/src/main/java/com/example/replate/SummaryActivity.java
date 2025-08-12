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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import android.content.Intent;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SummaryActivity extends AppCompatActivity {
    private String selectedPaymentMethod = null;  // albo np. "cash" / "przelewy24"


    String parcelId, title, description, startTime, endTime, date, address, creatorId;
    double price;
    ImageView parcel_image, arrowBack, paymentImage;
    TextView titleParcel, pickupLabel, timeLabel, payment, priceText, reservebuybtn;
    LinearLayout paymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

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
        date = getIntent().getStringExtra("date");  // UWAGA: wcześniej był "day" w Intent
        address = getIntent().getStringExtra("address");
        price = getIntent().getDoubleExtra("price", 0.0);
        creatorId = getIntent().getStringExtra("creatorId");

        titleParcel.setText(title != null ? title : "No title");
        paymentMethod.setOnClickListener(v -> showPaymentBottomSheet());

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
                    // Obsługa płatności online
                    Toast.makeText(this, "Płatność online jeszcze nieobsługiwana", Toast.LENGTH_SHORT).show();
                    // TODO: przekierowanie do płatności
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

        transferLayout.setOnClickListener(v -> {
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


}
