package com.kitbox.tools;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvertToolsServiceTest {

    @Test
    void timestampRoundTripPreservesValue() {
        long now = System.currentTimeMillis();
        String text = ConvertToolsService.timestampToDatetime(String.valueOf(now));
        long back = Long.parseLong(ConvertToolsService.datetimeToTimestamp(text, true));
        assertEquals(now, back);
    }

    @Test
    void secondAndMillisecondTimestampsDescribeSameMoment() {
        String fromSeconds = ConvertToolsService.timestampToDatetime("1700000000");
        String fromMillis = ConvertToolsService.timestampToDatetime("1700000000000");
        assertTrue(fromMillis.startsWith(fromSeconds),
                fromSeconds + " 应是 " + fromMillis + " 的秒级前缀");
    }

    @Test
    void datetimeToTimestampSecondPrecision() {
        long seconds = Long.parseLong(ConvertToolsService.datetimeToTimestamp("2026-09-20 12:00:00", false));
        assertEquals(0, seconds % 1);
        assertTrue(seconds > 1_700_000_000L);
        assertTrue(seconds < 2_000_000_000L);
    }

    @Test
    void datetimeWithMillisAccepted() {
        long ms = Long.parseLong(ConvertToolsService.datetimeToTimestamp("2026-09-20 12:00:00.123", true));
        assertEquals(123, ms % 1000);
    }

    @Test
    void rejectBadInputs() {
        assertThrows(IllegalArgumentException.class, () -> ConvertToolsService.timestampToDatetime("abc"));
        assertThrows(IllegalArgumentException.class,
                () -> ConvertToolsService.datetimeToTimestamp("2026/09/20 12:00:00", false));
    }

    @Test
    void radixConversions() {
        assertEquals("ff", ConvertToolsService.convertRadix("255", 10, 16));
        assertEquals("255", ConvertToolsService.convertRadix("FF", 16, 10));
        assertEquals("-1010", ConvertToolsService.convertRadix("-10", 10, 2));
        assertEquals("35", ConvertToolsService.convertRadix("z", 36, 10));
        assertEquals("777", ConvertToolsService.convertRadix("511", 10, 8));
    }

    @Test
    void radixRejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ConvertToolsService.convertRadix("129", 2, 10));
        assertThrows(IllegalArgumentException.class, () -> ConvertToolsService.convertRadix("10", 1, 10));
        assertThrows(IllegalArgumentException.class, () -> ConvertToolsService.convertRadix("10", 10, 37));
        assertThrows(IllegalArgumentException.class, () -> ConvertToolsService.convertRadix("  ", 10, 16));
    }

    @Test
    void uuidFormatAndCount() {
        List<String> uuids = ConvertToolsService.generateUuids(50, false, false);
        assertEquals(50, uuids.size());
        for (String uuid : uuids) {
            assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"), uuid);
        }
    }

    @Test
    void uuidFormattingOptions() {
        List<String> compact = ConvertToolsService.generateUuids(1, true, true);
        assertEquals(32, compact.get(0).length());
        assertEquals(compact.get(0), compact.get(0).toUpperCase());

        List<String> normal = ConvertToolsService.generateUuids(1, false, false);
        assertEquals(36, normal.get(0).length());
    }

    @Test
    void uuidsAreDistinct() {
        List<String> uuids = ConvertToolsService.generateUuids(200, false, false);
        Set<String> distinct = new HashSet<>(uuids);
        assertEquals(200, distinct.size());
    }

    @Test
    void uuidCountBounds() {
        assertThrows(IllegalArgumentException.class, () -> ConvertToolsService.generateUuids(0, false, false));
        assertThrows(IllegalArgumentException.class, () -> ConvertToolsService.generateUuids(1001, false, false));
    }
}
