///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 27
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.2
//DEPS org.openjfx:javafx-base:26
//DEPS org.openjfx:javafx-graphics:26
//DEPS org.openjfx:javafx-controls:26

// Replay a solution notebook the way JTaccuino does: the real JShell *local*
// execution engine, dependencies added with JShell.addToClasspath (not on the
// launch classpath), and every statement evaluated as its own snippet.
//
// This catches failures the jbang-based harness cannot: with the local engine,
// JDK 24+ instruments each snippet with the Class-File API, and the stack-map
// generator resolves referenced classes through the *system* class loader - a
// class added via addToClasspath cannot be resolved there, so a snippet that
// puts such a type on a stack-map merge point fails with
// "IllegalArgumentException: Could not resolve class ...".
//
// Usage: jbang tools/verify_jshell.java <notebook> <classpath> <cwd>

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jdk.jshell.JShell;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class verify_jshell {

    static final List<String> STUBS = List.of(
            "void addDependency(String gav) { }",
            "void use(String extension) { }",
            "void println(String fmt, Object... args) { System.out.printf(fmt, args); System.out.println(); }",
            "void println(Object value, Object... args) { System.out.println(String.valueOf(value)); }",
            "void display(Object node) { }");

    public static void main(String[] args) throws Exception {
        Path notebook = Paths.get(args[0]);
        String classpath = args[1];
        Path cwd = Paths.get(args[2]);

        JShell js = JShell.builder().executionEngine("local").build();
        // JTaccuino is a JavaFX app, so the toolkit is already up there
        try {
            javafx.application.Platform.startup(() -> { });
        } catch (Throwable ignored) {
            // already started, or no toolkit available
        }
        for (String entry : classpath.split(":")) {
            if (entry.endsWith(".jar")) {
                js.addToClasspath(entry);
            }
        }
        for (String stub : STUBS) {
            report("stub", js.eval(stub));
        }
        report("stub", js.eval("java.nio.file.Path cwd = java.nio.file.Path.of(\"" + cwd + "\");"));

        int failures = 0;
        int cells = 0;
        JsonNode root = new ObjectMapper().readTree(notebook.toFile());
        for (JsonNode cell : root.get("cells")) {
            if (!cell.get("cell_type").asText().equals("code")) {
                continue;
            }
            cells++;
            String source = cell.get("source").asText();
            // ReactiveJShell.eval does exactly this: split the cell into snippets
            // via analyzeCompletion and evaluate each one on its own
            String remaining = source;
            SourceCodeAnalysis.CompletionInfo completion;
            do {
                completion = js.sourceCodeAnalysis().analyzeCompletion(remaining);
                if (completion.completeness().isComplete()) {
                    try {
                        for (SnippetEvent event : js.eval(completion.source())) {
                            if (event.exception() != null) {
                                System.out.println("EXCEPTION: " + firstLine(event.snippet().source())
                                        + "\n           " + event.exception());
                                failures++;
                            } else if (event.status() == Snippet.Status.REJECTED) {
                                System.out.println("REJECTED : " + firstLine(event.snippet().source()));
                                failures++;
                            }
                        }
                    } catch (Throwable t) {
                        System.out.println("THREW    : " + firstLine(completion.source()) + "\n           " + t);
                        failures++;
                    }
                }
                remaining = completion.remaining().replaceFirst("\\s*", "");
            } while (!remaining.isEmpty()
                    && completion.completeness() != SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE);
        }
        System.out.println((failures == 0 ? "PASS" : "FAIL") + "  " + notebook.getFileName()
                + "  (" + cells + " code cells, " + failures + " problem(s))");
        js.close();
        System.exit(failures == 0 ? 0 : 1);
    }

    static String firstLine(String s) {
        String t = s.strip();
        int nl = t.indexOf('\n');
        return nl < 0 ? t : t.substring(0, nl);
    }

    static void report(String label, List<SnippetEvent> events) {
        for (SnippetEvent e : events) {
            if (e.exception() != null) {
                System.out.println(label + " EXCEPTION: " + e.exception());
            } else if (e.status() == Snippet.Status.REJECTED) {
                System.out.println(label + " REJECTED : " + firstLine(e.snippet().source()));
            }
        }
    }
}
