///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.2

// Fetch all current CelesTrak GP element sets (union of all GROUPs + SPECIAL sets) and the SATCAT
// into data/raw/, one download per source, honoring CelesTrak's usage policy:
//   - only https://celestrak.org (avoids .com 301 redirect storm)
//   - one download per 2-hour update window (reuses local cache within the TTL)
//   - stop hard on non-200 responses instead of hammering
//   - gentle 1.5s spacing between requests
//
// Usage:
//   jbang fetch.java            # fetch anything older than the TTL (default 120 min)
//   jbang fetch.java --force    # ignore cache, refetch all sources
//   jbang fetch.java --ttl=720  # use a 12h cache TTL

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class fetch {

    static final String BASE = "https://celestrak.org/NORAD/elements/gp.php";

    record Source(String name, String url, String kind) {}

    static List<Source> sources() {
        String[] groups = {
                "last-30-days", "stations", "visual", "active", "analyst",
                "fengyun-1c-debris", "iridium-33-debris", "cosmos-2251-debris",
                "weather", "resource", "sar", "sarsat", "dmc", "tdrss", "argos", "planet", "spire",
                "geo", "intelsat", "ses", "eutelsat", "telesat", "starlink", "oneweb", "qianfan",
                "hulianwang", "kuiper", "iridium-NEXT", "orbcomm", "globalstar", "amateur",
                "satnogs", "x-comm", "other-comm",
                "gnss", "gps-ops", "glo-ops", "galileo", "beidou", "sbas",
                "science", "geodetic", "engineering", "education",
                "military", "radar", "cubesat"
        };
        String[] specials = {"gpz", "gpz-plus", "decaying"};

        List<Source> out = new ArrayList<>();
        for (String g : groups) {
            out.add(new Source("gp_" + g, BASE + "?GROUP=" + g + "&FORMAT=csv", "GP"));
        }
        for (String s : specials) {
            out.add(new Source("gp_" + s, BASE + "?SPECIAL=" + s + "&FORMAT=csv", "GP"));
        }
        out.add(new Source("satcat", "https://celestrak.org/pub/satcat.csv", "SATCAT"));
        return out;
    }

    public static void main(String[] args) throws Exception {
        boolean force = Arrays.asList(args).contains("--force");
        long refreshMin = 120;
        for (String a : args) {
            if (a.startsWith("--ttl=")) refreshMin = Long.parseLong(a.substring(6));
        }

        Path raw = Path.of("data", "raw");
        Files.createDirectories(raw);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        List<Map<String, Object>> manifest = new ArrayList<>();
        ObjectMapper om = new ObjectMapper();
        Path mf = raw.resolve("manifest.json");
        int fetched = 0;
        int cached = 0;

        for (Source s : sources()) {
            Path out = raw.resolve(s.name() + ".csv");
            boolean fresh = !force && Files.exists(out)
                    && Files.getLastModifiedTime(out).toMillis()
                       > System.currentTimeMillis() - refreshMin * 60_000L;

            Map<String, Object> e = new LinkedHashMap<>();
            e.put("name", s.name());
            e.put("url", s.url());
            e.put("kind", s.kind());

            if (fresh) {
                cached++;
                System.out.println("CACHED " + s.name());
                e.put("status", "cached");
                e.put("fetchedAtUtc", Instant.ofEpochMilli(Files.getLastModifiedTime(out).toMillis()).toString());
                e.put("bytes", Files.size(out));
                e.put("sha256", sha256(out));
                e.put("rows", dataRows(out));
                manifest.add(e);
                continue;
            }

            System.out.println("FETCH  " + s.name() + "  <-  " + s.url());
            HttpRequest req = HttpRequest.newBuilder(URI.create(s.url()))
                    .timeout(Duration.ofSeconds(90))
                    .header("User-Agent", "celestrak-catalog-jbang/1.0 (one download per update)")
                    .GET()
                    .build();

            HttpResponse<byte[]> resp;
            try {
                resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            } catch (Exception ex) {
                System.err.println("ERROR   " + s.name() + " network failure: " + ex);
                writeManifest(om, mf, manifest);
                System.exit(3);
                return;
            }

            if (resp.statusCode() != 200) {
                String body = new String(resp.body(), StandardCharsets.UTF_8);
                // CelesTrak enforces one download per 2-hour update window; a 403 with this
                // message just means the data hasn't been re-published yet. Defer that source
                // and continue with the rest rather than hammering (or stopping everything).
                if (resp.statusCode() == 403 && body.contains("has not updated since your last")) {
                    System.out.println("DEFER   " + s.name() + " (data not yet updated; one download per update window)");
                    e.put("status", "deferred-not-updated");
                    e.put("httpStatus", resp.statusCode());
                    e.put("fetchedAtUtc", Instant.now().toString());
                    manifest.add(e);
                    continue;
                }
                System.err.println("NON-200 " + s.name() + " HTTP " + resp.statusCode());
                System.err.println(body.length() > 600 ? body.substring(0, 600) : body);
                System.err.println("Stopping (CelesTrak policy: stop on unexpected responses, do not hammer).");
                writeManifest(om, mf, manifest);
                System.exit(2);
            }

            byte[] body = resp.body();
            String text = new String(body, StandardCharsets.UTF_8);
            if (!text.contains("OBJECT_NAME")) {
                System.err.println("UNEXPECTED BODY for " + s.name() + " (missing OBJECT_NAME header). Stopping.");
                System.err.println(text.length() > 600 ? text.substring(0, 600) : text);
                writeManifest(om, mf, manifest);
                System.exit(2);
            }

            Files.write(out, body);
            e.put("status", "fetched");
            e.put("fetchedAtUtc", Instant.now().toString());
            e.put("httpStatus", resp.statusCode());
            e.put("bytes", body.length);
            e.put("sha256", sha256(out));
            e.put("rows", dataRows(out));
            manifest.add(e);
            fetched++;

            Thread.sleep(1500);
        }

        writeManifest(om, mf, manifest);

        System.out.println();
        System.out.println("Fetched " + fetched + " new, reused " + cached + " cached, of " + sources().size() + " sources.");
        System.out.println("Manifest: " + mf.toAbsolutePath());
    }

    static void writeManifest(ObjectMapper om, Path mf, List<Map<String, Object>> manifest) {
        try {
            om.writerWithDefaultPrettyPrinter().writeValue(mf.toFile(), manifest);
        } catch (Exception e) {
            System.err.println("WARNING: could not write manifest: " + e);
        }
    }

    // data rows = non-blank lines minus the CSV header
    static long dataRows(Path p) throws Exception {
        long lines = 0;
        for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) lines++;
        }
        return Math.max(0, lines - 1);
    }

    static String sha256(Path p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(Files.readAllBytes(p));
        StringBuilder sb = new StringBuilder();
        for (byte b : d) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}