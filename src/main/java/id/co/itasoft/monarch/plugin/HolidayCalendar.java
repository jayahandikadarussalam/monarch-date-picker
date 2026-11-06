package id.co.itasoft.monarch.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Tanggal Merah API client for submitted dates, including manual input. */
class HolidayCalendar {
    static final HolidayCalendar INSTANCE = new HolidayCalendar();
    static final String DEFAULT_URL = "https://tanggalmerah.upset.dev";
    private static final long CACHE_TTL = 60 * 60 * 1000L;
    private final Map<String, Entry> cache = new LinkedHashMap<String, Entry>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
            return size() > 100;
        }
    };

    String getHoliday(String baseUrl, LocalDate date, boolean includeLeave) throws Exception {
        String url = endpoint(baseUrl, date.getYear());
        Entry entry;
        synchronized (cache) {
            entry = cache.get(url);
        }
        if (entry == null || System.currentTimeMillis() - entry.loadedAt >= CACHE_TTL) {
            entry = new Entry(parse(fetch(url), date.getYear()));
            synchronized (cache) {
                cache.put(url, entry);
            }
        }
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < entry.holidays.length(); i++) {
            JSONObject holiday = entry.holidays.getJSONObject(i);
            if (date.toString().equals(holiday.getString("date"))
                    && ("holiday".equals(holiday.getString("type")) || includeLeave)) {
                if (names.length() > 0) {
                    names.append("; ");
                }
                names.append(holiday.getString("name"));
            }
        }
        return names.length() == 0 ? null : names.toString();
    }

    static String endpoint(String baseUrl, int year) {
        String base = baseUrl == null || baseUrl.trim().isEmpty() ? DEFAULT_URL : baseUrl.trim();
        URI uri = URI.create(base);
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Holiday API URL must be an HTTP(S) base URL without query or fragment");
        }
        base = base.replaceAll("/+$", "");
        if (!base.endsWith("/api/holidays")) {
            base += "/api/holidays";
        }
        return base + "?year=" + year;
    }

    protected String fetch(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setInstanceFollowRedirects(false);
        try {
            if (connection.getResponseCode() != 200) {
                throw new IOException("Holiday API HTTP " + connection.getResponseCode());
            }
            try (InputStream input = connection.getInputStream()) {
                byte[] body = input.readNBytes(1024 * 1024 + 1);
                if (body.length > 1024 * 1024) {
                    throw new IOException("Holiday API response too large");
                }
                return new String(body, StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    static JSONArray parse(String body, int year) throws Exception {
        JSONObject response = new JSONObject(body);
        if (!response.getBoolean("success")) {
            throw new IOException("Holiday API returned an error");
        }
        JSONArray holidays = response.getJSONArray("data");
        for (int i = 0; i < holidays.length(); i++) {
            JSONObject holiday = holidays.getJSONObject(i);
            String date = holiday.getString("date");
            String type = holiday.getString("type");
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}") || LocalDate.parse(date).getYear() != year
                    || !("holiday".equals(type) || "leave".equals(type)) || holiday.getString("name").isEmpty()) {
                throw new IOException("Invalid holiday data");
            }
        }
        return holidays;
    }

    private static class Entry {
        final JSONArray holidays;
        final long loadedAt = System.currentTimeMillis();

        Entry(JSONArray holidays) {
            this.holidays = holidays;
        }
    }
}
