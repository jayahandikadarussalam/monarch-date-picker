<div class="form-cell monarch-date-picker" ${elementMetaData!}>
<#if element.properties.readonly! != 'true'>
    <#if !(request.getAttribute("id.co.itasoft.monarch.plugin.MonarchDatePicker_EDITABLE")??)>
        <link rel="stylesheet" href="${request.contextPath}/plugin/id.co.itasoft.monarch.plugin.MonarchDatePicker/css/datePicker.css?v=${pluginVersion?html}" />
        <script type="text/javascript" src="${request.contextPath}/plugin/id.co.itasoft.monarch.plugin.MonarchDatePicker/js/monarch-datepicker.js?v=${pluginVersion?html}"></script>
    </#if>
    <#if !(request.getAttribute("org.joget.apps.form.lib.DatePicker_EDITABLE")??) && !(request.getAttribute("id.co.itasoft.monarch.plugin.MonarchDatePicker_EDITABLE")??)>
        <#if locale! != ''>
            <script type="text/javascript" src="${request.contextPath}/js/jquery/ui/i18n/jquery.ui.datepicker-${locale}.js"></script>
        </#if>
        <script type="text/javascript" src="${request.contextPath}/plugin/org.joget.apps.form.lib.DatePicker/js/jquery.placeholder.min.js"></script>
        <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.apps.form.lib.DatePicker/css/jquery-ui-timepicker-addon.css" />
        <script type="text/javascript" src="${request.contextPath}/plugin/org.joget.apps.form.lib.DatePicker/js/jquery-ui-timepicker-addon.js"></script>
    </#if>
<script type="text/javascript">
    $(document).ready(function() {
$(document.getElementById("${elementParamName?js_string}_${(element.properties.elementUniqueKey!'')?js_string}")).monarchDatepicker({
showOn: "button",
buttonImage: "${request.contextPath}/css/images/calendar.png",
buttonImageOnly: true,
changeMonth: true,
changeYear: true,
timeInput: true,
holidayApiEnabled: "${(element.properties.holidayApiEnabled!'')?js_string}",
holidayApiUrl: "${(element.properties.holidayApiUrl!'')?js_string}",
holidayAction: "${(element.properties.holidayAction!'highlight')?js_string}",
holidayIncludeLeave: "${(element.properties.holidayIncludeLeave!'')?js_string}"
<#if element.properties.format24hr! != 'true'>
,timeFormat: "hh:mm tt"
</#if>
<#if element.properties.format! != ''>
,dateFormat: "${element.properties.format?js_string}"
</#if>
<#if element.properties.yearRange! != ''>
,yearRange: "${element.properties.yearRange?js_string}"
</#if>
<#if element.properties.startDateFieldId! != ''>
,startDateFieldId: "${element.properties.startDateFieldId?js_string}"
</#if>
<#if element.properties.endDateFieldId! != ''>
,endDateFieldId: "${element.properties.endDateFieldId?js_string}"
</#if>
<#if element.properties.currentDateAs! != ''>
,currentDateAs: "${element.properties.currentDateAs?js_string}"
</#if>
<#if element.properties.datePickerType! != ''>
,datePickerType: "${clientDatePickerType?js_string}"
</#if>
<#if element.properties.firstday! != ''>
,firstDay: "${element.properties.firstday?js_string}"
</#if>
});
    });
</script>
</#if>
    <label field-tooltip="${elementParamName!}" class="label" for="${elementParamName!}_${element.properties.elementUniqueKey!}">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if element.properties.showUserTimeZone! == 'true' && !(element.properties.readonly! == 'true' && element.properties.readonlyLabel! == 'true') ><br/><span>(${userTimeZone!?html})</span></#if><#if error??> <span class="form-error-message">${error}</span></#if></label>
    <#if (element.properties.readonly! == 'true' && element.properties.readonlyLabel! == 'true') >
        <span>${value!?html} <#if element.properties.showUserTimeZone! == 'true' >(${userTimeZone!?html})</#if></span>
        <input id="${elementParamName!}" name="${elementParamName!}" type="hidden" value="${value!?html}" />
    <#else>
        <input id="${elementParamName!}_${element.properties.elementUniqueKey!}" name="${elementParamName!}" type="text" <#if element.properties.size?has_content>size="${element.properties.size?html}"</#if> value="${value!?html}" class="${elementParamName!} <#if error??>form-error-cell</#if>" <#if (element.properties.allowManual! != 'true' || element.properties.readonly! == 'true')>readonly</#if> placeholder="<#if (element.properties.placeholder! != '')>${element.properties.placeholder!?html}<#else>${displayFormat!?html}</#if>" />
    </#if>
</div>