package id.co.itasoft.monarch.plugin;

import java.util.HashMap;
import org.joget.apps.form.model.FormData;
import org.junit.Test;
import static org.junit.Assert.*;

public class MonarchDatePickerTest {
    static class Picker extends MonarchDatePicker {
        final java.util.Map<String, Object> testProperties = new HashMap<>();
        @Override public java.util.Map<String, Object> getProperties() { return testProperties; }
        @Override public Object getProperty(String key) { return testProperties.get(key); }
        @Override public String getPropertyString(String key) { return (String) testProperties.getOrDefault(key, ""); }
        @Override public void setProperty(String key, Object value) { testProperties.put(key, value); }
        final HolidayCalendarTest.StubCalendar calendar = new HolidayCalendarTest.StubCalendar();
        @Override protected HolidayCalendar getHolidayCalendar() { return calendar; }
    }
    private Picker picker() {
        Picker picker = new Picker();
        picker.setProperty("id", "date");
        picker.setProperty("format", "mm/dd/yy");
        picker.setProperty("holidayApiEnabled", "true");
        picker.setProperty("holidayAction", "disable");
        return picker;
    }
    private FormData data(String value) {
        FormData data = new FormData();
        data.addRequestParameterValues("date", new String[]{value});
        return data;
    }
    @Test public void rejectsManuallyEnteredNationalHoliday() {
        Picker picker = picker();
        FormData data = data("12/25/2026");
        assertFalse(picker.selfValidate(data));
        assertTrue(data.getFormErrors().get("date").contains("Natal"));
    }
    @Test public void acceptsWorkingDateAndOptionalLeave() {
        Picker picker = picker();
        assertTrue(picker.selfValidate(data("12/26/2026")));
        assertTrue(picker.selfValidate(data("12/24/2026")));
        picker.setProperty("holidayIncludeLeave", "true");
        assertFalse(picker.selfValidate(data("12/24/2026")));
    }
    @Test public void allowsHighlightsAndDisabledIntegrationWithoutRequests() {
        Picker picker = picker();
        picker.setProperty("holidayAction", "highlight");
        assertTrue(picker.selfValidate(data("12/25/2026")));
        picker.setProperty("holidayAction", "disable");
        picker.setProperty("holidayApiEnabled", "");
        assertTrue(picker.selfValidate(data("12/25/2026")));
        assertEquals(0, picker.calendar.requests);
    }
    @Test public void emptyValueAndTimeOnlyDoNotCallApi() {
        Picker picker = picker();
        assertTrue(picker.selfValidate(data("")));
        picker.setProperty("disableSaturday", "true");
        picker.setProperty("disableSunday", "true");
        picker.setProperty("datePickerType", "timeOnly");
        picker.setProperty("format24hr", "true");
        assertTrue(picker.selfValidate(data("13:30")));
        assertEquals(0, picker.calendar.requests);
    }
    @Test public void utcDateTimeIsValidatedWithTimeAndKeptAsUtc() {
        Picker picker = picker();
        picker.setProperty("datePickerType", "utcdateTime");
        picker.setProperty("format24hr", "true");
        assertFalse(picker.selfValidate(data("12/25/2026 13:30")));
        assertTrue(picker.selfValidate(data("12/26/2026 13:30")));
        assertEquals("utcdateTime", picker.getPropertyString("datePickerType"));
    }
    @Test public void rejectsSubmissionWhenApiUnavailable() {
        Picker picker = picker();
        picker.calendar.fail = true;
        FormData data = data("12/26/2026");
        assertFalse(picker.selfValidate(data));
        assertTrue(data.getFormErrors().get("date").contains("tidak tersedia"));
    }
    @Test public void saturdayAndSundayCanBeDisabledIndependentlyWithoutApi() {
        Picker picker = picker();
        picker.setProperty("holidayApiEnabled", "");
        picker.setProperty("disableSaturday", "true");
        FormData saturday = data("12/26/2026");
        assertFalse(picker.selfValidate(saturday));
        assertTrue(saturday.getFormErrors().get("date").contains("Saturday"));
        assertTrue(picker.selfValidate(data("12/27/2026")));
        picker.setProperty("disableSaturday", "");
        picker.setProperty("disableSunday", "true");
        assertTrue(picker.selfValidate(data("12/26/2026")));
        FormData sunday = data("12/27/2026");
        assertFalse(picker.selfValidate(sunday));
        assertTrue(sunday.getFormErrors().get("date").contains("Sunday"));
        assertTrue(picker.selfValidate(data("12/28/2026")));
        assertEquals(0, picker.calendar.requests);
    }
    @Test public void bothWeekendsComposeWithHolidayAndHighlightModes() {
        Picker picker = picker();
        picker.setProperty("disableSaturday", "true");
        picker.setProperty("disableSunday", "true");
        assertFalse(picker.selfValidate(data("12/25/2026")));
        assertFalse(picker.selfValidate(data("12/26/2026")));
        assertFalse(picker.selfValidate(data("12/27/2026")));
        assertTrue(picker.selfValidate(data("12/28/2026")));
        picker.setProperty("holidayAction", "highlight");
        assertTrue(picker.selfValidate(data("12/25/2026")));
        assertFalse(picker.selfValidate(data("12/26/2026")));
    }
    @Test public void weekendRulesApplyToDateTimeAndUtcInUserTimezone() {
        Picker picker = picker();
        picker.setProperty("holidayApiEnabled", "");
        picker.setProperty("disableSaturday", "true");
        picker.setProperty("disableSunday", "true");
        picker.setProperty("format24hr", "true");
        for (String type : new String[]{"dateTime", "utcdateTime"}) {
            picker.setProperty("datePickerType", type);
            assertFalse(picker.selfValidate(data("12/26/2026 00:30")));
            assertFalse(picker.selfValidate(data("12/27/2026 23:30")));
            assertTrue(picker.selfValidate(data("12/28/2026 00:30")));
        }
        assertEquals(0, picker.calendar.requests);
    }

}
