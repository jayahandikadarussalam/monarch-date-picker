/* Run in a browser after jQuery UI, native Joget cdatepicker and monarch-datepicker.js. */
(async function () {
    "use strict";
    var results = document.getElementById("results");
    var failures = 0;
    var requests = [];
    var nativePicker = $.fn.cdatepicker;
    var realAjax = $.ajax;
    var pending = [];
    function assert(condition, text) {
        var item = document.createElement("li");
        item.textContent = (condition ? "PASS: " : "FAIL: ") + text;
        item.style.color = condition ? "#167543" : "#b42318";
        results.appendChild(item);
        if (!condition) failures++;
    }
    function wait() { return new Promise(function (resolve) { setTimeout(resolve, 50); }); }
    function make(id, options) {
        var cell = $("<div>", {"class": "form-cell monarch-date-picker"}).appendTo("#fixtures");
        $("<label>", {"class": "label", text: id, "for": id}).appendTo(cell);
        var input = $("<input>", {id: id, type: "text", placeholder: "MM/DD/YYYY"}).appendTo(cell);
        input.monarchDatepicker($.extend({dateFormat: "mm/dd/yy", showOn: "button", buttonImageOnly: true,
            buttonImage: "http://localhost:8080/jw/css/images/calendar.png", changeMonth: true, changeYear: true,
            yearRange: "2025:2027", holidayApiEnabled: "true", holidayAction: "highlight"}, options));
        return input;
    }
    function response(year) {
        return {success: true, data: [
            {date: year + "-12-24", name: "Cuti Natal", type: "leave"},
            {date: year + "-12-25", name: "Natal <b>uji</b>", type: "holiday"}
        ]};
    }
    $.ajax = function (options) {
        var deferred = $.Deferred();
        requests.push(options);
        pending.push(function () {
            if (options.url.indexOf("offline") >= 0) deferred.reject();
            else if (options.url.indexOf("malformed") >= 0) deferred.resolve({success: true, data: {}});
            else deferred.resolve(response(options.data.year));
        });
        return deferred;
    };
    function resolve() {
        var tasks = pending.splice(0);
        tasks.forEach(function (task) { task(); });
    }
    try {
        var disabled = make("API disabled", {holidayApiEnabled: ""});
        var time = make("Time only", {datePickerType: "timeOnly", timeFormat: "HH:mm", disableSaturday: "true", disableSunday: "true"});
        assert(requests.length === 0, "Disabled integration and Time Only make no API calls");
        var marked = make("Highlight holidays");
        marked.val("12/25/2026").trigger("change");
        var blocked = make("Disable holidays", {holidayAction: "disable", holidayIncludeLeave: "true"});
        blocked.val("12/25/2026").trigger("change");
        var day = blocked.datepicker("option", "beforeShowDay");
        assert(!day(new Date(2026, 11, 25))[0], "Disable mode blocks dates while loading");
        assert(requests.length === 1, "Fields share one request for the same API and year");
        assert(requests[0].url === "https://tanggalmerah.upset.dev/api/holidays", "Default API URL has exactly one slash before api/holidays");
        resolve(); await wait();
        assert(day(new Date(2026, 11, 25))[0] === false, "National holiday cannot be selected");
        assert(day(new Date(2026, 11, 24))[0] === false, "Collective leave cannot be selected when included");
        assert(day(new Date(2026, 11, 26))[0] === true, "Ordinary working date remains selectable");
        assert(blocked[0].validationMessage.indexOf("Natal") >= 0, "Manual holiday input has a validation error");
        blocked.val("12/26/2026").trigger("change");
        assert(blocked[0].validationMessage === "", "Manual working date clears the error");
        var markDay = marked.datepicker("option", "beforeShowDay");
        assert(markDay(new Date(2026, 11, 25))[0] && /monarch-public-holiday/.test(markDay(new Date(2026, 11, 25))[1]), "Highlight mode marks holidays and allows selection");
        assert(markDay(new Date(2026, 11, 24))[1] === "", "Collective leave excluded by default");
        day(new Date(2027, 0, 1));
        assert(requests.some(function (r) { return r.data.year === 2027; }), "Navigation loads the new year");
        resolve(); await wait();
        var offline = make("API unavailable", {holidayApiUrl: "https://offline.example.org", holidayAction: "disable"});
        offline.val("12/26/2026").trigger("change");
        resolve(); await wait();
        assert(offline[0].validationMessage.indexOf("tidak tersedia") >= 0, "API failure displays an error and prevents submission");
        assert(!offline.datepicker("option", "beforeShowDay")(new Date(2026, 11, 26))[0], "API failure does not silently allow blocked dates");
        var malformed = make("Malformed API", {holidayApiUrl: "https://malformed.example.org", holidayAction: "disable"});
        malformed.val("12/26/2026").trigger("change");
        resolve(); await wait();
        assert(malformed[0].validationMessage.indexOf("tidak tersedia") >= 0, "Malformed data is treated as unavailable");
        marked.val("12/25/2026").trigger("change");
        await wait();
        marked.parent().find("a.close-icon").trigger("click");
        assert(marked.val() === "", "Native clear control clears the field");
        assert($.fn.cdatepicker === nativePicker, "Monarch preserves Joget's native initializer");
        assert(!marked.is("[size]"), "No empty size attribute overrides the Joget theme width");
        var before = requests.length;
        marked.monarchDatepicker({holidayApiEnabled: "true"});
        assert(marked.parent().find("a.close-icon").length === 1 && requests.length === before, "Repeated initialization creates no duplicate controls or requests");
        var beforeWeekend = requests.length;
        var saturdayOnly = make("Disable Saturday only", {holidayApiEnabled: "", disableSaturday: "true"});
        saturdayOnly.val("12/26/2026").trigger("change");
        var saturdayDay = saturdayOnly.datepicker("option", "beforeShowDay");
        assert(!saturdayDay(new Date(2026, 11, 26))[0] && saturdayDay(new Date(2026, 11, 27))[0], "Saturday can be disabled while Sunday remains selectable");
        assert(saturdayOnly[0].validationMessage.indexOf("Saturday") >= 0, "Manual Saturday input is rejected without the Holiday API");
        saturdayOnly.val("12/28/2026").trigger("change");
        assert(saturdayOnly[0].validationMessage === "", "Changing to a weekday clears the weekend error");
        var sundayOnly = make("Disable Sunday only", {holidayApiEnabled: "", disableSunday: "true"});
        sundayOnly.val("12/27/2026").trigger("change");
        var sundayDay = sundayOnly.datepicker("option", "beforeShowDay");
        assert(!sundayDay(new Date(2026, 11, 27))[0] && sundayDay(new Date(2026, 11, 26))[0], "Sunday can be disabled while Saturday remains selectable");
        assert(sundayOnly[0].validationMessage.indexOf("Sunday") >= 0 && requests.length === beforeWeekend, "Weekend rules make no API calls");
        var both = make("Disable weekends with highlight", {disableSaturday: "true", disableSunday: "true"});
        var bothDay = both.datepicker("option", "beforeShowDay");
        assert(!bothDay(new Date(2026, 11, 26))[0] && !bothDay(new Date(2026, 11, 27))[0]
            && bothDay(new Date(2026, 11, 25))[0], "Both weekends stay disabled when holidays are highlight-only");
        saturdayOnly.datepicker("setDate", new Date(2026, 11, 1));
        saturdayOnly.datepicker("show");
        resolve(); await wait();
        var weekendCell = $("#ui-datepicker-div td.monarch-disabled-weekend").first();
        weekendCell.trigger("mouseenter");
        assert(!$("#monarch-holiday-tooltip").length && !weekendCell.attr("title"), "Disabled weekend has neither a custom tooltip nor a native title tooltip");
        saturdayOnly.datepicker("hide");
        await wait();
        // Keep a real calendar visible for visual inspection.
        blocked.datepicker("setDate", new Date(2026, 11, 1));
        blocked.datepicker("show");
        resolve(); await wait();
        var holidayCell = $("#ui-datepicker-div td.monarch-public-holiday").first();
        assert(holidayCell.hasClass("ui-state-disabled") && holidayCell.css("pointer-events") === "auto", "Disabled holiday cells allow hover without enabling selection");
        holidayCell.trigger("mouseenter");
        var tooltip = $("#monarch-holiday-tooltip");
        assert(tooltip.length === 1 && tooltip.text().indexOf("25 December 2026") >= 0
            && tooltip.text().indexOf("Hari Libur Nasional: Natal") >= 0, "Hover shows the date, holiday name and national holiday type");
        assert(tooltip.find("b").length === 0 && tooltip.text().indexOf("<b>uji</b>") >= 0, "API holiday names are displayed as safe plain text");
        holidayCell.trigger("mouseleave");
        assert(!$("#monarch-holiday-tooltip").length && holidayCell.attr("title").indexOf("Natal") >= 0, "Mouse leave removes the tooltip and restores the native title");
        var leaveCell = $("#ui-datepicker-div td.monarch-collective-leave").first();
        leaveCell.trigger("mouseenter");
        assert($("#monarch-holiday-tooltip").text().indexOf("Cuti Bersama: Cuti Natal") >= 0, "Collective leave hover shows its type and name");
        blocked.datepicker("setDate", new Date(2027, 0, 1));
        resolve(); await wait();
        assert(!$("#monarch-holiday-tooltip").length, "Calendar redraw removes the previous holiday tooltip");
        blocked.datepicker("setDate", new Date(2026, 11, 1));
        holidayCell = $("#ui-datepicker-div td.monarch-public-holiday").first();
        holidayCell.trigger("mouseenter");
        $(document).trigger($.Event("keydown", {key: "Escape", keyCode: 27}));
        assert(!$("#monarch-holiday-tooltip").length, "Escape dismisses the tooltip");
        holidayCell.trigger("mouseenter");
        blocked.datepicker("hide");
        await wait();
        assert(!$("#monarch-holiday-tooltip").length, "Closing the calendar removes the holiday tooltip");
        blocked.datepicker("show");
        document.getElementById("summary").textContent = failures ? failures + " BROWSER TESTS FAILED" : "ALL " + results.children.length + " BROWSER TESTS PASSED";
        document.title = document.getElementById("summary").textContent;
    } catch (error) {
        document.getElementById("summary").textContent = "BROWSER TEST ERROR: " + error.stack;
        document.title = "BROWSER TEST ERROR";
    } finally {
        $.ajax = realAjax;
    }
})();
