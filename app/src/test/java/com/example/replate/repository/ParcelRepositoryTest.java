package com.example.replate.repository;

import com.example.replate.model.Parcel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParcelRepositoryTest {

    @Mock private FirebaseFirestore db;
    @Mock private CollectionReference col;
    @Mock private Query query;
    @Mock private Task<QuerySnapshot> task;
    @Mock private QuerySnapshot snapshot;

    @Mock private ParcelRepository.OnParcelsLoadedListener listener;

    @BeforeEach
    void setUp() {
        when(db.collection("parcels")).thenReturn(col);
        when(col.whereEqualTo("status", "active")).thenReturn(query);
        when(query.get()).thenReturn(task);
    }

    @Test
    void getActiveParcels_Success_ReturnsList() {
        // Arrange - mock dokumentów
        QueryDocumentSnapshot d1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot d2 = mock(QueryDocumentSnapshot.class);

        Parcel p1 = new Parcel(); p1.setTitle("Parcel 1");
        Parcel p2 = new Parcel(); p2.setTitle("Parcel 2");

        when(d1.toObject(Parcel.class)).thenReturn(p1);
        when(d1.getId()).thenReturn("id1");
        when(d2.toObject(Parcel.class)).thenReturn(p2);
        when(d2.getId()).thenReturn("id2");

        when(snapshot.iterator()).thenReturn(Arrays.asList(d1, d2).iterator());

        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> captor =
                ArgumentCaptor.forClass(OnSuccessListener.class);

        when(task.addOnSuccessListener(captor.capture())).thenReturn(task);
        when(task.addOnFailureListener(any())).thenReturn(task);

        ParcelRepository repo = new ParcelRepository(db);

        // Act
        repo.getActiveParcels(listener);
        captor.getValue().onSuccess(snapshot);

        // Assert
        ArgumentCaptor<List<Parcel>> parcelsCaptor = ArgumentCaptor.forClass(List.class);
        verify(listener).onSuccess(parcelsCaptor.capture());

        List<Parcel> result = parcelsCaptor.getValue();

        assertEquals(2, result.size());
        assertEquals("id1", result.get(0).getId());
        assertEquals("Parcel 1", result.get(0).getTitle());
    }
}
