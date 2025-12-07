package com.example.replate.repository;

import com.example.replate.model.Parcel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ParcelRepositoryTest {

    @Mock FirebaseFirestore mockFirestore;
    @Mock CollectionReference mockCollection;
    @Mock Query mockQuery;
    @Mock Task<QuerySnapshot> mockTask;
    @Mock QuerySnapshot mockSnapshot;
    @Mock QueryDocumentSnapshot doc1;
    @Mock QueryDocumentSnapshot doc2;

    ParcelRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockFirestore.collection("parcels")).thenReturn(mockCollection);
        when(mockCollection.whereEqualTo("status", "active")).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        repository = new ParcelRepository(mockFirestore);
    }

    @Test
    public void getActiveParcels_Success_ReturnsListOfParcels() {
        // Parcel 1
        Parcel p1 = new Parcel(); p1.setTitle("Parcel 1");
        when(doc1.toObject(Parcel.class)).thenReturn(p1);
        when(doc1.getId()).thenReturn("id1");

        // Parcel 2
        Parcel p2 = new Parcel(); p2.setTitle("Parcel 2");
        when(doc2.toObject(Parcel.class)).thenReturn(p2);
        when(doc2.getId()).thenReturn("id2");

        when(mockSnapshot.iterator()).thenReturn(Arrays.asList(doc1, doc2).iterator());

        // Przechwytujemy listener
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> successCaptor =
                ArgumentCaptor.forClass(OnSuccessListener.class);

        when(mockTask.addOnSuccessListener(successCaptor.capture()))
                .thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);

        // Mock listenera repozytorium
        ParcelRepository.OnParcelsLoadedListener listener =
                mock(ParcelRepository.OnParcelsLoadedListener.class);

        // Wywołanie funkcji
        repository.getActiveParcels(listener);

        // Symulujemy sukces Firebase
        successCaptor.getValue().onSuccess(mockSnapshot);

        // Sprawdzenie wyniku
        ArgumentCaptor<List<Parcel>> parcelCaptor = ArgumentCaptor.forClass(List.class);
        verify(listener).onSuccess(parcelCaptor.capture());

        List<Parcel> result = parcelCaptor.getValue();

        assertEquals(2, result.size());
        assertEquals("id1", result.get(0).getId());
        assertEquals("Parcel 1", result.get(0).getTitle());
        assertEquals("id2", result.get(1).getId());
        assertEquals("Parcel 2", result.get(1).getTitle());
    }
}
