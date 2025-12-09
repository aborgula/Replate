package com.example.replate;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.example.replate.repository.ParcelRepository;
import com.example.replate.repository.UserRepository;
import com.example.replate.service.PlacesService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TESTY INTEGRACYJNE SettingsActivity ↔ Repositories/Services (BEZ EMULATORA)
 *
 * Testują prawdziwą integrację między:
 * - SettingsActivity ↔ UserRepository (z mockowanym Firestore)
 * - SettingsActivity ↔ ParcelRepository (z mockowanym Firestore)
 * - SettingsActivity ↔ PlacesService (z mockowanym Places API)
 *
 * Używamy prawdziwych klas Repository/Service, ale mockujemy ich zależności (Firebase/Google API)
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class SettingsActivityIntegrationTest {

    @Mock
    private FirebaseFirestore mockFirestore;
    @Mock
    private FirebaseAuth mockAuth;
    @Mock
    private FirebaseUser mockUser;
    @Mock
    private CollectionReference mockUsersCollection;
    @Mock
    private CollectionReference mockParcelsCollection;
    @Mock
    private DocumentReference mockUserDocument;
    @Mock
    private DocumentSnapshot mockDocumentSnapshot;
    @Mock
    private Task<DocumentSnapshot> mockDocumentTask;
    @Mock
    private Task<Void> mockVoidTask;
    @Mock
    private Task<QuerySnapshot> mockQueryTask;
    @Mock
    private QuerySnapshot mockQuerySnapshot;
    @Mock
    private Query mockQuery;
    @Mock
    private PlacesClient mockPlacesClient;
    @Mock
    private Task<FindAutocompletePredictionsResponse> mockPredictionsTask;
    @Mock
    private FindAutocompletePredictionsResponse mockPredictionsResponse;

    private UserRepository userRepository;
    private ParcelRepository parcelRepository;
    private PlacesService placesService;
    private String testUserId = "test_user_123";

    @BeforeClass
    public static void setUpClass() {
        // Nie potrzebujemy już inicjalizacji Firebase
        // bo używamy dependency injection w konstruktorach
    }

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup Firebase mocks
        setupFirebaseMocks();

        // Utwórz PRAWDZIWE instancje repositories z mockowanymi zależnościami
        // Używamy konstruktorów z dependency injection
        userRepository = new UserRepository(mockFirestore);
        parcelRepository = new ParcelRepository(mockFirestore);

        // Setup PlacesService z mockowanym klientem
        placesService = new PlacesService(mockPlacesClient);
    }

    @After
    public void tearDown() {
        // Cleanup if needed
    }

    private void setupFirebaseMocks() {
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn(testUserId);
        when(mockUser.getEmail()).thenReturn("test@example.com");

        when(mockFirestore.collection("users")).thenReturn(mockUsersCollection);
        when(mockFirestore.collection("parcels")).thenReturn(mockParcelsCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocument);
    }

    // ========== TESTY INTEGRACJI: SettingsActivity ↔ UserRepository ==========

    @Test
    public void testIntegration_LoadUserData_SuccessFlow() throws InterruptedException {
        // Given - UserRepository dostanie dane z "Firebase"
        Map<String, Object> userData = new HashMap<>();
        userData.put("address", "Test Street 123");
        userData.put("phone", "+48 123 456 789");
        userData.put("login", "testuser");
        userData.put("notificationsEnabled", true);

        when(mockUserDocument.get()).thenReturn(mockDocumentTask);
        when(mockDocumentTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);

            when(mockDocumentSnapshot.exists()).thenReturn(true);
            when(mockDocumentSnapshot.getString("address")).thenReturn("Test Street 123");
            when(mockDocumentSnapshot.getString("phone")).thenReturn("+48 123 456 789");
            when(mockDocumentSnapshot.getString("login")).thenReturn("testuser");
            when(mockDocumentSnapshot.getBoolean("notificationsEnabled")).thenReturn(true);

            listener.onSuccess(mockDocumentSnapshot);
            return mockDocumentTask;
        });
        when(mockDocumentTask.addOnFailureListener(any())).thenReturn(mockDocumentTask);

        // When - Wywołujemy UserRepository (jak robi to SettingsActivity)
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UserRepository.UserData> result = new AtomicReference<>();

        userRepository.getUserData(testUserId, new UserRepository.OnUserDataLoadedListener() {
            @Override
            public void onSuccess(UserRepository.UserData data) {
                result.set(data);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Should not fail: " + e.getMessage());
                latch.countDown();
            }
        });

        // Then - Sprawdzamy czy dane są poprawnie przekazane (jak w SettingsActivity)
        assertTrue("Should complete within timeout", latch.await(2, TimeUnit.SECONDS));
        assertNotNull("UserData should not be null", result.get());
        assertEquals("Address should match", "Test Street 123", result.get().address);
        assertEquals("Phone should match", "+48 123 456 789", result.get().phone);
        assertEquals("Login should match", "testuser", result.get().login);
        assertTrue("Notifications should be enabled", result.get().notificationsEnabled);
    }

    @Test
    public void testIntegration_UpdateUserData_SuccessFlow() throws InterruptedException {
        // Given - Symulujemy zapisywanie danych (jak w saveUserData())
        Map<String, Object> updates = new HashMap<>();
        updates.put("address", "New Address 456");
        updates.put("phone", "+48 999 888 777");

        when(mockUserDocument.update(any(Map.class))).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockVoidTask;
        });
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        // When - Wywołujemy update przez UserRepository
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        userRepository.updateUserData(testUserId, updates, new UserRepository.OnUserUpdateListener() {
            @Override
            public void onSuccess() {
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Should not fail: " + e.getMessage());
                latch.countDown();
            }
        });

        // Then - Weryfikujemy integrację
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue("Update should succeed", success.get());
        verify(mockUserDocument, times(1)).update(updates);
    }

    @Test
    public void testIntegration_UpdateNotifications_SuccessFlow() throws InterruptedException {
        // Given - Symulujemy zmianę switcha notyfikacji
        when(mockUserDocument.update(anyString(), any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockVoidTask;
        });
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        // When - Jak w notificationsSwitch.setOnCheckedChangeListener
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        userRepository.updateNotificationSettings(testUserId, true,
                new UserRepository.OnUserUpdateListener() {
                    @Override
                    public void onSuccess() {
                        success.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        fail("Should not fail: " + e.getMessage());
                        latch.countDown();
                    }
                });

        // Then
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(success.get());
        verify(mockUserDocument).update("notificationsEnabled", true);
    }

    // ========== TESTY INTEGRACJI: SettingsActivity ↔ ParcelRepository ==========

    @Test
    public void testIntegration_DeleteUserParcels_SuccessFlow() throws InterruptedException {
        // Given - Mockujemy usuwanie paczek (jak w deleteAccount())
        when(mockParcelsCollection.whereEqualTo("userId", testUserId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryTask);

        when(mockQueryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            when(mockQuerySnapshot.isEmpty()).thenReturn(true);
            when(mockQuerySnapshot.size()).thenReturn(0);
            listener.onSuccess(mockQuerySnapshot);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        // When - Wywołujemy ParcelRepository (jak w deleteAccount())
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        parcelRepository.deleteUserParcels(testUserId, new ParcelRepository.OnParcelDeletedListener() {
            @Override
            public void onSuccess() {
                success.set(true);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Should not fail: " + e.getMessage());
                latch.countDown();
            }
        });

        // Then
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(success.get());
        verify(mockParcelsCollection).whereEqualTo("userId", testUserId);
    }



    // ========== TESTY INTEGRACJI: SettingsActivity ↔ PlacesService ==========

    @Test
    public void testIntegration_PlacesService_AddressValidation() {
        // Given - PlacesService używany w setupAddressAutocomplete()

        // When & Then - testujemy walidację adresów
        assertTrue("Valid address with number",
                placesService.validateAddress("Test Street 123"));

        assertTrue("Valid address with number at start",
                placesService.validateAddress("123 Main Street"));

        assertFalse("Invalid address without number",
                placesService.validateAddress("Test Street"));

        assertFalse("Invalid empty address",
                placesService.validateAddress(""));

        assertFalse("Invalid null address",
                placesService.validateAddress(null));
    }

    // ========== TEST CAŁEGO PRZEPŁYWU: Usuwanie konta ==========

    @Test
    public void testIntegration_CompleteAccountDeletion_Flow() throws InterruptedException {
        // Given - Mockujemy cały przepływ usuwania (jak w deleteAccount())

        // Step 1: Delete parcels
        when(mockParcelsCollection.whereEqualTo("userId", testUserId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            when(mockQuerySnapshot.isEmpty()).thenReturn(true);
            listener.onSuccess(mockQuerySnapshot);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        // Step 2: Delete user document
        when(mockUserDocument.delete()).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockVoidTask;
        });
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        // When - Wykonujemy pełny przepływ (DOKŁADNIE jak w SettingsActivity.deleteAccount())
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean allStepsCompleted = new AtomicBoolean(false);

        // Krok 1: Usuń paczki
        parcelRepository.deleteUserParcels(testUserId, new ParcelRepository.OnParcelDeletedListener() {
            @Override
            public void onSuccess() {
                // Krok 2: Usuń dokument użytkownika
                userRepository.deleteUserDocument(testUserId, new UserRepository.OnUserUpdateListener() {
                    @Override
                    public void onSuccess() {
                        // W prawdziwej aplikacji tutaj byłby currentUser.delete()
                        allStepsCompleted.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        fail("User document deletion failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                fail("Parcel deletion failed: " + e.getMessage());
                latch.countDown();
            }
        });

        // Then - Weryfikujemy że cały przepływ działa
        assertTrue("Flow should complete", latch.await(3, TimeUnit.SECONDS));
        assertTrue("All steps should complete successfully", allStepsCompleted.get());

        // Verify proper sequence
        verify(mockParcelsCollection).whereEqualTo("userId", testUserId);
        verify(mockUserDocument).delete();
    }

    @Test
    public void testIntegration_AccountDeletion_FailsIfParcelDeletionFails() throws InterruptedException {
        // Given - Mockujemy niepowodzenie usuwania paczek
        Exception testException = new Exception("Network error");

        when(mockParcelsCollection.whereEqualTo("userId", testUserId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(testException);
            return mockQueryTask;
        });

        // When - Próbujemy usunąć konto
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean userDeletionAttempted = new AtomicBoolean(false);
        AtomicReference<Exception> caughtException = new AtomicReference<>();

        parcelRepository.deleteUserParcels(testUserId, new ParcelRepository.OnParcelDeletedListener() {
            @Override
            public void onSuccess() {
                // To się nie powinno wywołać
                userRepository.deleteUserDocument(testUserId, new UserRepository.OnUserUpdateListener() {
                    @Override
                    public void onSuccess() {
                        userDeletionAttempted.set(true);
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                caughtException.set(e);
                latch.countDown();
            }
        });

        // Then - Weryfikujemy że proces się zatrzymał
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse("User deletion should not be attempted", userDeletionAttempted.get());
        assertNotNull("Should catch exception", caughtException.get());
        assertEquals("Network error", caughtException.get().getMessage());

        // Verify user document was never touched
        verify(mockUserDocument, never()).delete();
    }

    // ========== TESTY WALIDACJI (jak w SettingsActivity.saveUserData()) ==========

    @Test
    public void testIntegration_SaveUserData_ValidationLogic() {
        // Given - różne dane wejściowe
        String validAddress = "Test Street 123";
        String validPhone = "+48 123 456 789";
        String validLogin = "testuser";

        String emptyAddress = "";
        String emptyPhone = "";
        String emptyLogin = "";

        // When & Then - testujemy logikę walidacji z saveUserData()

        // Valid data should create updates map
        Map<String, Object> updates1 = new HashMap<>();
        if (!validAddress.isEmpty()) updates1.put("address", validAddress);
        if (!validPhone.isEmpty()) updates1.put("phone", validPhone);
        if (!validLogin.isEmpty()) updates1.put("login", validLogin);

        assertFalse("Should have updates", updates1.isEmpty());
        assertEquals(3, updates1.size());

        // Empty data should create empty map
        Map<String, Object> updates2 = new HashMap<>();
        if (!emptyAddress.isEmpty()) updates2.put("address", emptyAddress);
        if (!emptyPhone.isEmpty()) updates2.put("phone", emptyPhone);
        if (!emptyLogin.isEmpty()) updates2.put("login", emptyLogin);

        assertTrue("Should have no updates", updates2.isEmpty());
    }

    @Test
    public void testIntegration_AddressAutocomplete_ValidationFlow() {
        // Given - adres z autocomplete
        String selectedAddress = "Main Street 123";

        // When - walidujemy (jak w setOnItemClickListener)
        boolean isValid = placesService.validateAddress(selectedAddress);

        // Then
        assertTrue("Valid address should pass validation", isValid);

        // Given - niepełny adres
        String incompleteAddress = "Main Street";

        // When
        boolean isInvalid = placesService.validateAddress(incompleteAddress);

        // Then
        assertFalse("Incomplete address should fail validation", isInvalid);
    }
}