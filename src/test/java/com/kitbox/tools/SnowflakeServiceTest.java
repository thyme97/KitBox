package com.kitbox.tools;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeServiceTest {

    @BeforeAll
    static void setUp() {
        SnowflakeService.configure(3, 7);
    }

    @Test
    void idsAreUniqueAndMonotonicallyIncreasing() {
        List<String> ids = SnowflakeService.generate(1000);
        assertEquals(1000, ids.size());
        Set<String> distinct = new HashSet<>(ids);
        assertEquals(1000, distinct.size());
        long prev = -1;
        for (String id : ids) {
            long value = Long.parseLong(id);
            assertTrue(value > prev, "应严格递增: " + id);
            prev = value;
        }
    }

    @Test
    void parseRecoversConfiguredFields() {
        SnowflakeService.configure(9, 5);
        long before = System.currentTimeMillis();
        String id = SnowflakeService.generate(1).get(0);
        SnowflakeService.Parsed parsed = SnowflakeService.parse(id);

        assertEquals(9, parsed.workerId);
        assertEquals(5, parsed.datacenterId);
        assertEquals(Long.parseLong(id), parsed.id);
        assertTrue(Math.abs(parsed.epochMillis - (before + System.currentTimeMillis()) / 2) < 5000,
                "时间戳应接近当前时间");
        assertNotNull(parsed.datetime);
        SnowflakeService.configure(3, 7);
    }

    @Test
    void parseAcceptsHexForm() {
        String id = SnowflakeService.generate(1).get(0);
        SnowflakeService.Parsed fromDec = SnowflakeService.parse(id);
        SnowflakeService.Parsed fromHex = SnowflakeService.parse("0x" + Long.toHexString(Long.parseLong(id)));
        assertEquals(fromDec.id, fromHex.id);
        assertEquals(fromDec.workerId, fromHex.workerId);
        assertEquals(fromDec.epochMillis, fromHex.epochMillis);
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SnowflakeService.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> SnowflakeService.parse("-5"));
        assertThrows(IllegalArgumentException.class, () -> SnowflakeService.parse(""));
        assertThrows(IllegalArgumentException.class, () -> SnowflakeService.generate(0));
        assertThrows(IllegalArgumentException.class, () -> SnowflakeService.configure(32, 0));
        assertThrows(IllegalArgumentException.class, () -> SnowflakeService.configure(0, -1));
    }
}
