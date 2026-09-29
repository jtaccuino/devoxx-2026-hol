///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 27
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.2

// Replay the solution notebooks through JShell with the JTaccuino builtins
// stubbed, so every cell is checked exactly as a student would run it.
//
// Usage (from the repository root):
//   jbang tools/verify_notebooks.java                 # all
//   jbang tools/verify_notebooks.java 03-dataframes-with-dflib
//
// The JavaFX notebooks (04, the penguins worksheet, the 3-D bonus) cannot run
// as bare JShell snippets: gog4j plot construction and SvgExporter both need the
// JavaFX Application Thread. Those are reassembled into a jbang program whose
// body runs inside Platform.runLater.

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class verify_notebooks {

    static final Path ROOT = Path.of(".");
    static final String JAVA_HOME = Path.of(System.getProperty("user.home"),
            ".sdkman/candidates/java/27.0.0+35-zulu").toString();
    static final ObjectMapper OM = new ObjectMapper();
    static final Pattern METHOD_START = Pattern.compile("^(?:String|double|int|long|boolean|void|Path)\\s+\\w+\\(");
    static final Pattern ADD_DEP = Pattern.compile("(?m)^(\\s*addDependency\\([^;\\n]*\\))\\s*$");

    record Job(String label, String file, String holder, String fx) {}

    static final List<Job> JOBS = List.of(
            new Job("01-jtaccuino-basics", "notebooks/solutions/01-jtaccuino-basics.ipynb", "e1", null),
            new Job("02-parquet-with-hardwood", "notebooks/solutions/02-parquet-with-hardwood.ipynb", "e2", null),
            new Job("03-dataframes-with-dflib", "notebooks/solutions/03-dataframes-with-dflib.ipynb", "e3", null),
            new Job("04-plotting-with-gog4j", "notebooks/solutions/04-plotting-with-gog4j.ipynb", "e4", "04"),
            new Job("penguins-worksheet-solutions", "notebooks/fallback/penguins-worksheet-solutions.ipynb", "penguins", "penguins"),
            new Job("orbits-now-3d-solutions", "notebooks/bonus/orbits-now-3d-solutions.ipynb", "bonus3d", "orbits"),
            new Job("orbits-live-3d-solutions", "notebooks/bonus/orbits-live-3d-solutions.ipynb", "bonus3d", "orbits"));

    static final String PREAMBLE = """
            void addDependency(String gav) { System.out.println("[deps] " + gav); }
            void println(String fmt, Object... args) { System.out.printf(fmt, args); System.out.println(); }
            java.nio.file.Path cwd = {{CWD}};
            void display(Object node) { System.out.println("[display] " + (node == null ? "null" : node.getClass().getName())); }
            """;

    static final String FX_TEMPLATE = """
            //JAVA 27
            {{DEPS}}
            {{IMPORTS}}

            class NotebookRun {
                static void addDependency(String gav) { System.out.println("[deps] " + gav); }
                static void println(String fmt, Object... args) { System.out.printf(fmt, args); System.out.println(); }
                static java.nio.file.Path cwd = {{CWD}};
                static javafx.scene.layout.Pane __sink;
                // faithful to JTaccuino: display() marshals to the FX thread and
                // attaches the node to a live scene, so off-thread mutation fails
                static void display(Object node) {
                    javafx.application.Platform.runLater(() -> {
                        try {
                            if (node instanceof javafx.scene.Node n) {
                                if (__sink == null) {
                                    __sink = new javafx.scene.layout.Pane();
                                    new javafx.scene.Scene(__sink);
                                }
                                __sink.getChildren().add(n);
                            }
                            System.out.println("[display] " + (node == null ? "null" : node.getClass().getSimpleName()));
                        } catch (Throwable t) { t.printStackTrace(); }
                    });
                }
            {{METHODS}}

                public static void main(String[] args) throws Exception {
                    var up = new java.util.concurrent.CountDownLatch(1);
                    javafx.application.Platform.startup(up::countDown);
                    up.await();
                    var done = new java.util.concurrent.CountDownLatch(1);
                    // JTaccuino evaluates snippets on a worker thread, not on the
                    // FX thread - run the body the same way
                    var worker = new Thread(() -> {
                        try {
            {{BODY}}
                        } catch (Throwable t) { t.printStackTrace(); }
                        finally { done.countDown(); }
                    }, "jshell-worker");
                    worker.start();
                    done.await();
                    var settle = new java.util.concurrent.CountDownLatch(1);
                    javafx.application.Platform.runLater(settle::countDown);
                    settle.await();
                    javafx.application.Platform.exit();
                }
            }
            """;

    public static void main(String[] args) throws Exception {
        List<String> svgBefore = svgs();
        int failures = 0;
        List<Job> jobs = new ArrayList<>();
        for (Job j : JOBS) {
            if (args.length == 0 || Stream.of(args).anyMatch(a -> j.label().contains(a))) jobs.add(j);
        }

        for (Job job : jobs) {
            List<String> code = codeCells(Path.of(job.file()));
            Result r = job.fx() == null ? runJShell(job, code) : runFx(job, code);
            List<String> bad = problems(r.output());
            System.out.println("\n" + "=".repeat(70));
            System.out.println((bad.isEmpty() ? "PASS" : "FAIL") + "  " + job.label());
            System.out.println("=".repeat(70));
            List<String> tail = r.output().lines().filter(l -> !l.isBlank()).toList();
            tail.subList(Math.max(0, tail.size() - 60), tail.size()).forEach(System.out::println);
            if (!bad.isEmpty()) {
                failures++;
                System.out.println("\n!! problems:");
                bad.stream().limit(14).forEach(l -> System.out.println("   " + l.strip()));
            }
        }

        for (String svg : svgs()) {
            if (!svgBefore.contains(svg)) Files.deleteIfExists(Path.of(svg));
        }
        System.exit(failures == 0 ? 0 : 1);
    }

    record Result(String output) {}

    static List<String> codeCells(Path notebook) throws IOException {
        JsonNode root = OM.readTree(notebook.toFile());
        List<String> out = new ArrayList<>();
        for (JsonNode cell : root.get("cells")) {
            if (cell.get("cell_type").asText().equals("code")) out.add(cell.get("source").asText());
        }
        return out;
    }

    static Result runJShell(Job job, List<String> code) throws Exception {
        StringBuilder script = new StringBuilder(PREAMBLE.replace("{{CWD}}", cwdExpr(job)));
        for (String c : code) script.append(c).append("\n");
        Path tmp = Files.createTempFile("notebook", ".jsh");
        Files.writeString(tmp, script, StandardCharsets.UTF_8);

        String cp = classpath(job.holder());
        List<String> cmd = List.of("jshell", "--class-path", cp,
                "-J-Duser.language=en", "-J-Duser.country=US",
                "-R-Duser.language=en", "-R-Duser.country=US",
                "-q", tmp.toString());
        Result r = exec(cmd);
        Files.deleteIfExists(tmp);
        return r;
    }

    static Result runFx(Job job, List<String> code) throws Exception {
        List<String> imports = new ArrayList<>();
        List<String> methods = new ArrayList<>();
        StringBuilder body = new StringBuilder();

        for (String cell : code) {
            String[] lines = cell.split("\n", -1);
            int i = 0;
            while (i < lines.length) {
                String line = lines[i];
                if (line.startsWith("import ")) {
                    imports.add(line);
                    i++;
                    continue;
                }
                if (METHOD_START.matcher(line).find()) {
                    int depth = 0;
                    int start = i;
                    while (i < lines.length) {
                        depth += count(lines[i], '{') - count(lines[i], '}');
                        i++;
                        if (depth == 0) break;
                    }
                    StringBuilder m = new StringBuilder();
                    for (int k = start; k < i; k++) {
                        if (k == start) m.append("static ");
                        m.append(lines[k]);
                        if (k + 1 < i) m.append('\n');
                    }
                    methods.add(m.toString());
                    continue;
                }
                body.append(line).append('\n');
                i++;
            }
        }

        String bodyText = ADD_DEP.matcher(body.toString()).replaceAll("$1;");
        StringBuilder indented = new StringBuilder();
        for (String line : bodyText.split("\n", -1)) indented.append("        ").append(line).append('\n');

        StringBuilder deps = new StringBuilder();
        for (String d : fxDeps(job.fx())) deps.append("//DEPS ").append(d).append('\n');

        String program = FX_TEMPLATE
                .replace("{{CWD}}", cwdExpr(job))
                .replace("{{DEPS}}", deps.toString().strip())
                .replace("{{IMPORTS}}", String.join("\n", imports))
                .replace("{{METHODS}}", methods.isEmpty() ? "" : String.join("\n", methods))
                .replace("{{BODY}}", indented.toString().stripTrailing());

        Path tmp = Files.createTempFile("NotebookRun", ".java");
        Files.writeString(tmp, program, StandardCharsets.UTF_8);
        Result r = exec(List.of("jbang", tmp.toString()));
        Files.deleteIfExists(tmp);
        return r;
    }

    static List<String> fxDeps(String key) {
        return switch (key) {
            case "04" -> List.of(
                    "org.jtaccuino:gog4j:0.5-SNAPSHOT",
                    "org.jtaccuino:gog4j-hardwood:0.5-SNAPSHOT",
                    "org.dflib:dflib:2.0.0-M7",
                    "org.dflib:dflib-parquet:2.0.0-M7");
            case "orbits" -> List.of(
                    "org.orekit:orekit:13.0.3",
                    "org.dflib:dflib:2.0.0-M7",
                    "org.dflib:dflib-parquet:2.0.0-M7",
                    "org.jtaccuino:gog4j:0.5-SNAPSHOT",
                    "org.jtaccuino:gog4j-hardwood:0.5-SNAPSHOT");
            default -> List.of(
                    "org.jtaccuino:gog4j:0.5-SNAPSHOT",
                    "org.jtaccuino:gog4j-dflib:0.5-SNAPSHOT",
                    "org.jtaccuino:gog4j-dflib-data:0.5-SNAPSHOT",
                    "org.jtaccuino:gog4j-data:0.5-SNAPSHOT",
                    "org.dflib:dflib:2.0.0-M7",
                    "org.dflib:dflib-csv:2.0.0-M7");
        };
    }

    static String cwdExpr(Job job) {
        // run each notebook from its own folder, exactly as JTaccuino does
        String dir = Path.of(job.file()).toAbsolutePath().getParent().toString().replace("\\", "\\\\");
        return "java.nio.file.Path.of(\"" + dir + "\")";
    }

    static String classpath(String holder) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("jbang", "info", "classpath", "tools/cp/" + holder + ".java");
        pb.environment().put("JAVA_HOME", JAVA_HOME);
        pb.environment().put("PATH", JAVA_HOME + "/bin:" + pb.environment().get("PATH"));
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        p.getOutputStream().close();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        p.waitFor();
        int idx = out.indexOf(':');
        return idx >= 0 ? out.substring(idx + 1) : out;
    }

    static Result exec(List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.environment().put("JAVA_HOME", JAVA_HOME);
        pb.environment().put("PATH", JAVA_HOME + "/bin:" + pb.environment().get("PATH"));
        Process p = pb.start();
        p.getOutputStream().close();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();
        return new Result(output);
    }

    static List<String> problems(String text) {
        return text.lines()
                .filter(l -> l.contains("Error:") || l.contains("Exception")
                        || l.contains("cannot find symbol") || l.contains("[ERROR]")
                        || l.startsWith("FAIL"))
                .toList();
    }

    static List<String> svgs() throws IOException {
        try (Stream<Path> s = Files.walk(ROOT)) {
            return s.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(n -> n.endsWith(".svg"))
                    .sorted().toList();
        }
    }

    static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }
}
