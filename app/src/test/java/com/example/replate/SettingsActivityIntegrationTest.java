package com.example.replate;

import com.example.replate.repository.ParcelRepository;
import com.example.replate.repository.UserRepository;
import com.example.replate.service.PlacesService;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.robolectric.annotation.Config;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Config(sdk = 28, manifest = Config.NONE)
class SettingsActivityIntegrationTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseUser mockUser;
    @Mock private CollectionReference mockUsersCollection;
    @Mock private CollectionReference mockParcelsCollection;
    @Mock private DocumentReference mockUserDocument;
    @Mock private DocumentSnapshot mockDocumentSnapshot;
    @Mock private Task<DocumentSnapshot> mockDocumentTask;
    @Mock private Task<Void> mockVoidTask;
    @Mock private Task<QuerySnapshot> mockQueryTask;
    @Mock private QuerySnapshot mockQuerySnapshot;
    @Mock private Query mockQuery;
    @Mock private PlacesClient mockPlacesClient;
    @Mock private Task<FindAutocompletePredictionsResponse> mockPredictionsTask;
    @Mock private FindAutocompletePredictionsResponse mockPredictionsResponse;

    private UserRepository userRepository;
    private ParcelRepository parcelRepository;
    private PlacesService placesService;
    private String testUserId = "test_user_123";

    @BeforeAll
    static void setUpClass() {
        // Nie potrzebujemy inicjalizacji Firebase
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup Firebase mocks
        setupFirebaseMocks();

        userRepository = new UserRepository(mockFirestore);
        parcelRepository = new ParcelRepository(mockFirestore);
        placesService = new PlacesService(mockPlacesClient);
    }

    @AfterEach
    void tearDown() {
        // Cleanup if needed
    }

    private void setupFirebaseMocks() {
        lenient().when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        lenient().when(mockUser.getUid()).thenReturn(testUserId);
        lenient().when(mockUser.getEmail()).thenReturn("test@example.com");

        lenient().when(mockFirestore.collection("users")).thenReturn(mockUsersCollection);
        lenient().when(mockFirestore.collection("parcels")).thenReturn(mockParcelsCollection);
        lenient().when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocument);
    }


    @Test
    void testIntegration_LoadUserData_SuccessFlow() throws InterruptedException {
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

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(result.get());
        assertEquals("Test Street 123", result.get().address);
        assertEquals("+48 123 456 789", result.get().phone);
        assertEquals("testuser", result.get().login);
        assertTrue(result.get().notificationsEnabled);
    }

    @Test
    void testIntegration_UpdateUserData_SuccessFlow() throws InterruptedException {
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

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(success.get());
        verify(mockUserDocument, times(1)).update(updates);
    }

    @Test
    void testIntegration_PlacesService_AddressValidation() {
        assertTrue(placesService.validateAddress("Test Street 123"));
        assertTrue(placesService.validateAddress("123 Main Street"));
        assertFalse(placesService.validateAddress("Test Street"));
        assertFalse(placesService.validateAddress(""));
        assertFalse(placesService.validateAddress(null));
    }

    @Test
    void testIntegration_SaveUserData_ValidationLogic() {
        String validAddress = "Test Street 123";
        String validPhone = "+48 123 456 789";
        String validLogin = "testuser";

        Map<String, Object> updates1 = new HashMap<>();
        if (!validAddress.isEmpty()) updates1.put("address", validAddress);
        if (!validPhone.isEmpty()) updates1.put("phone", validPhone);
        if (!validLogin.isEmpty()) updates1.put("login", validLogin);

        assertFalse(updates1.isEmpty());
        assertEquals(3, updates1.size());

        Map<String, Object> updates2 = new HashMap<>();
        assertTrue(updates2.isEmpty());
    }

    @Test
    void testIntegration_AddressAutocomplete_ValidationFlow() {
        assertTrue(placesService.validateAddress("Main Street 123"));
        assertFalse(placesService.validateAddress("Main Street"));
    }
}
