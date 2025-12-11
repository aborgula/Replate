package com.example.replate.integration;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.replate.repository.UserRepository;
import com.example.replate.service.AuthService;
import com.example.replate.service.FCMTokenService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class EmailRegistrationIntegrationTest {

    @Mock
    private FirebaseAuth mockAuth;

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private FCMTokenService mockFcmTokenService; // Mock FCM zamiast prawdziwego

    @Mock
    private Task<AuthResult> mockAuthTask;

    @Mock
    private Task<Void> mockFirestoreTask;

    @Mock
    private AuthResult mockAuthResult;

    @Mock
    private FirebaseUser mockUser;

    @Mock
    private CollectionReference mockUsersCollection;

    @Mock
    private DocumentReference mockUserDocument;

    private AuthService authService;
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_LOGIN = "testuser";
    private static final String TEST_USER_ID = "user123";
    private static final String TEST_FCM_TOKEN = "fcm_token_123";

    @Before
    public void setUp() {
        when(mockAuth.createUserWithEmailAndPassword(anyString(), anyString()))
                .thenReturn(mockAuthTask);
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDocument);
        when(mockUserDocument.set(any(Map.class))).thenReturn(mockFirestoreTask);

        // Mock FCM token service
        doAnswer(invocation -> {
            FCMTokenService.OnTokenReceivedListener listener = invocation.getArgument(0);
            listener.onSuccess(TEST_FCM_TOKEN);
            return null;
        }).when(mockFcmTokenService).getToken(any());

        // Prawdziwe instancje z wstrzykniętym mockiem FCM
        authService = new AuthService(mockAuth);
        userRepository = new UserRepository(mockDb, mockFcmTokenService);
    }

    @Test
    public void testSuccessfulEmailRegistration_AuthAndRepositoryIntegration()
            throws InterruptedException {


        when(mockAuthResult.getUser()).thenReturn(mockUser);
        when(mockUser.getUid()).thenReturn(TEST_USER_ID);
        CountDownLatch latch = new CountDownLatch(1);


        // Mock Auth success
        doAnswer(invocation -> {
            OnCompleteListener<AuthResult> listener = invocation.getArgument(0);
            when(mockAuthTask.isSuccessful()).thenReturn(true);
            when(mockAuthTask.getResult()).thenReturn(mockAuthResult);
            listener.onComplete(mockAuthTask);
            return mockAuthTask;
        }).when(mockAuthTask).addOnCompleteListener(any());


        // Mock Firestore success
        doAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockFirestoreTask;
        }).when(mockFirestoreTask).addOnSuccessListener(any());


        // Act
        authService.registerWithEmail(TEST_EMAIL, TEST_PASSWORD,
                userId -> userRepository.createUser(userId, TEST_LOGIN, TEST_EMAIL,
                        new UserRepository.UserCreationCallback() {
                            @Override
                            public void onSuccess() {
                                latch.countDown();
                            }


                            @Override
                            public void onFailure(Exception e) {
                                latch.countDown();
                            }
                        }),
                e -> latch.countDown()
        );


        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS));


        verify(mockAuth).createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD);
        verify(mockFcmTokenService).getToken(any());


        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockUserDocument).set(captor.capture());


        Map<String, Object> data = captor.getValue();
        assertEquals(TEST_LOGIN, data.get("login"));
        assertEquals(TEST_EMAIL, data.get("email"));
        assertEquals(TEST_FCM_TOKEN, data.get("fcmToken"));
        assertNotNull(data.get("likedUsers"));}


    @Test
    public void testSuccessfulEmailRegistration_AuthAndRepositoryIntegration()
            throws InterruptedException {
        // Arrange
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

        // Act
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

        // Assert
        assertTrue("Callback nie wywołany", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Rejestracja powinna się udać", success[0]);
        assertEquals("Niepoprawne userId", TEST_USER_ID, savedUserId[0]);

        verify(mockAuth).createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD);
        verify(mockFcmTokenService).getToken(any());

        ArgumentCaptor<Map> userDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockUserDocument).set(userDataCaptor.capture());

        Map<String, Object> savedData = userDataCaptor.getValue();
        assertEquals(TEST_LOGIN, savedData.get("login"));
        assertEquals(TEST_EMAIL, savedData.get("email"));
        assertEquals(TEST_FCM_TOKEN, savedData.get("fcmToken"));
        assertNotNull("likedUsers powinno istnieć", savedData.get("likedUsers"));
    }


    @Test
    public void testRegistration_AuthFails_NoFirestoreCall()
            throws InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] capturedError = {null};

        Exception authError = new Exception("Email already exists");

        doAnswer(invocation -> {
            OnCompleteListener<AuthResult> listener = invocation.getArgument(0);
            when(mockAuthTask.isSuccessful()).thenReturn(false);
            when(mockAuthTask.getException()).thenReturn(authError);
            listener.onComplete(mockAuthTask);
            return mockAuthTask;
        }).when(mockAuthTask).addOnCompleteListener(any());

        // Act
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
                        capturedError[0] = e;
                        latch.countDown();
                    }
                });

        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull("Powinien być błąd Auth", capturedError[0]);
        assertEquals("Email already exists", capturedError[0].getMessage());

        verify(mockAuth).createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD);
        verify(mockUserDocument, never()).set(any(Map.class));
        verify(mockFcmTokenService, never()).getToken(any());
    }

    @Test
    public void testRegistration_UserDataCompleteness()
            throws InterruptedException {
        // Arrange
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

        // Act
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

        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        ArgumentCaptor<Map> userDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockUserDocument).set(userDataCaptor.capture());

        Map<String, Object> savedData = userDataCaptor.getValue();

        assertTrue("Brak pola 'login'", savedData.containsKey("login"));
        assertTrue("Brak pola 'email'", savedData.containsKey("email"));
        assertTrue("Brak pola 'fcmToken'", savedData.containsKey("fcmToken"));
        assertTrue("Brak pola 'parcelIds'", savedData.containsKey("parcelIds"));
        assertTrue("Brak pola 'likedUsers'", savedData.containsKey("likedUsers"));

        assertEquals(TEST_LOGIN, savedData.get("login"));
        assertEquals(TEST_EMAIL, savedData.get("email"));
        assertEquals(TEST_FCM_TOKEN, savedData.get("fcmToken"));
    }
}