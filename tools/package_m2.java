///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//SOURCES LabVersions.java

// Build the prepackaged Maven repository for the lab, as a single zip.
//
// The notebooks resolve a fixed set of artifacts, including the gog4j version
// This script assembles a self-contained ~/.m2/repository into
// devoxx-hol-2026-m2.zip, which the "Getting everything" slide points at.
// Students unpack it into ~/.m2/repository and the whole lab runs offline.
//
// gog4j is published to GitHub Packages, which requires authentication
// even for a public package, so it is resolved:
//   * locally  - seeded from ~/.m2/repository if a developer installed it, or
//   * remotely - from GitHub Packages when GOG4J_REPO_PASSWORD is set
//                (that is what the .github workflow does)
//
// Requires: mvn on the PATH, and network access on first build.
//
// Usage: jbang tools/package_m2.java
//        jbang tools/package_m2.java --keep   (keep build/m2-bundle for inspection)
//
// Env (CI):
//   GOG4J_REPO_URL       default from tools/versions.properties
//   GOG4J_REPO_USER      default GITHUB_ACTOR
//   GOG4J_REPO_PASSWORD  default GITHUB_TOKEN   (needs packages: read)

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class package_m2 {

    static final Path ROOT = Path.of(".");
    static final Path LOCAL_M2 = Path.of(System.getProperty("user.home"), ".m2", "repository");
    static final Path WORK = ROOT.resolve("build").resolve("m2-bundle");
    static final Path REPO = WORK.resolve("repo");
    static final Path POM = WORK.resolve("pom.xml");
    static final Path ZIP = ROOT.resolve("devoxx-hol-2026-m2.zip");
    static final Path SETTINGS = WORK.resolve("settings.xml");

    // gog4j releases live on GitHub Packages; CI reads them from there
    static final String REMOTE_URL = envOr("GOG4J_REPO_URL", LabVersions.gog4jRepoUrl());
    static final String REMOTE_USER = envOr("GOG4J_REPO_USER", System.getenv("GITHUB_ACTOR"));
    static final String REMOTE_PASSWORD = envOr("GOG4J_REPO_PASSWORD", System.getenv("GITHUB_TOKEN"));

    static String envOr(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    static boolean remoteEnabled() {
        return REMOTE_PASSWORD != null && !REMOTE_PASSWORD.isBlank();
    }

    // exactly the coordinates the notebooks addDependency(...) on
    static final List<String> DEPS = List.of(
            "dev.hardwood:hardwood-core:1.1.0.Beta1",
            "org.dflib:dflib:2.0.0-M7",
            "org.dflib:dflib-parquet:2.0.0-M7",
            "org.dflib:dflib-csv:2.0.0-M7",
            "org.orekit:orekit:13.0.3",
            LabVersions.gog4jCoordinate("gog4j"),
            LabVersions.gog4jCoordinate("gog4j-hardwood"),
            LabVersions.gog4jCoordinate("gog4j-dflib"),
            LabVersions.gog4jCoordinate("gog4j-dflib-data"),
            LabVersions.gog4jCoordinate("gog4j-data"));

    // a developer may have installed the gog4j artifacts locally - seed those first
    static List<String> seedPaths() {
        List<String> out = new ArrayList<>();
        for (String gav : DEPS) {
            String[] p = gav.split(":");
            if (p[0].equals("org.jtaccuino")) {
                out.add("org/jtaccuino/" + p[1] + "/" + p[2]);
            }
        }
        return out;
    }

    // JavaFX picks a classifier per OS at build time; a conference lab is not one
    // OS, so pull every platform explicitly into the bundle
    static final String JAVAFX_VERSION = "26";
    static final List<String> JAVAFX_MODULES = List.of("javafx-base", "javafx-graphics", "javafx-controls");
    static final List<String> JAVAFX_PLATFORMS = List.of("linux", "linux-aarch64", "mac", "mac-aarch64", "win");

    public static void main(String[] args) throws Exception {
        boolean keep = List.of(args).contains("--keep");

        requireMvn();
        delete(WORK);
        Files.createDirectories(REPO);
        System.out.println("work dir : " + WORK.toAbsolutePath());

        int seeded = seedLocalArtifacts();
        System.out.println("seeded   : " + seeded + " local artifacts");
        if (remoteEnabled()) {
            System.out.println("remote   : " + REMOTE_URL + " (user " + REMOTE_USER + ")");
        } else {
            System.out.println("remote   : disabled (no GOG4J_REPO_PASSWORD / GITHUB_TOKEN)");
        }

        Files.writeString(POM, pom(), StandardCharsets.UTF_8);
        System.out.println("pom      : " + POM.toAbsolutePath());

        List<String> credentialArgs = List.of();
        if (remoteEnabled()) {
            Files.writeString(SETTINGS, settings(), StandardCharsets.UTF_8);
            credentialArgs = List.of("-s", SETTINGS.toString());
        }

        System.out.println("\nresolving the closure (online, first run downloads)...");
        String resolve = mvn(resolveArgs(credentialArgs).toArray(String[]::new));
        if (!resolve.contains("BUILD SUCCESS")) {
            System.out.println(resolve);
            throw new IllegalStateException("dependency:resolve failed");
        }

        System.out.println("\nverifying the bundle resolves with no network...");
        String offline = mvn(offlineArgs(credentialArgs).toArray(String[]::new));
        if (!offline.contains("BUILD SUCCESS")) {
            System.out.println(offline);
            throw new IllegalStateException("offline verification failed - the bundle is incomplete");
        }
        System.out.println("offline  : BUILD SUCCESS");

        long files = zip(REPO, ZIP);
        System.out.printf("%nwrote    : %s%n", ZIP.toAbsolutePath());
        System.out.printf("size     : %s   (%d files)%n", human(Files.size(ZIP)), files);

        if (!keep) {
            delete(WORK);
            System.out.println("cleaned  : " + WORK);
        } else {
            System.out.println("kept     : " + WORK);
        }
    }

    // ------------------------------------------------------------------ steps

    static List<String> resolveArgs(List<String> credentials) {
        List<String> args = new ArrayList<>(List.of("-B", "-ntp", "-U", "-f", POM.toString(),
                "-Dmaven.repo.local=" + REPO));
        args.addAll(credentials);
        args.add("dependency:resolve");
        return args;
    }

    static List<String> offlineArgs(List<String> credentials) {
        List<String> args = new ArrayList<>(List.of("-o", "-B", "-ntp", "-f", POM.toString(),
                "-Dmaven.repo.local=" + REPO));
        args.addAll(credentials);
        args.add("dependency:resolve");
        return args;
    }

    // minimal settings.xml that authenticates the GitHub Packages repository
    static String settings() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
                  <servers>
                    <server>
                      <id>github</id>
                      <username>%s</username>
                      <password>%s</password>
                    </server>
                  </servers>
                </settings>
                """.formatted(REMOTE_USER, REMOTE_PASSWORD);
    }

    static int seedLocalArtifacts() throws IOException {
        int count = 0;
        for (String artifact : seedPaths()) {
            Path src = LOCAL_M2.resolve(artifact);
            if (!Files.isDirectory(src)) {
                // not installed locally - it will be resolved from GitHub Packages
                System.out.println("  local miss: " + artifact
                        + (remoteEnabled() ? "  (GitHub Packages)" : ""));
                continue;
            }
            try (var walk = Files.walk(src)) {
                for (Path file : walk.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString();
                    if (name.endsWith("-sources.jar") || name.endsWith("-javadoc.jar")
                            || name.endsWith("-test-fixtures.jar") || name.endsWith(".lastUpdated")
                            || name.equals("_remote.repositories")
                            || name.startsWith("resolver-status")) {
                        continue;
                    }
                    Path target = REPO.resolve(LOCAL_M2.relativize(file).toString());
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                    // JTaccuino consumes ~/.m2/repository as a file:// REMOTE, which
                    // looks for maven-metadata.xml - expose the local copy under both names
                    if (name.equals("maven-metadata-local.xml")) {
                        Files.copy(file, target.getParent().resolve("maven-metadata.xml"),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                    count++;
                }
            }
        }
        return count;
    }

    static String pom() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.jtaccuino</groupId>
                  <artifactId>devoxx-hol-2026-deps</artifactId>
                  <version>1.0</version>
                  <packaging>pom</packaging>
                  <name>devoxx-hol-2026 lab dependencies</name>
                """);
        if (remoteEnabled()) {
            sb.append("  <repositories>\n")
              .append("    <repository>\n")
              .append("      <id>github</id>\n")
              .append("      <url>").append(REMOTE_URL).append("</url>\n")
              .append("      <snapshots><enabled>true</enabled></snapshots>\n")
              .append("      <releases><enabled>true</enabled></releases>\n")
              .append("    </repository>\n")
              .append("  </repositories>\n");
        }
        sb.append("  <dependencies>\n");
        for (String gav : DEPS) {
            String[] p = gav.split(":");
            sb.append("    <dependency><groupId>").append(p[0])
              .append("</groupId><artifactId>").append(p[1])
              .append("</artifactId><version>").append(p[2])
              .append("</version></dependency>\n");
        }
        for (String module : JAVAFX_MODULES) {
            for (String platform : JAVAFX_PLATFORMS) {
                sb.append("    <dependency><groupId>org.openjfx</groupId><artifactId>").append(module)
                  .append("</artifactId><version>").append(JAVAFX_VERSION)
                  .append("</version><classifier>").append(platform)
                  .append("</classifier></dependency>\n");
            }
        }
        sb.append("  </dependencies>\n</project>\n");
        return sb.toString();
    }

    static long zip(Path source, Path target) throws IOException {
        long count = 0;
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            try (var walk = Files.walk(source)) {
                for (Path file : walk.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(Path::toString)).toList()) {
                    String entry = source.relativize(file).toString().replace('\\', '/');
                    zip.putNextEntry(new ZipEntry(entry));
                    Files.copy(file, zip);
                    zip.closeEntry();
                    count++;
                }
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ util

    static void requireMvn() {
        try {
            mvn("-v");
        } catch (Exception e) {
            throw new IllegalStateException("mvn not found on PATH", e);
        }
    }

    static String mvn(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add("mvn");
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getOutputStream().close();
        String out;
        try (InputStream in = p.getInputStream()) {
            out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        p.waitFor();
        return out;
    }

    static void delete(Path root) throws IOException {
        if (!Files.exists(root)) return;
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                Files.delete(f);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static String human(long n) {
        if (n < 1024) return n + " B";
        if (n < 1024 * 1024) return String.format(java.util.Locale.ROOT, "%.1f KB", n / 1024.0);
        if (n < 1024L * 1024 * 1024) return String.format(java.util.Locale.ROOT, "%.1f MB", n / (1024.0 * 1024));
        return String.format(java.util.Locale.ROOT, "%.2f GB", n / (1024.0 * 1024 * 1024));
    }
}
