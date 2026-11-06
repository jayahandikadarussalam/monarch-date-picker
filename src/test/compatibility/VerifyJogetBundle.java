import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import org.apache.tomcat.jakartaee.JogetPluginMigration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;

/** Run with the target Joget WEB-INF/lib jars, never against its running framework. */
public class VerifyJogetBundle {
    private static final String PLUGIN = "id.co.itasoft.monarch.plugin.MonarchDatePicker";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: VerifyJogetBundle candidate.jar output-directory [broken-1.1.0.jar]");
        }
        Path output = Path.of(args[1]).toAbsolutePath();
        Files.createDirectories(output);
        // Each run owns its own framework cache and does not deploy to app_plugins.
        Path work = Files.createTempDirectory(output, "joget-bundle-");
        Path candidate = Path.of(args[0]).toAbsolutePath();
        verifyManifest(candidate, "javax.servlet");
        Path migrated = migrate(candidate, work.resolve("candidate.jar"));
        verifyManifest(migrated, "jakarta.servlet");

        Properties runtime = new Properties();
        try (InputStream config = VerifyJogetBundle.class.getClassLoader().getResourceAsStream("felix_config.properties")) {
            if (config == null) throw new AssertionError("Joget felix_config.properties is missing");
            runtime.load(config);
        }
        Map<String, String> settings = new HashMap<>();
        for (String key : runtime.stringPropertyNames()) settings.put(key, runtime.getProperty(key));
        settings.put(Constants.FRAMEWORK_STORAGE, work.resolve("felix-cache").toString());
        settings.put("felix.service.urlhandlers", "false");
        Framework framework = new org.apache.felix.framework.FrameworkFactory().newFramework(settings);
        try {
            framework.start();
            if (args.length > 2) {
                Path baseline = migrate(Path.of(args[2]).toAbsolutePath(), work.resolve("baseline.jar"));
                Bundle broken = framework.getBundleContext().installBundle(baseline.toUri().toString());
                try {
                    broken.start();
                    throw new AssertionError("Expected the 1.1.0 Servlet version failure");
                } catch (BundleException expected) {
                    if (expected.getType() != BundleException.RESOLVE_ERROR
                            || !expected.getMessage().contains("jakarta.servlet")) throw expected;
                    System.out.println("PASS: reproduced 1.1.0 Jakarta Servlet resolution failure");
                } finally {
                    broken.uninstall();
                }
            }
            Bundle bundle = framework.getBundleContext().installBundle(migrated.toUri().toString());
            bundle.start();
            if (bundle.getState() != Bundle.ACTIVE) throw new AssertionError("Bundle is not ACTIVE");
            ServiceReference<?>[] services = bundle.getRegisteredServices();
            if (services == null || services.length != 1) throw new AssertionError("Plugin service was not registered");
            Object plugin = framework.getBundleContext().getService(services[0]);
            if (plugin == null || !plugin.getClass().getName().equals(PLUGIN)) {
                throw new AssertionError("Wrong plugin service");
            }
            Object name = plugin.getClass().getMethod("getName").invoke(plugin);
            Object version = plugin.getClass().getMethod("getVersion").invoke(plugin);
            if (!bundle.getVersion().toString().equals(version)) throw new AssertionError("Plugin and bundle versions differ");
            // Resolve the newly referenced runtime APIs as well as the activator.
            java.lang.reflect.Method timezone = plugin.getClass().getDeclaredMethod("getUserTZ");
            timezone.setAccessible(true);
            if (!(timezone.invoke(plugin) instanceof java.util.TimeZone)) throw new AssertionError("User timezone unavailable");
            plugin.getClass().getDeclaredMethod("renderTemplate",
                    org.joget.apps.form.model.FormData.class, Map.class);
            verifyHolidayValidation(bundle, plugin);
            framework.getBundleContext().ungetService(services[0]);
            System.out.println("PASS: migrated bundle ACTIVE; registered " + name + " " + version);
            bundle.stop();
            if (bundle.getRegisteredServices() != null) throw new AssertionError("Service remains registered after stop");
            System.out.println("PASS: plugin stopped and service unregistered");
        } finally {
            framework.stop();
            framework.waitForStop(10000);
        }
        System.out.println("Compatibility artifacts: " + work);
    }

    private static void verifyHolidayValidation(Bundle bundle, Object plugin) throws Exception {
        // Seed the API cache so this classloader check needs no network or application database.
        Class<?> calendarType = bundle.loadClass("id.co.itasoft.monarch.plugin.HolidayCalendar");
        java.lang.reflect.Field singleton = calendarType.getDeclaredField("INSTANCE");
        singleton.setAccessible(true);
        Object calendar = singleton.get(null);
        java.lang.reflect.Method parse = calendarType.getDeclaredMethod("parse", String.class, int.class);
        parse.setAccessible(true);
        Object holidays = parse.invoke(null,
                "{\"success\":true,\"data\":[{\"date\":\"2026-12-25\",\"type\":\"holiday\",\"name\":\"<b>Libur</b>\"}]}", 2026);
        Class<?> entryType = bundle.loadClass("id.co.itasoft.monarch.plugin.HolidayCalendar$Entry");
        java.lang.reflect.Constructor<?> entryConstructor = entryType.getDeclaredConstructor(holidays.getClass());
        entryConstructor.setAccessible(true);
        java.lang.reflect.Field cacheField = calendarType.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> cache = (Map<String, Object>) cacheField.get(calendar);
        cache.put("https://tanggalmerah.upset.dev/api/holidays?year=2026", entryConstructor.newInstance(holidays));
        org.joget.apps.form.lib.DatePicker picker = (org.joget.apps.form.lib.DatePicker) plugin;
        picker.setProperty("id", "date");
        picker.setProperty("format", "mm/dd/yy");
        picker.setProperty("holidayApiEnabled", "true");
        picker.setProperty("holidayAction", "disable");
        org.joget.apps.form.model.FormData data = new org.joget.apps.form.model.FormData();
        data.addRequestParameterValues("date", new String[]{"12/25/2026"});
        if (picker.selfValidate(data) || !data.getFormErrors().get("date").contains("&lt;b&gt;Libur&lt;/b&gt;")) {
            throw new AssertionError("Holiday validation or HTML escaping failed under OSGi");
        }
        System.out.println("PASS: holiday validation, JSON parsing and HTML escaping work under OSGi");
    }

    private static Path migrate(Path source, Path target) throws Exception {
        JogetPluginMigration migration = new JogetPluginMigration();
        migration.setSource(source.toFile());
        migration.setDestination(target.toFile());
        migration.execute();
        return target;
    }

    private static void verifyManifest(Path file, String servletPackage) throws Exception {
        try (JarFile jar = new JarFile(file.toFile())) {
            String imports = jar.getManifest().getMainAttributes().getValue("Import-Package");
            // Requiring bare imports is intentional: the target Joget exports these at version 0.0.0.
            for (String name : new String[]{servletPackage, servletPackage + ".http"}) {
                if (!("," + imports + ",").contains("," + name + ",")) {
                    throw new AssertionError("Expected a mandatory unversioned import for " + name + " in " + file);
                }
            }
            if (jar.stream().anyMatch(entry -> entry.getName().startsWith("javax/servlet/")
                    || entry.getName().startsWith("jakarta/servlet/")
                    || entry.getName().matches("dependency/.*servlet.*\\.jar"))) {
                throw new AssertionError("Servlet API must be provided by Joget, not embedded");
            }
            System.out.println("PASS: mandatory unversioned " + servletPackage + " imports; no embedded Servlet API");
        }
    }
}
