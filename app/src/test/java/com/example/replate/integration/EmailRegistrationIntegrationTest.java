package com.example.replate.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.replate.repository.UserRepository;
import com.example.replate.service.AuthService;
import com.example.replate.service.FCMTokenService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class EmailRegistrationIntegrationTest {

    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseFirestore mockDb;
    @Mock private FCMTokenService mockFcmTokenService;
    @Mock private Task<AuthResult> mockAuthTask;
    @Mock private Task<Void> mockFirestoreTask;
    @Mock private AuthResult mockAuthResult;
    @Mock private FirebaseUser mockUser;
    @Mock private CollectionReference mockUsersCollection;
    @Mock private DocumentReference mockUserDocument;

    private AuthService authService;
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_LOGIN = "testuser";
    private static final String TEST_USER_ID = "user123";
    private static final String TEST_FCM_TOKEN = "fcm_token_123";

    @BeforeEach
    void setUp() {
        when(mockAuth.createUserWithEmailAndPassword(anyString(), anyString()))
                .thenReturn(mockAuthTask);
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocument);
        when(mockUserDocument.set(any(Map.class))).thenReturn(mockFirestoreTask);

        doAnswer(invocation -> {
            FCMTokenService.OnTokenReceivedListener listener = invocation.getArgument(0);
            listener.onSuccess(TEST_FCM_TOKEN);
            return null;
        }).when(mockFcmTokenService).getToken(any());

        authService = new AuthService(mockAuth);
        userRepository = new UserRepository(mockDb, mockFcmTokenService);
    }

    @Test
    void testSuccessfulEmailRegistration_AuthAndRepositoryIntegration() throws InterruptedException {
        when(mockAuthResult.getUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn(TEST_USER_ID);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] savedUserId = {null};

        doAnswer(invocation -> {
            OnCompleteListener<AuthResult> listener = invocation.getArgument(0);
            when(mockAuthTask.isSuccessful()).thenReturn(true);
            when(mockAuthTask.getResult()).thenReturn(mockAuthResult);
            listener.onComplete(mockAuthTask);
            return mockAuthTask;
        }).when(mockAuthTask).addOnCompleteListener(any());

        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockFirestoreTask;
        }).when(mockFirestoreTask).addOnSuccessListener(any());

        doAnswer(invocation -> mockFirestoreTask)
                .when(mockFirestoreTask).addOnFailureListener(any());

        authService.registerWithEmail(TEST_EMAIL, TEST_PASSWORD,
                new AuthService.AuthCallback() {
                    @Override
                    public void onSuccess(String userId) {
                        userRepository.createUser(userId, TEST_LOGIN, TEST_EMAIL,
                                new UserRepository.UserCreationCallback() {
                                    @Override
                                    public void onSuccess() {
                                        success[0] = true;
                                        savedUserId[0] = userId;
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
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback nie wywołany");
        assertTrue(success[0], "Rejestracja powinna się udać");
        assertEquals(TEST_USER_ID, savedUserId[0], "Niepoprawne userId");

        verify(mockAuth).createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD);
        verify(mockFcmTokenService).getToken(any());

        ArgumentCaptor<Map> userDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockUserDocument).set(userDataCaptor.capture());

        Map<String, Object> savedData = userDataCaptor.getValue();
        assertEquals(TEST_LOGIN, savedData.get("login"));
        assertEquals(TEST_EMAIL, savedData.get("email"));
        assertEquals(TEST_FCM_TOKEN, savedData.get("fcmToken"));
        assertNotNull(savedData.get("likedUsers"), "likedUsers powinno istnieć");
    }

    @Test
    void testRegistration_UserDataCompleteness() throws InterruptedException {
        when(mockAuthResult.getUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn(TEST_USER_ID);

        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            OnCompleteListener<AuthResult> listener = invocation.getArgument(0);
            when(mockAuthTask.isSuccessful()).thenReturn(true);
            when(mockAuthTask.getResult()).thenReturn(mockAuthResult);
            listener.onComplete(mockAuthTask);
            return mockAuthTask;
        }).when(mockAuthTask).addOnCompleteListener(any());

        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockFirestoreTask;
        }).when(mockFirestoreTask).addOnSuccessListener(any());

        doAnswer(invocation -> mockFirestoreTask)
                .when(mockFirestoreTask).addOnFailureListener(any());

        authService.registerWithEmail(TEST_EMAIL, TEST_PASSWORD,
                new AuthService.AuthCallback() {
                    @Override
                    public void onSuccess(String userId) {
                        userRepository.createUser(userId, TEST_LOGIN, TEST_EMAIL,
                                new UserRepository.UserCreationCallback() {
                                    @Override
                                    public void onSuccess() {
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
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        ArgumentCaptor<Map> userDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockUserDocument).set(userDataCaptor.capture());

        Map<String, Object> savedData = userDataCaptor.getValue();

        assertTrue(savedData.containsKey("login"), "Brak pola 'login'");
        assertTrue(savedData.containsKey("email"), "Brak pola 'email'");
        assertTrue(savedData.containsKey("fcmToken"), "Brak pola 'fcmToken'");
        assertTrue(savedData.containsKey("parcelIds"), "Brak pola 'parcelIds'");
        assertTrue(savedData.containsKey("likedUsers"), "Brak pola 'likedUsers'");

        assertEquals(TEST_LOGIN, savedData.get("login"));
        assertEquals(TEST_EMAIL, savedData.get("email"));
        assertEquals(TEST_FCM_TOKEN, savedData.get("fcmToken"));
    }
}
