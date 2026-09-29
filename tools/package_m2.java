///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21

// Build the prepackaged Maven repository for the lab, as a single zip.
//
// The notebooks resolve a fixed set of artifacts, including 0.5-SNAPSHOT builds
// of gog4j that are not on Maven Central. This script assembles a self-contained
// ~/.m2/repository into devoxx-hol-2026-m2.zip, which the "Getting everything"
// slide points at. Students unpack it into ~/.m2/repository and the whole lab
// runs offline.
//
// Requires: mvn on the PATH, and network access on first build.
//
// Usage: jbang tools/package_m2.java
//        jbang tools/package_m2.java --keep   (keep build/m2-bundle for inspection)

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

    // exactly the coordinates the notebooks addDependency(...) on
    static final List<String> DEPS = List.of(
            "dev.hardwood:hardwood-core:1.1.0.Beta1",
            "org.dflib:dflib:2.0.0-M7",
            "org.dflib:dflib-parquet:2.0.0-M7",
            "org.dflib:dflib-csv:2.0.0-M7",
            "org.orekit:orekit:13.0.3",
            "org.jtaccuino:gog4j:0.5-SNAPSHOT",
            "org.jtaccuino:gog4j-hardwood:0.5-SNAPSHOT",
            "org.jtaccuino:gog4j-dflib:0.5-SNAPSHOT",
            "org.jtaccuino:gog4j-dflib-data:0.5-SNAPSHOT",
            "org.jtaccuino:gog4j-data:0.5-SNAPSHOT");

    // the SNAPSHOTs are only in the local ~/.m2, so seed them before resolving
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

        int seeded = seedSnapshots();
        System.out.println("seeded   : " + seeded + " local SNAPSHOT files");

        Files.writeString(POM, pom(), StandardCharsets.UTF_8);
        System.out.println("pom      : " + POM.toAbsolutePath());

        System.out.println("\nresolving the closure (online, first run downloads)...");
        String resolve = mvn("-B", "-ntp", "-f", POM.toString(),
                "-Dmaven.repo.local=" + REPO, "dependency:resolve");
        if (!resolve.contains("BUILD SUCCESS")) {
            System.out.println(resolve);
            throw new IllegalStateException("dependency:resolve failed");
        }

        System.out.println("\nverifying the bundle resolves with no network...");
        String offline = mvn("-o", "-B", "-ntp", "-f", POM.toString(),
                "-Dmaven.repo.local=" + REPO, "dependency:resolve");
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

    static int seedSnapshots() throws IOException {
        int count = 0;
        for (String artifact : seedPaths()) {
            Path src = LOCAL_M2.resolve(artifact);
            if (!Files.isDirectory(src)) {
                throw new IllegalStateException("missing local artifact: " + src
                        + " (build gog4j with 'mvn install' first)");
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
                  <dependencies>
                """);
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
