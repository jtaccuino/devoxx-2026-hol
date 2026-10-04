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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            LabVersions.deepnettsCoreCoordinate(),
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

    // JavaFX is deliberately NOT packaged. The notebooks addDependency(...) only
    // gog4j; gog4j depends on JavaFX at *runtime* scope, and students run the
    // notebooks inside the JTaccuino release, which already brings JavaFX for
    // their platform. Packaging five platform classifiers cost ~45 MB and was
    // the single biggest thing in the zip.
    //
    // Maven's own plugin machinery ends up in the same local repository because
    // dependency:resolve runs the plugin against it; none of it belongs in a
    // repository that only has to satisfy the notebooks.
    static final Pattern EXCLUDED_ARTIFACT_PATHS = Pattern.compile(
            "^org/openjfx/.*|^org/apache/maven/.*|^org/codehaus/plexus/.*"
            + "|^org/sonatype/plexus/.*|^org/apache/velocity/.*");

    // The gog4j 0.5.0 POMs declare some dependencies without a version, relying
    // on an imported BOM (they are published from Gradle with module metadata).
    // Maven tolerates it with a warning; jbang's Aether rejects the descriptor
    // outright ("Could not read artifact descriptor"), which broke CI. The
    // versions come from the BOMs the same POMs import.
    static final java.util.Map<String, String> DEPENDENCY_VERSIONS = java.util.Map.of(
            "dflib", "2.0.0-M7",
            "dflib-csv", "2.0.0-M7",
            "dflib-parquet", "2.0.0-M7",
            "hardwood-core", "1.1.0.Beta1",
            "zstd-jni", "1.5.7-9");

    static final Pattern DEPENDENCY = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL);
    static final Pattern ARTIFACT_ID = Pattern.compile("<artifactId>(.*?)</artifactId>");

    /**
     * Returns the POM with a version injected into every dependency that lacks
     * one, when we know that artifact's version. Unknown artifacts are left
     * alone - better an untouched POM than a wrong version.
     */
    static String repairPom(String xml) {
        StringBuilder out = new StringBuilder();
        Matcher block = DEPENDENCY.matcher(xml);
        int last = 0;
        while (block.find()) {
            String dependency = block.group(1);
            out.append(xml, last, block.start());
            last = block.end();
            if (dependency.contains("<version>")) {
                out.append(block.group());
                continue;
            }
            Matcher id = ARTIFACT_ID.matcher(dependency);
            String version = id.find() ? DEPENDENCY_VERSIONS.get(id.group(1)) : null;
            if (version == null) {
                out.append(block.group());
                continue;
            }
            // insert <version> at the end of the dependency's body (the group
            // captured between <dependency> and </dependency>)
            String injected = dependency.stripTrailing()
                    + "\n      <version>" + version + "</version>\n    ";
            out.append("<dependency>").append(injected).append("</dependency>");
        }
        out.append(xml.substring(last));
        return out.toString();
    }

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

        Set<String> resolved = resolvedArtifactDirs(credentialArgs);
        System.out.println("resolved : " + resolved.size() + " artifacts in the closure");

        long files = zip(REPO, ZIP, resolved);
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
                "-Dmaven.repo.local=" + REPO, "-DincludeScope=runtime"));
        args.addAll(credentials);
        args.add("dependency:resolve");
        return args;
    }

    static List<String> offlineArgs(List<String> credentials) {
        List<String> args = new ArrayList<>(List.of("-o", "-B", "-ntp", "-f", POM.toString(),
                "-Dmaven.repo.local=" + REPO, "-DincludeScope=runtime"));
        args.addAll(credentials);
        args.add("dependency:resolve");
        return args;
    }

    // The repository holds every version Maven downloaded while mediating the
    // graph; only one version of each artifact is actually resolved. dependency:list
    // tells us which, so we can drop the losing versions (e.g. the extra zstd-jni).
    static List<String> listArgs(List<String> credentials) {
        List<String> args = new ArrayList<>(List.of("-o", "-B", "-ntp", "-f", POM.toString(),
                "-Dmaven.repo.local=" + REPO, "-DincludeScope=runtime"));
        args.addAll(credentials);
        args.add("dependency:list");
        return args;
    }

    static final Pattern RESOLVED_LINE =
            Pattern.compile("([\\w.\\-]+):([\\w.\\-]+):jar:([\\w.\\-]+):(compile|runtime)");

    static Set<String> resolvedArtifactDirs(List<String> credentials)
            throws IOException, InterruptedException {
        String out = mvn(listArgs(credentials).toArray(String[]::new));
        Set<String> dirs = new HashSet<>();
        for (String line : out.split("\\R")) {
            Matcher m = RESOLVED_LINE.matcher(line);
            if (m.find()) {
                dirs.add(m.group(1).replace('.', '/') + "/" + m.group(2) + "/" + m.group(3));
            }
        }
        return dirs;
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
              .append("</version>")
              // JavaFX comes from the JTaccuino runtime, never from this bundle
              .append("<exclusions><exclusion>")
              .append("<groupId>org.openjfx</groupId><artifactId>*</artifactId>")
              .append("</exclusion></exclusions>")
              .append("</dependency>\n");
        }
        sb.append("  </dependencies>\n</project>\n");
        return sb.toString();
    }

    static long zip(Path source, Path target, Set<String> resolved) throws IOException {
        long count = 0;
        long skipped = 0;
        long repaired = 0;
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            try (var walk = Files.walk(source)) {
                for (Path file : walk.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(Path::toString)).toList()) {
                    // Do not ship Maven's bookkeeping: `_remote.repositories`
                    // records which repository an artifact came from, and a
                    // `*.lastUpdated` marks a failed resolution. Both make a
                    // consumer that unpacks this bundle refuse to use the
                    // artifact ("came from the wrong repository" / "cached
                    // failure"), and they are pure noise in a prepackaged repo.
                    if (isMavenBookkeeping(file.getFileName().toString())) {
                        skipped++;
                        continue;
                    }
                    // Anything the notebooks never need: JavaFX (provided by the
                    // JTaccuino runtime) and Maven's own plugin machinery, which
                    // dependency:resolve drags into the same local repository.
                    String relative = source.relativize(file).toString().replace('\\', '/');
                    if (EXCLUDED_ARTIFACT_PATHS.matcher(relative).find()) {
                        skipped++;
                        continue;
                    }
                    String name = file.getFileName().toString();

                    // Drop the losing versions: keep every artifact directory's
                    // POM/metadata so resolution can still mediate, but only ship
                    // the jar for the version dependency:list resolved.
                    if (name.endsWith(".jar")) {
                        String artifactDir = relative.substring(0, relative.lastIndexOf('/'));
                        if (!resolved.contains(artifactDir)) {
                            skipped++;
                            continue;
                        }
                    }

                    // A repaired gog4j POM no longer matches its published .sha1,
                    // so ship the POM without the checksum rather than a wrong one.
                    if (name.endsWith(".pom.sha1") || name.endsWith(".pom.md5")
                            || name.endsWith(".pom.sha256") || name.endsWith(".pom.sha512")) {
                        if (file.getParent().toString().contains("org/jtaccuino")) {
                            skipped++;
                            continue;
                        }
                    }

                    String entry = relative;
                    zip.putNextEntry(new ZipEntry(entry));
                    if (name.endsWith(".pom")) {
                        String xml = Files.readString(file, StandardCharsets.UTF_8);
                        String fixed = repairPom(xml);
                        if (!fixed.equals(xml)) {
                            repaired++;
                            System.out.println("repaired : " + entry);
                        }
                        zip.write(fixed.getBytes(StandardCharsets.UTF_8));
                    } else {
                        Files.copy(file, zip);
                    }
                    zip.closeEntry();
                    count++;
                }
            }
        }
        if (skipped > 0) {
            System.out.println("skipped  : " + skipped + " Maven bookkeeping files");
        }
        if (repaired > 0) {
            System.out.println("repaired : " + repaired + " POM(s) missing dependency versions");
        }
        return count;
    }

    static boolean isMavenBookkeeping(String name) {
        return name.equals("_remote.repositories")
                || name.endsWith(".lastUpdated")
                || name.startsWith("resolver-status");
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
