package id.co.itasoft.monarch.plugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import org.joget.commons.util.LogUtil;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormBuilderPalette;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.PwaOfflineResources;
import org.joget.commons.util.DateUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.commons.util.SetupManager;
import org.joget.commons.util.StringUtil;
import org.joget.commons.util.TimeZoneUtil;
import org.joget.workflow.util.WorkflowUtil;
////import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;

public class MonarchDatePicker extends org.joget.apps.form.lib.DatePicker implements FormBuilderPaletteElement, PwaOfflineResources {

    public static final String UTC_DATEFORMAT = "yyyy-MM-dd HH:mm";
    public static final Set<String> SUPPORTED_LOCALE = new HashSet<>(Arrays.asList("ar", "cs", "de", "es", "fa", "fr", "he", "hr", "hu", "it", "ja", "ko", "ms", "nl", "pl", "pt-BR", "pt", "ro", "ru", "sl", "sv", "th", "tr", "uk", "vi", "zh-CN", "zh-TW"));

    private String format = null;

    @Override
    public String getName() {
        return "Shadow Date Picker";
    }

    @Override
    public String getVersion() {
        return "1.1.6";
    }

    @Override
    public String getDescription() {
        return "Shadow Date Picker Element";
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "monarchDatePicker.ftl";

        String format = getFormat();

        String displayFormat = getJavaDateFormat(format);
        String timeformat = getTimeFormat();
        if ("timeOnly".equalsIgnoreCase(getPropertyString("datePickerType"))) {
            displayFormat = timeformat;
        } else if ("dateTime".equalsIgnoreCase(getPropertyString("datePickerType"))
                || "utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
            displayFormat = displayFormat + " " + timeformat;
        }

        // set value
        String value = FormUtil.getElementPropertyValue(this, formData);

        if (FormUtil.isReadonly(this, formData)) {
            value = formattedDisplayValue(value, displayFormat, formData);
        } else {
            value = formattedValue(value, displayFormat, formData);
        }

        setProperty("firstday", AppUtil.getAppFirstDayOfWeek());

        dataModel.put("displayFormat", displayFormat.toUpperCase());
        if ("utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
            dataModel.put("userTimeZone", getUserTZ().getDisplayName());
        }
        dataModel.put("value", value);
        dataModel.put("clientDatePickerType", "utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))
                ? "dateTime" : getPropertyString("datePickerType"));

        dataModel.put("locale", getLocale());
        dataModel.put("pluginVersion", getVersion());

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        // The native Date Picker can reuse the timepicker dependencies loaded by this template.
        if (!FormUtil.isReadonly(this, formData) && WorkflowUtil.getHttpServletRequest() != null) {
            WorkflowUtil.getHttpServletRequest().setAttribute("org.joget.apps.form.lib.DatePicker_EDITABLE", true);
        }
        return html;
    }

    public FormRowSet formatData(FormData formData) {
        FormRowSet rowSet = null;

        // get value
        String id = getPropertyString(FormUtil.PROPERTY_ID);
        if (id != null) {
            String value = FormUtil.getElementPropertyValue(this, formData);
            if (!FormUtil.isReadonly(this, formData) && ((getPropertyString("dataFormat") != null && !getPropertyString("dataFormat").isEmpty()
                    && ("dateTime".equalsIgnoreCase(getPropertyString("datePickerType")) || getPropertyString("datePickerType").isEmpty()))
                    || "utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType")))) {
                String binderValue = formData.getLoadBinderDataProperty(this, id);
                if (value != null && !value.equals(binderValue)) {
                    try {
                        String displayFormat = getJavaDateFormat(getFormat());
                        if (!displayFormat.equals(getPropertyString("dataFormat"))) {
                            String timeformat = "";
                            if ("dateTime".equalsIgnoreCase(getPropertyString("datePickerType"))
                                    || "utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
                                timeformat = " " + getTimeFormat();
                            }

                            SimpleDateFormat data = null;
                            SimpleDateFormat display = new SimpleDateFormat(displayFormat + timeformat);
                            if ("utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
                                data = new SimpleDateFormat(UTC_DATEFORMAT);
                                if (FormUtil.PROPERTY_DATE_CREATED.equals(id) || FormUtil.PROPERTY_DATE_MODIFIED.equals(id)) {
                                    data.setTimeZone(TimeZone.getDefault());
                                } else {
                                    data.setTimeZone(TimeZone.getTimeZone("UTC"));
                                }
                                display.setTimeZone(getUserTZ());
                            } else {
                                data = new SimpleDateFormat(getPropertyString("dataFormat") + timeformat);
                            }
                            Date date = display.parse(value);
                            value = data.format(date);
                        }
                    } catch (Exception e) {}
                }
            }
            if (value != null) {
                // set value into Properties and FormRowSet object
                FormRow result = new FormRow();
                result.setProperty(id, value);
                rowSet = new FormRowSet();
                rowSet.add(result);
            }
        }

        return rowSet;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>" + ResourceBundleUtil.getMessage("org.joget.apps.form.lib.DatePicker.pluginLabel") + "</label><input type='text' />";
    }

    @Override
    public String getLabel() {
        return "Shadow Date Picker";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/form/monarchDatePicker.json", null, true, "messages/form/monarchDatePicker");
    }

    @Override
    public String getFormBuilderCategory() {
        return "<span><i class=\"fas fa-crown\"></i> Monarch </span>";
    }

    @Override
    public int getFormBuilderPosition() {
        return 500;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fas fa-calendar-alt\"></i>";
    }

    public String getFormat() {
        if (format == null) {
            format = getPropertyString("format");
            if (format.isEmpty()) {

                Locale locale = AppUtil.getAppLocale() != null
                        ? Locale.forLanguageTag(AppUtil.getAppLocale().replace("_", "-"))
                        : Locale.getDefault();

                // khusus Chinese locale
                if ("zh".equals(locale.getLanguage())) {
                    format = "yy-mm-dd";
                } else {
                    SetupManager setupManager = (SetupManager) AppUtil
                            .getApplicationContext()
                            .getBean("setupManager");

                    if ("true".equalsIgnoreCase(setupManager.getSettingValue("dateFormatFollowLocale"))) {
                        DateFormat dateInstance = DateFormat.getDateInstance(DateFormat.SHORT, locale);
                        if (dateInstance instanceof SimpleDateFormat) {
                            format = ((SimpleDateFormat) dateInstance).toPattern();
                            format = format.replaceAll("MM", "M");
                            format = format.replaceAll("M", "mm");
                            format = format.replaceAll("dd", "d");
                            format = format.replaceAll("d", "dd");
                            format = format.replaceAll("YYYY", "yy");
                        }
                    }
                }

                if (format == null || format.isEmpty()) {
                    format = "mm/dd/yy";
                }

                setProperty("format", format);
            }
        }
        return format;
    }

    protected TimeZone getUserTZ() {
        // Let the native Date Picker access Spring through Joget's classloader.
        return super.getUserTZ();
    }

    protected String getTimeFormat() {
        if ("timeOnly".equalsIgnoreCase(getPropertyString("datePickerType")) || "dateTime".equalsIgnoreCase(getPropertyString("datePickerType"))
                || "utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
            if ("true".equalsIgnoreCase(getPropertyString("format24hr"))) {
                return "HH:mm";
            } else {
                return "hh:mm a";
            }
        }
        return "";
    }

    public String getJavaDateFormat(String format) {
        if (format.contains("DD")) {
            format = format.replaceAll("DD", "EEEE");
        } else {
            format = format.replaceAll("D", "EEE");
        }

        if (format.contains("MM")) {
            format = format.replaceAll("MM", "MMMM");
        } else {
            format = format.replaceAll("M", "MMM");
        }

        if (format.contains("mm")) {
            format = format.replaceAll("mm", "MM");
        } else {
            format = format.replaceAll("m", "M");
        }

        if (format.contains("yy")) {
            format = format.replaceAll("yy", "yyyy");
        } else {
            format = format.replaceAll("y", "yy");
        }

        if (format.contains("tt") || format.contains("TT")) {
            format = format.replaceAll("tt","a");
            format = format.replaceAll("TT","a");
        }

        return format;
    }

    @Override
    public Boolean selfValidate(FormData formData) {
        Boolean valid = true;
        String id = FormUtil.getElementParameterName(this);
        String value = FormUtil.getElementPropertyValue(this, formData);

        if (value != null && !value.isEmpty()) {
            String displayFormat = getJavaDateFormat(getFormat());

            String timeformat = getTimeFormat();
            if ("timeOnly".equalsIgnoreCase(getPropertyString("datePickerType"))) {
                displayFormat = timeformat;
            } else if ("dateTime".equalsIgnoreCase(getPropertyString("datePickerType"))
                    || "utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
                displayFormat = displayFormat + " " + timeformat;
            }

            String formattedValue = formattedValue(value, displayFormat, formData);
            valid = DateUtil.validateDateFormat(formattedValue, displayFormat);

            if (!valid) {
                formData.addFormError(id, ResourceBundleUtil.getMessage("form.datepicker.error.invalidFormat"));
                return false;
            }

            if (!validateDateRestrictions(formattedValue, displayFormat, formData)) {
                valid = false;
            }

            Form form = null;
            if (!getPropertyString("startDateFieldId").isEmpty() ||
                    !getPropertyString("endDateFieldId").isEmpty()) {
                form = FormUtil.findRootForm(this);
            }

            String startDate = "";
            String endDate = "";

            if (!getPropertyString("startDateFieldId").isEmpty()) {
                Element e = FormUtil.findElement(getPropertyString("startDateFieldId"), form, formData);
                if (e != null) {
                    String compareValue = FormUtil.getElementPropertyValue(e, formData);
                    if (compareValue != null && !compareValue.isEmpty()) {
                        String formattedCompare = compareValue;
                        if (e instanceof org.joget.apps.form.lib.DatePicker || e instanceof MonarchDatePicker) {
                            formattedCompare = formatCompareValue(compareValue, displayFormat);
                        }
                        if (!DateUtil.compare(formattedCompare, formattedValue, displayFormat) && !formattedCompare.equals(value)) {
                            valid = false;
                            startDate = formattedCompare;
                        }
                    }
                }
            }

            if (!getPropertyString("endDateFieldId").isEmpty()) {
                Element e = FormUtil.findElement(getPropertyString("endDateFieldId"), form, formData);
                if (e != null) {
                    String compareValue = FormUtil.getElementPropertyValue(e, formData);
                    if (compareValue != null && !compareValue.isEmpty()) {
                        String formattedCompare = compareValue;
                        if (e instanceof org.joget.apps.form.lib.DatePicker || e instanceof MonarchDatePicker) {
                            formattedCompare = formatCompareValue(compareValue, displayFormat);
                        }
                        if (!DateUtil.compare(formattedValue, formattedCompare , displayFormat) && !formattedCompare.equals(value)) {
                            valid = false;
                            endDate = formattedCompare;
                        }
                    }
                }
            }

            String type = getPropertyString("currentDateAs");
            if (!type.isEmpty()) {
                String formattedCompare = TimeZoneUtil.convertToTimeZone(new Date(), null, displayFormat);
                String start, end;
                if ("minDate".equals(type)) {
                    start = formattedCompare;
                    end = formattedValue;
                } else {
                    start = formattedValue;
                    end = formattedCompare;
                }

                if (!DateUtil.compare(start, end , displayFormat) && !formattedCompare.equals(formattedValue)) {
                    valid = false;

                    if ("minDate".equals(type)) {
                        if (startDate.isEmpty() || !DateUtil.compare(formattedCompare, startDate, displayFormat)) {
                            startDate = formattedCompare;
                        }
                    } else {
                        if (endDate.isEmpty() || !DateUtil.compare(endDate, formattedCompare, displayFormat)) {
                            endDate = formattedCompare;
                        }
                    }
                }
            }

            if (!startDate.isEmpty()) {
                formData.addFormError(id, ResourceBundleUtil.getMessage("form.datepicker.error.minDate", new String[]{startDate}));
            }

            if (!endDate.isEmpty()) {
                formData.addFormError(id, ResourceBundleUtil.getMessage("form.datepicker.error.maxDate", new String[]{endDate}));
            }
        }

        return valid;
    }

    protected HolidayCalendar getHolidayCalendar() {
        return HolidayCalendar.INSTANCE;
    }

    private boolean validateDateRestrictions(String value, String displayFormat, FormData formData) {
        boolean blockHolidays = "true".equalsIgnoreCase(getPropertyString("holidayApiEnabled"))
                && "disable".equals(getPropertyString("holidayAction"));
        boolean disableSaturday = "true".equalsIgnoreCase(getPropertyString("disableSaturday"));
        boolean disableSunday = "true".equalsIgnoreCase(getPropertyString("disableSunday"));
        if ((!blockHolidays && !disableSaturday && !disableSunday)
                || "timeOnly".equalsIgnoreCase(getPropertyString("datePickerType"))
                || FormUtil.isReadonly(this, formData)) {
            return true;
        }
        String id = FormUtil.getElementParameterName(this);
        try {
            SimpleDateFormat parser = new SimpleDateFormat(displayFormat);
            parser.setLenient(false);
            parser.setTimeZone(getUserTZ());
            LocalDate date = parser.parse(value).toInstant().atZone(getUserTZ().toZoneId()).toLocalDate();
            if ((disableSaturday && date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY)
                    || (disableSunday && date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY)) {
                formData.addFormError(id, date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                        ? "Saturday cannot be selected." : "Sunday cannot be selected.");
                return false;
            }
            if (!blockHolidays) {
                return true;
            }
            String holiday = getHolidayCalendar().getHoliday(getPropertyString("holidayApiUrl"), date,
                    "true".equalsIgnoreCase(getPropertyString("holidayIncludeLeave")));
            if (holiday != null) {
                formData.addFormError(id, "Tanggal libur tidak dapat dipilih: " + StringUtil.escapeString(holiday, StringUtil.TYPE_HTML, null));
                return false;
            }
        } catch (Exception e) {
            LogUtil.warn(getClassName(), "Holiday API validation failed: " + e.getClass().getSimpleName());
            formData.addFormError(id, "Data hari libur tidak tersedia. Silakan coba kembali.");
            return false;
        }
        return true;
    }

    private String formatCompareValue(String value, String displayFormat) {
        String dataFormat = getPropertyString("dataFormat");

        String timeformat = getTimeFormat();
        if ("timeOnly".equalsIgnoreCase(getPropertyString("datePickerType"))) {
            dataFormat = timeformat;
        } else if ("dateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
            dataFormat = dataFormat + " " + timeformat;
        }
        if ("utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
            dataFormat = UTC_DATEFORMAT;
        }

        String tempValue = value.replaceAll("[0-9]", "x");
        String tempFormat = dataFormat.replaceAll("[a-zA-Z]", "x");

        if (!displayFormat.equals(dataFormat) && tempValue.equals(tempFormat)) {
            try {
                SimpleDateFormat data = new SimpleDateFormat(dataFormat);
                SimpleDateFormat display = new SimpleDateFormat(displayFormat);
                if ("utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
                    String id = getPropertyString(FormUtil.PROPERTY_ID);
                    if (FormUtil.PROPERTY_DATE_CREATED.equals(id) || FormUtil.PROPERTY_DATE_MODIFIED.equals(id)) {
                        data.setTimeZone(TimeZone.getDefault());
                    } else {
                        data.setTimeZone(TimeZone.getTimeZone("UTC"));
                    }
                    display.setTimeZone(getUserTZ());
                }
                Date date = data.parse(value);
                value = display.format(date);
            } catch (Exception e) {}
        }
        return value;
    }

    private String formattedDisplayValue(String value, String displayFormat, FormData formData) {
        if ((getPropertyString("dataFormat") != null && !getPropertyString("dataFormat").isEmpty())
                || "utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
            try {
                String dataFormat = getPropertyString("dataFormat");
                String timeformat = getTimeFormat();
                if ("timeOnly".equalsIgnoreCase(getPropertyString("datePickerType"))) {
                    dataFormat = timeformat;
                } else if ("dateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
                    dataFormat = dataFormat + " " + timeformat;
                }
                if ("utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
                    dataFormat = UTC_DATEFORMAT;
                }

                if (!displayFormat.equals(dataFormat) || value.length() > displayFormat.length() + 5) { //or value has time value but format doesn't
                    SimpleDateFormat data = new SimpleDateFormat(dataFormat);
                    SimpleDateFormat display = new SimpleDateFormat(displayFormat);
                    if ("utcdateTime".equalsIgnoreCase(getPropertyString("datePickerType"))) {
                        String id = getPropertyString(FormUtil.PROPERTY_ID);
                        if (FormUtil.PROPERTY_DATE_CREATED.equals(id) || FormUtil.PROPERTY_DATE_MODIFIED.equals(id)) {
                            data.setTimeZone(TimeZone.getDefault());
                        } else {
                            data.setTimeZone(TimeZone.getTimeZone("UTC"));
                        }
                        display.setTimeZone(getUserTZ());
                    }
                    Date date = data.parse(value);
                    value = display.format(date);
                }
            } catch (Exception e) {
            }
        }
        return value;
    }

    private String formattedValue(String value, String displayFormat, FormData formData) {
        if (!FormUtil.isFormSubmitted(this, formData) && !FormUtil.isFormDataImported(this, formData)) {
            value = formattedDisplayValue(value, displayFormat, formData);
        }
        return value;
    }

    @Override
    public Set<String> getOfflineStaticResources() {
        Set<String> urls = new HashSet<String>();
        String contextPath = AppUtil.getRequestContextPath();
        urls.add(contextPath + "/plugin/" + getClassName() + "/js/monarch-datepicker.js");
        urls.add(contextPath + "/plugin/" + getClassName() + "/css/datePicker.css");
        urls.add(contextPath + "/plugin/org.joget.apps.form.lib.DatePicker/js/jquery.placeholder.min.js");
        urls.add(contextPath + "/plugin/org.joget.apps.form.lib.DatePicker/css/datePicker.css");
        urls.add(contextPath + "/plugin/org.joget.apps.form.lib.DatePicker/css/jquery-ui-timepicker-addon.css");
        urls.add(contextPath + "/plugin/org.joget.apps.form.lib.DatePicker/js/jquery-ui-timepicker-addon.js");

        return urls;
    }

    public static String getLocale() {
        String locale = AppUtil.getAppLocale();
        if (locale != null && !locale.isEmpty()) {
            locale = locale.replace("_", "-");
            if (SUPPORTED_LOCALE.contains(locale)) {
                return locale;
            } else {
                locale = locale.substring(0, 2);
                if (SUPPORTED_LOCALE.contains(locale)) {
                    return locale;
                }
            }
        }
        return "";
    }
}
