package id.co.itasoft.monarch.plugin;

import java.io.IOException;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class HolidayCalendarTest {
    private static final String RESPONSE = "{\"success\":true,\"data\":["
            + "{\"date\":\"2026-12-24\",\"name\":\"Cuti Natal\",\"type\":\"leave\"},"
            + "{\"date\":\"2026-12-25\",\"name\":\"Natal\",\"type\":\"holiday\"}]}";

    static class StubCalendar extends HolidayCalendar {
        int requests;
        boolean fail;
        @Override protected String fetch(String url) throws IOException {
            requests++;
            if (fail) throw new IOException("Offline");
            return RESPONSE;
        }
    }

    @Test public void includesNationalHolidaysAndOptionalLeave() throws Exception {
        StubCalendar calendar = new StubCalendar();
        assertEquals("Natal", calendar.getHoliday("", LocalDate.of(2026, 12, 25), false));
        assertNull(calendar.getHoliday("", LocalDate.of(2026, 12, 24), false));
        assertEquals("Cuti Natal", calendar.getHoliday("", LocalDate.of(2026, 12, 24), true));
        assertNull(calendar.getHoliday("", LocalDate.of(2026, 12, 26), true));
        assertEquals(1, calendar.requests);
    }

    @Test public void normalizesBaseUrlAndEndpoint() {
        assertEquals("https://tanggalmerah.upset.dev/api/holidays?year=2026", HolidayCalendar.endpoint("", 2026));
        assertEquals("https://example.org/api/holidays?year=2027", HolidayCalendar.endpoint(" https://example.org/ ", 2027));
        assertEquals("https://example.org/api/holidays?year=2026", HolidayCalendar.endpoint("https://example.org/api/holidays/", 2026));
    }

    @Test public void rejectsUnsafeOrFilteredUrls() {
        for (String url : new String[]{"file:///etc/passwd", "https://example.org?month=1", "https://example.org#x", "https://user:pass@example.org"}) {
            try { HolidayCalendar.endpoint(url, 2026); fail(url); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void rejectsInvalidResponsesAndWrongYear() throws Exception {
        for (String response : new String[]{"{}", "{\"success\":false,\"data\":[]}", "{\"success\":true,\"data\":{}}",
                RESPONSE.replace("2026-12-24", "2026-02-30"), RESPONSE.replace("2026-12-24", "2025-12-24"),
                RESPONSE.replace("leave", "other")}) {
            try { HolidayCalendar.parse(response, 2026); fail(response); }
            catch (Exception expected) { }
        }
    }

    @Test public void doesNotCacheFailures() throws Exception {
        StubCalendar calendar = new StubCalendar();
        calendar.fail = true;
        try { calendar.getHoliday("", LocalDate.of(2026, 12, 25), false); fail(); }
        catch (IOException expected) { }
        calendar.fail = false;
        assertEquals("Natal", calendar.getHoliday("", LocalDate.of(2026, 12, 25), false));
        assertEquals(2, calendar.requests);
    }
}
