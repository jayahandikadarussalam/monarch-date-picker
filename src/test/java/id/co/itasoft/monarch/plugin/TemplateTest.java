package id.co.itasoft.monarch.plugin;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class TemplateTest {
    public static class Request {
        public String getContextPath() { return "/jw"; }
        public Object getAttribute(String name) { return null; }
    }
    private String render(Map<String, Object> properties) throws Exception {
        properties.put("label", "Monarch Date Picker");
        properties.put("elementUniqueKey", "1");
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> element = new HashMap<>();
        element.put("properties", properties);
        data.put("element", element);
        data.put("elementParamName", "date");
        data.put("decoration", "");
        data.put("displayFormat", "MM/DD/YYYY");
        data.put("clientDatePickerType", "");
        data.put("request", new Request());
        data.put("pluginVersion", "1.1.6");
        Configuration configuration = new Configuration();
        configuration.setDirectoryForTemplateLoading(new File("src/main/resources/templates"));
        Template template = configuration.getTemplate("monarchDatePicker.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }
    @Test public void emptySizeUsesNativeThemeWidthAndLoadsOwnAssets() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("size", "");
        String html = render(properties);
        assertFalse(html.contains("size="));
        assertTrue(html.contains("/MonarchDatePicker/js/monarch-datepicker.js")
                || html.contains(".MonarchDatePicker/js/monarch-datepicker.js"));
        assertFalse(html.contains("MonarchDatePicker/resources/"));
        assertTrue(html.contains("jquery-ui-timepicker-addon.js"));
        assertTrue(html.contains(".monarchDatepicker("));
    }
    @Test public void pluginAssetsUseVersionedUrlsToBypassPreviousReleaseCache() throws Exception {
        String html = render(new HashMap<String, Object>());
        assertTrue(html.contains("MonarchDatePicker/js/monarch-datepicker.js?v=1.1.6"));
        assertTrue(html.contains("MonarchDatePicker/css/datePicker.css?v=1.1.6"));
    }
    @Test public void explicitSizeAndHolidayPropertiesAreRendered() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("size", "40");
        properties.put("holidayApiEnabled", "true");
        properties.put("disableSaturday", "true");
        properties.put("disableSunday", "true");
        properties.put("holidayApiUrl", "https://example.org/\"quote");
        properties.put("holidayAction", "disable");
        String html = render(properties);
        assertTrue(html.contains("size=\"40\""));
        assertTrue(html.contains("disableSaturday: \"true\""));
        assertTrue(html.contains("disableSunday: \"true\""));
        assertTrue(html.contains("holidayApiEnabled: \"true\""));
        assertTrue(html.contains("holidayAction: \"disable\""));
        assertTrue(html.contains("\\\"quote"));
    }
    @Test public void readonlyLabelDoesNotInitializeCalendar() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("readonly", "true");
        properties.put("readonlyLabel", "true");
        String html = render(properties);
        assertTrue(html.contains("type=\"hidden\""));
        assertFalse(html.contains(".monarchDatepicker("));
        assertFalse(html.contains("<script"));
    }
}
