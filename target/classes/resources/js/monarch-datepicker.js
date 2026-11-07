(function ($) {
    "use strict";
    // Never replace Joget's cdatepicker: themes and other Date Picker fields use it too.
    if ($.fn.monarchDatepicker) {
        return;
    }
    var cache = Object.create(null);
    var cacheTtl = 60 * 60 * 1000;
    var retryDelay = 30000;
    var unavailable = "Data hari libur tidak tersedia. Silakan coba kembali.";
    var loading = "Memuat data hari libur…";

    var holidayCells = "#ui-datepicker-div td.monarch-public-holiday, #ui-datepicker-div td.monarch-collective-leave";
    var tooltipCell = null;
    var tooltip = null;
    var tooltipObserver = null;

    function hideHolidayTooltip() {
        if (tooltipObserver) {
            tooltipObserver.disconnect();
            tooltipObserver = null;
        }
        if (tooltipCell) {
            tooltipCell.attr("title", tooltipCell.data("monarchHolidayTitle"));
            var describedBy = (tooltipCell.attr("aria-describedby") || "").split(/\s+/).filter(function (id) {
                return id && id !== "monarch-holiday-tooltip";
            }).join(" ");
            if (describedBy) {
                tooltipCell.attr("aria-describedby", describedBy);
            } else {
                tooltipCell.removeAttr("aria-describedby");
            }
            tooltipCell = null;
        }
        if (tooltip) {
            tooltip.remove();
            tooltip = null;
        }
    }

    function showHolidayTooltip(cell) {
        if (tooltipCell && tooltipCell[0] === cell) {
            return;
        }
        hideHolidayTooltip();
        var target = $(cell);
        var text = target.attr("title");
        if (!text) {
            return;
        }
        tooltipCell = target;
        target.data("monarchHolidayTitle", text).removeAttr("title");
        target.attr("aria-describedby", ((target.attr("aria-describedby") || "") + " monarch-holiday-tooltip").trim());
        // API names are plain text, never HTML.
        tooltip = $("<div>", {id: "monarch-holiday-tooltip", role: "tooltip"}).text(text).appendTo(document.body);
        var rect = cell.getBoundingClientRect();
        var width = tooltip.outerWidth();
        var height = tooltip.outerHeight();
        var left = Math.max(8, Math.min(rect.left + (rect.width - width) / 2, window.innerWidth - width - 8));
        var top = rect.bottom + 8;
        if (top + height > window.innerHeight - 8) {
            top = Math.max(8, rect.top - height - 8);
        }
        tooltip.css({left: left, top: top});
        // jQuery UI shares and redraws its popup. Never leave a tooltip from an old month/field.
        tooltipObserver = new MutationObserver(hideHolidayTooltip);
        tooltipObserver.observe(target.closest("#ui-datepicker-div")[0], {
            childList: true, subtree: true, attributes: true, attributeFilter: ["style"]
        });
    }

    $(document).on("mouseenter.monarchHolidayTooltip focusin.monarchHolidayTooltip", holidayCells, function () {
        showHolidayTooltip(this);
    }).on("mouseleave.monarchHolidayTooltip focusout.monarchHolidayTooltip", holidayCells, hideHolidayTooltip)
        .on("mousedown.monarchHolidayTooltip touchstart.monarchHolidayTooltip", hideHolidayTooltip)
        .on("keydown.monarchHolidayTooltip", function (event) {
            if (event.key === "Escape" || event.keyCode === 27) {
                hideHolidayTooltip();
            }
        });
    $(window).on("resize.monarchHolidayTooltip scroll.monarchHolidayTooltip", hideHolidayTooltip);

    function endpoint(base) {
        var url = new URL((base || "").trim() || "https://tanggalmerah.upset.dev");
        if (!/^https?:$/.test(url.protocol) || url.username || url.password || url.search || url.hash) {
            throw new Error("Invalid Holiday API base URL");
        }
        var path = url.pathname.replace(/\/+$/, "");
        if (!/\/api\/holidays$/.test(path)) {
            path += "/api/holidays";
        }
        url.pathname = path;
        return url.href;
    }

    function parse(response, year) {
        if (!response || response.success !== true || !Array.isArray(response.data)) {
            throw new Error("Invalid Holiday API response");
        }
        var days = Object.create(null);
        response.data.forEach(function (holiday) {
            if (!holiday || typeof holiday.date !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(holiday.date)
                    || Number(holiday.date.slice(0, 4)) !== year
                    || (holiday.type !== "holiday" && holiday.type !== "leave")
                    || typeof holiday.name !== "string" || !holiday.name) {
                throw new Error("Invalid holiday data");
            }
            var date = $.datepicker.parseDate("yy-mm-dd", holiday.date);
            if ($.datepicker.formatDate("yy-mm-dd", date) !== holiday.date) {
                throw new Error("Invalid holiday date");
            }
            if (!days[holiday.date]) {
                days[holiday.date] = [];
            }
            days[holiday.date].push(holiday);
        });
        return days;
    }

    function request(base, year) {
        var key = base + "|" + year;
        var entry = cache[key];
        var now = Date.now();
        if (entry && now - entry.created < (entry.failed ? retryDelay : cacheTtl)) {
            return entry;
        }
        entry = {created: now, days: null, failed: false, promise: $.Deferred()};
        cache[key] = entry;
        $.ajax({url: base, data: {year: year}, dataType: "json", timeout: 7000,
            // Do not let a jQuery global setup turn an external request into a credentialed request.
            xhrFields: {withCredentials: false}
        }).done(function (response) {
            try {
                entry.days = parse(response, year);
                entry.promise.resolve();
            } catch (error) {
                entry.failed = true;
                entry.promise.reject();
            }
        }).fail(function () {
            entry.failed = true;
            entry.promise.reject();
        });
        return entry;
    }

    $.fn.monarchDatepicker = function (options) {
        return this.each(function () {
            var input = $(this);
            if (input.data("monarchDatepicker")) {
                return;
            }
            input.data("monarchDatepicker", true);
            var settings = $.extend({}, options);
            var enabled = settings.holidayApiEnabled === "true" && settings.datePickerType !== "timeOnly";
            var disableSaturday = settings.disableSaturday === "true" && settings.datePickerType !== "timeOnly";
            var disableSunday = settings.disableSunday === "true" && settings.datePickerType !== "timeOnly";
            var dateRestrictions = enabled || disableSaturday || disableSunday;
            var block = settings.holidayAction === "disable";
            var includeLeave = settings.holidayIncludeLeave === "true";
            var subscriptions = Object.create(null);
            var base;
            var status;
            var visibleYear;
            var beforeShowDay = settings.beforeShowDay;
            var monthChanged = settings.onChangeMonthYear;

            function weekendReason(date) {
                if (disableSaturday && date.getDay() === 6) {
                    return "Saturday cannot be selected."; // "Hari Sabtu tidak dapat dipilih.";
                }
                if (disableSunday && date.getDay() === 0) {
                    return "Sunday cannot be selected."; // "Hari Minggu tidak dapat dipilih.";
                }
                return "";
            }

            function message(text) {
                status.text(text);
            }

            function holidays(entry, date) {
                var day = $.datepicker.formatDate("yy-mm-dd", date);
                return (entry.days[day] || []).filter(function (holiday) {
                    return holiday.type === "holiday" || includeLeave;
                });
            }

            function load(year) {
                if (!base) {
                    return {failed: true, days: null};
                }
                var entry = request(base, year);
                if (subscriptions[year] !== entry) {
                    subscriptions[year] = entry;
                    entry.promise.always(function () {
                        // Refresh only this field's open popup; jQuery UI shares one popup across fields.
                        setTimeout(function () {
                            if (!$.contains(document, input[0])) {
                                return;
                            }
                            validate();
                            if ($.datepicker._curInst && $.datepicker._curInst.input[0] === input[0]) {
                                input.datepicker("refresh");
                            }
                        }, 0);
                    });
                }
                return entry;
            }

            function validate() {
                var error = "";
                var invalid = false;
                var current = null;
                if (input.val()) {
                    try {
                        current = input.datepicker("getDate");
                    } catch (ignored) {
                        // Joget validates the date format on submission.
                    }
                }
                if (current && weekendReason(current)) {
                    error = weekendReason(current);
                    invalid = true;
                } else if (current && enabled) {
                    var selectedEntry = load(current.getFullYear());
                    if (!selectedEntry.days) {
                        error = selectedEntry.failed ? unavailable : loading;
                    } else {
                        var selectedHolidays = holidays(selectedEntry, current);
                        if (block && selectedHolidays.length) {
                            error = "Tanggal libur tidak dapat dipilih: " + selectedHolidays.map(function (h) {
                                return h.name;
                            }).join("; ");
                        }
                    }
                    invalid = block && error !== "";
                }
                // Readonly fields do not participate in HTML constraint validation;
                // the plugin also enforces this rule in selfValidate on the server.
                input[0].setCustomValidity(invalid ? error : "");
                if (!error && enabled && visibleYear) {
                    var visibleEntry = load(visibleYear);
                    if (!visibleEntry.days) {
                        error = visibleEntry.failed ? unavailable : loading;
                    }
                }
                message(error);
            }

            if (dateRestrictions) {
                status = $("<span>", {"class": "monarch-holiday-status", "role": "status",
                    "aria-live": "polite", id: input[0].id + "_holiday_status"});
                input.parent().append(status);
                input.attr("aria-describedby", ((input.attr("aria-describedby") || "") + " " + status.attr("id")).trim());
                try {
                    base = endpoint(settings.holidayApiUrl);
                } catch (error) {
                    base = null;
                }
                settings.beforeShowDay = function (date) {
                    var inst = $.datepicker._getInst(input[0]);
                    visibleYear = inst.drawYear || date.getFullYear();
                    var original = beforeShowDay ? beforeShowDay.call(this, date).slice() : [true, "", ""];
                    var weekend = weekendReason(date);
                    if (weekend) {
                        original[0] = false;
                        original[1] = (original[1] + " monarch-disabled-weekend").trim();
                    }
                    if (!enabled) {
                        return original;
                    }
                    var entry = load(date.getFullYear());
                    if (!entry.days) {
                        message(entry.failed ? unavailable : loading);
                        return [original[0] && !block, original[1], original[2] || (weekend ? "" : (entry.failed ? unavailable : loading))];
                    }
                    var matches = holidays(entry, date);
                    if (!matches.length) {
                        return original;
                    }
                    var national = matches.some(function (h) { return h.type === "holiday"; });
                    var css = national ? "monarch-public-holiday" : "monarch-collective-leave";
                    var names = $.datepicker.formatDate("dd MM yy", date) + "\n" + matches.map(function (h) {
                        return (h.type === "holiday" ? "Hari Libur Nasional: " : "Cuti Bersama: ") + h.name;
                    }).join("\n");
                    return [original[0] && !block, (original[1] + " " + css).trim(),
                        [original[2], names].filter(Boolean).join("; ")];
                };
                settings.onChangeMonthYear = function (year, month, inst) {
                    visibleYear = year;
                    if (monthChanged) {
                        monthChanged.call(this, year, month, inst);
                    }
                    validate();
                };
            }

            // Use the same native initializer as Joget's Date Picker (clear, range, keyboard, time).
            input.cdatepicker(settings);
            if (dateRestrictions) {
                input.on("change.monarchHoliday", validate);
                input.on("click.monarchHoliday focus.monarchHoliday", function () {
                    var inst = $.datepicker._getInst(input[0]);
                    visibleYear = inst.drawYear || new Date().getFullYear();
                    validate();
                });
                validate();
            }
        });
    };
})(jQuery);
