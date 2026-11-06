# Monarch Date Picker 1.1.3

Joget form element using the active Joget theme and native `cdatepicker` initializer.

The input now omits an empty `size` attribute, matching Joget's template and allowing the theme to determine its width. Time picker dependencies are loaded even when the form contains only Monarch fields. The native calendar, clear control, keyboard handling, and date range settings remain in use. UTC mode is preserved during rendering so submitted values retain their UTC conversion behavior.

Version 1.1.3 includes the plugin version in its CSS and JavaScript URLs. This prevents the browser from reusing assets cached for up to an hour by Joget's plugin resource servlet after an upgrade. Reload the form page after uploading the new JAR.

## Joget 9 bundle compatibility

Version 1.1.1 fixes the OSGi startup failure introduced in 1.1.0. Joget exports Servlet packages without an OSGi package version. The build now removes the inferred version attribute only from the two Servlet imports, while retaining mandatory resolution and the provided Servlet API dependency. This avoids an incompatible version constraint after Joget's Jakarta migration. No Servlet API classes are embedded in the plugin.

## Build and install

```sh
mvn test package
```

Upload `target/monarch-date-picker-1.1.3.jar` in Joget's **Manage Plugins**, replacing the existing Monarch Date Picker plugin. Reload the form page after updating the plugin.

## Public Holidays properties

In Form Builder, select **Monarch Date Picker → Public Holidays**:

| Property | Behavior |
| --- | --- |
| Enable Holiday API | Enable integration; existing forms remain unchanged until enabled. |
| Holiday API Base URL | Default `https://tanggalmerah.upset.dev`. A compatible base URL or `/api/holidays` endpoint is accepted, without query parameters or fragments. |
| Holiday Selection: Highlight | Mark national holidays red while allowing selection. |
| Holiday Selection: Disable | Mark holidays and prevent selection. Manually submitted dates are also validated on the server. |
| Include Collective Leave | Apply the chosen behavior to `leave` entries (cuti bersama), marked amber. Off by default. |

The integration uses [`GET /api/holidays?year=YYYY`](https://upset.dev/tanggalmerah), with the year taken from the displayed or entered date. National holidays use `type: holiday`; optional collective leave uses `type: leave`. Hover a holiday to see its date, name and type in a tooltip, including disabled dates. The tooltip closes when leaving the date, navigating the calendar or closing the popup. Time Only fields do not call the holiday API.

Data is cached per API URL and year for one hour. In Disable mode, dates awaiting data or unavailable from the API cannot be selected, and server validation rejects submission until data is available. Highlight mode continues to allow selection when the API is unavailable. Errors are shown beside the field; failed browser requests can retry after 30 seconds when the calendar is opened again. Existing values are retained so the user can correct them.

The browser and Joget server both need access to the configured API. A custom endpoint must allow browser CORS requests; use HTTPS when the form is served over HTTPS. Only the year is sent to the holidays endpoint.

## Verification

`mvn test` covers API parsing/caching, optional collective leave, manual input validation, API failures, UTC validation, and template output (including readonly fields and script escaping).

`src/test/browser/holiday-tests.js` runs deterministic browser checks after loading jQuery UI, Joget's native `cdatepicker`, the time picker addon, and `monarch-datepicker.js`. The page needs `#summary`, `#results`, and `#fixtures` containers. API calls are stubbed during these checks. The checks were run against the form JavaScript bundle from local Joget DX 9; the built JAR still needs to be installed and checked in the target form.

### Verify the bundle with a target Joget 9 runtime

The compatibility check uses Joget's actual migration classes and Felix configuration in a separate JVM and a fresh cache under `target/compatibility`. It does not install into the running Joget server. It checks the mandatory Servlet imports before and after migration, starts the bundle, verifies plugin service registration, exercises timezone/holiday validation/HTML escaping, and stops the bundle.

```sh
joget_lib="/path/to/tomcat/webapps/jw/WEB-INF/lib"
mkdir -p target/compatibility
javac -proc:none -cp "$joget_lib/*" -d target/compatibility src/test/compatibility/VerifyJogetBundle.java
java -Dwflow.home=target/compatibility/wflow -cp "target/compatibility:$joget_lib/*" VerifyJogetBundle target/monarch-date-picker-1.1.3.jar target/compatibility
```

Optionally append the original `target/monarch-date-picker-1.1.0.jar` as the third argument to reproduce the previous Jakarta Servlet resolution failure before checking the fixed bundle. This check passed using the installed Joget 9.1.2 libraries and Felix 7.0.5.

The Servlet import adjustment uses Bnd's documented [`-remove-attribute` import directive](https://bnd.bndtools.org/heads/import_package.html), scoped to `javax.servlet` and `javax.servlet.http`. Timezone handling delegates to Joget's native Date Picker, and holiday error escaping uses Joget's exported `StringUtil`, avoiding direct dependencies on Spring packages that Joget does not export through OSGi.
