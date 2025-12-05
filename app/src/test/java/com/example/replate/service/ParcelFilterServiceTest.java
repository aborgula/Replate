package com.example.replate.service;

import com.example.replate.model.Parcel;
import com.example.replate.service.ParcelFilterService;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class ParcelFilterServiceTest {

    private ParcelFilterService service;
    private Parcel parcel1;
    private Parcel parcel2;

    @Before
    public void setUp() {
        service = new ParcelFilterService();

        parcel1 = new Parcel();
        parcel1.setUserId("user1");
        parcel1.setTags(Arrays.asList("fruit", "vege"));
        parcel1.setDate("2025-12-05");
        parcel1.setStartTime("10:00");
        parcel1.setEndTime("12:00");

        parcel2 = new Parcel();
        parcel2.setUserId("user2");
        parcel2.setTags(Arrays.asList("bread"));
        parcel2.setDate("2025-12-05");
        parcel2.setStartTime("11:00");
        parcel2.setEndTime("13:00");
    }

    @Test
    public void testFilterByUserId() {
        // Arrange
        ParcelFilterService.FilterCriteria criteria = new ParcelFilterService.FilterCriteria(
                "user1", null, "2025-12-05", 0, 1440
        );

        // Act
        List<Parcel> filtered = service.filterParcels(Arrays.asList(parcel1, parcel2), criteria);

        // Assert
        assertEquals(1, filtered.size());
        assertEquals("user2", filtered.get(0).getUserId());
    }

    @Test
    public void testFilterByChips() {
        // Arrange
        ParcelFilterService.FilterCriteria criteria = new ParcelFilterService.FilterCriteria(
                "someone", Arrays.asList("bread"), "2025-12-05", 0, 1440
        );

        // Act
        List<Parcel> filtered = service.filterParcels(Arrays.asList(parcel1, parcel2), criteria);

        // Assert
        assertEquals(1, filtered.size());
        assertTrue(filtered.get(0).getTags().contains("bread"));
    }

    @Test
    public void testFilterByTime() {
        // Arrange
        ParcelFilterService.FilterCriteria criteria = new ParcelFilterService.FilterCriteria(
                "someone", null, "2025-12-05", 11 * 60, 14 * 60 // 11:00 - 14:00
        );

        // Act
        List<Parcel> filtered = service.filterParcels(Arrays.asList(parcel1, parcel2), criteria);

        // Assert
        assertEquals(2, filtered.size());
    }
}
