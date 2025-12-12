package com.example.replate.integration;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import com.example.replate.repository.UserRepository;
import com.example.replate.service.RatingService;
import com.google.firebase.firestore.*;
import com.google.android.gms.tasks.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RatingServiceUserRepositoryIntegrationTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private CollectionReference mockUsersCollection;
    @Mock private CollectionReference mockParcelsCollection;

    private RatingService ratingService;
    private UserRepository userRepository;

    @Before
    public void setUp() {
        when(mockFirestore.collection("users")).thenReturn(mockUsersCollection);
        when(mockFirestore.collection("parcels")).thenReturn(mockParcelsCollection);

        ratingService = new RatingService(mockFirestore);
        userRepository = new UserRepository(mockFirestore);
    }

    @Test
    public void testGetParcelOwnerAndLoadUserData_Integration() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        final String[] ownerId = {null};
        final UserRepository.UserData[] userData = {null};

        // Mock paczki
        mockDocument(mockParcelsCollection, "parcel_789", "userId", "author_123");

        // Mock użytkownika
        mockDocument(mockUsersCollection, "author_123", "login", "TestAuthor");

        // Act - test integracji RatingService -> UserRepository
        ratingService.getParcelOwner("parcel_789", new RatingService.ParcelOwnerCallback() {
            @Override
            public void onSuccess(String authorId, String parcelTitle) {
                ownerId[0] = authorId;
                latch.countDown();

                userRepository.getUserData(authorId, new UserRepository.OnUserDataLoadedListener() {
                    @Override
                    public void onSuccess(UserRepository.UserData data) {
                        userData[0] = data;
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onParcelNotFound() { latch.countDown(); latch.countDown(); }

            @Override
            public void onFailure(Exception e) { latch.countDown(); latch.countDown(); }
        });

        // Assert
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("author_123", ownerId[0]);
        assertNotNull(userData[0]);
        assertEquals("TestAuthor", userData[0].login);
    }

    private void mockDocument(CollectionReference collection, String docId, String key, String value) {
        DocumentReference docRef = mock(DocumentReference.class);
        Task<DocumentSnapshot> task = mock(Task.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);

        when(collection.document(docId)).thenReturn(docRef);
        when(docRef.get()).thenReturn(task);

        doAnswer(inv -> {
            OnSuccessListener<DocumentSnapshot> listener = inv.getArgument(0);
            when(snapshot.exists()).thenReturn(true);
            when(snapshot.getString(key)).thenReturn(value);
            listener.onSuccess(snapshot);
            return task;
        }).when(task).addOnSuccessListener(any());

        doAnswer(inv -> task).when(task).addOnFailureListener(any());
    }
}