///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21

// Load the lab's dependency versions from tools/versions.properties, so the
// version of gog4j lives in exactly one place. This file is pulled into the
// other tools with a jbang //SOURCES line; it is not meant to be run alone.
//
// It also checks that tools/cp/*.java - which jbang parses directly, so they
// cannot be templated - carry the same version. Run it to check, or let
// tools/check_versions.java do exactly that:
//
//     jbang tools/check_versions.java

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class LabVersions {

    private static final Properties PROPS = load();

    private LabVersions() {
    }

    private static Properties load() {
        try {
            var props = new Properties();
            var file = Path.of("tools", "versions.properties");
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("cannot find " + file.toAbsolutePath()
                        + " - run from the repository root");
            }
            try (var in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(in);
            }
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read tools/versions.properties", e);
        }
    }

    /** The value of a property, or a clear failure if it is missing. */
    static String get(String key) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new MissingResourceException("no such key", "versions.properties", key);
        }
        return value;
    }

    static String gog4jVersion() {
        return get("gog4j.version");
    }

    static String gog4jRepoUrl() {
        return get("gog4j.repo.url");
    }

    /** "org.jtaccuino:gog4j-hardwood:<version>" */
    static String gog4jCoordinate(String artifact) {
        return "org.jtaccuino:" + artifact + ":" + gog4jVersion();
    }

    static String deepnettsVersion() {
        return get("deepnetts.version");
    }

    /** "com.deepnetts:deepnetts-core:<version>" */
    static String deepnettsCoreCoordinate() {
        return "com.deepnetts:deepnetts-core:" + deepnettsVersion();
    }

    /**
     * Returns the mismatched lines in tools/cp/*.java, or an empty list when
     * they all agree with the properties file.
     */
    static java.util.List<String> mismatchedClasspathHolders() {
        var expected = gog4jVersion();
        var literal = Pattern.compile("org\\.jtaccuino:([A-Za-z0-9-]+):([^\\s\"]+)");
        var problems = new java.util.ArrayList<String>();
        try (Stream<Path> files = Files.list(Path.of("tools", "cp"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                var text = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = literal.matcher(text);
                while (m.find()) {
                    if (!m.group(2).equals(expected)) {
                        problems.add(file.getFileName() + ": " + m.group(0)
                                + " (expected " + expected + ")");
                    }
                }
            }
        } catch (IOException e) {
            problems.add("cannot read tools/cp: " + e);
        }
        return problems;
    }
}
