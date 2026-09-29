#!/usr/bin/env python3
"""Replay solution notebooks through JShell with the JTaccuino builtins stubbed.

Usage:  python3 tools/verify_notebooks.py [name ...]
        (no names = all)

Each notebook's code cells run in one shared JShell session, exactly like the
notebook. `addDependency` / `println` / `cwd` / `display` are the only builtins
that need stubbing; everything else is the notebook's own Java.
"""
import json
import os
import re
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOL = os.path.join(ROOT, "notebooks", "solutions")
JAVA_HOME = os.path.expanduser("~/.sdkman/candidates/java/27.0.0+35-zulu")

HOLDER = {
    "01-jtaccuino-basics": "e1",
    "02-parquet-with-hardwood": "e2",
    "03-dataframes-with-dflib": "e3",
    "04-plotting-with-gog4j": "e4",
}

PREAMBLE = """void addDependency(String gav) { System.out.println("[deps] " + gav); }
void println(String fmt, Object... args) { System.out.printf(fmt, args); System.out.println(); }
java.nio.file.Path cwd = java.nio.file.Path.of(".");
void display(Object node) { System.out.println("[display] " + (node == null ? "null" : node.getClass().getName())); }
"""

FX_START = """javafx.application.Platform.startup(() -> {});
System.out.println("[fx] toolkit started");
"""


def classpath(holder):
    out = subprocess.check_output(
        ["jbang", "info", "classpath", f"tools/cp/{holder}.java"],
        cwd=ROOT, stderr=subprocess.DEVNULL)
    cp = out.decode().strip()
    # drop the holder's own jar (first entry)
    return cp.split(":", 1)[1] if ":" in cp else cp


# The gog4j/JavaFX notebook cannot run as bare JShell snippets: Plot construction
# and SvgExporter both need the FX Application Thread. That notebook is instead
# reassembled into a jbang program whose body runs inside Platform.runLater.
def run_fx(name):
    with open(os.path.join(SOL, name + ".ipynb"), encoding="utf-8") as f:
        nb = json.load(f)
    cells = [c["source"] for c in nb["cells"] if c["cell_type"] == "code"]

    imports, methods, body = [], [], []

    def split_cell(src):
        lines = src.splitlines()
        i = 0
        while i < len(lines):
            line = lines[i]
            if line.startswith("import "):
                imports.append(line)
                i += 1
                continue
            if line.startswith("void save("):
                depth, start = 0, i
                while i < len(lines):
                    depth += lines[i].count("{") - lines[i].count("}")
                    i += 1
                    if depth == 0:
                        break
                method = ["static " + lines[k] if k == start else lines[k]
                          for k in range(start, i)]
                methods.append("\n".join(method))
                continue
            body.append(line)
            i += 1

    for src in cells:
        split_cell(src)

    body_src = re.sub(r"(?m)^(\s*addDependency\([^;\n]*\))\s*$", r"\1;", "\n".join(body))
    program = """//JAVA 27
//DEPS org.jtaccuino:gog4j:0.5-SNAPSHOT
//DEPS org.jtaccuino:gog4j-hardwood:0.5-SNAPSHOT
//DEPS org.dflib:dflib:2.0.0-M7
//DEPS org.dflib:dflib-parquet:2.0.0-M7
""" + "\n".join(imports) + """

class NotebookRun {
    static void addDependency(String gav) { System.out.println("[deps] " + gav); }
    static void println(String fmt, Object... args) { System.out.printf(fmt, args); System.out.println(); }
    static java.nio.file.Path cwd = java.nio.file.Path.of(".");
    static void display(Object node) { System.out.println("[display] " + (node == null ? "null" : node.getClass().getSimpleName())); }
""" + "\n".join(methods) + """

    public static void main(String[] a) throws Exception {
        var up = new java.util.concurrent.CountDownLatch(1);
        javafx.application.Platform.startup(up::countDown); up.await();
        var done = new java.util.concurrent.CountDownLatch(1);
        javafx.application.Platform.runLater(() -> {
            try {
""" + "\n".join("                " + l for l in body_src.splitlines()) + """
            } catch (Throwable t) { t.printStackTrace(); }
            finally { done.countDown(); }
        });
        done.await();
        javafx.application.Platform.exit();
    }
}
"""
    with tempfile.NamedTemporaryFile("w", suffix=".java", delete=False) as f:
        f.write(program)
        path = f.name
    env = dict(os.environ, JAVA_HOME=JAVA_HOME, PATH=f"{JAVA_HOME}/bin:" + os.environ["PATH"])
    proc = subprocess.run(["jbang", path], cwd=ROOT, env=env, capture_output=True, text=True)
    os.unlink(path)
    text = proc.stdout + proc.stderr
    bad = [l for l in text.splitlines()
           if "Exception" in l or "Fehler:" in l or "[ERROR]" in l
           or " error" in l or l.startswith("FAIL")]
    return text, bad


def run(name):
    if name.startswith("04"):
        return run_fx(name)
    with open(os.path.join(SOL, name + ".ipynb"), encoding="utf-8") as f:
        nb = json.load(f)
    cells = [c["source"] for c in nb["cells"] if c["cell_type"] == "code"]

    script = PREAMBLE + "\n"
    script += "\n".join(cells) + "\n"

    with tempfile.NamedTemporaryFile("w", suffix=".jsh", delete=False) as f:
        f.write(script)
        path = f.name

    env = dict(os.environ, JAVA_HOME=JAVA_HOME, PATH=f"{JAVA_HOME}/bin:" + os.environ["PATH"])
    proc = subprocess.run(
        ["jshell", "--class-path", classpath(HOLDER[name]),
         "-J-Duser.language=en", "-J-Duser.country=US",
         "-R-Duser.language=en", "-R-Duser.country=US",
         "-q", path],
        cwd=ROOT, env=env, capture_output=True, text=True)
    os.unlink(path)

    text = proc.stdout + proc.stderr
    bad = [l for l in text.splitlines()
           if l.startswith("|  Error:") or "Exception" in l or "error:" in l
           or l.strip().startswith("Error:")]
    return text, bad


def main():
    names = sys.argv[1:] or list(HOLDER)
    rc = 0
    for name in names:
        text, bad = run(name)
        status = "PASS" if not bad else "FAIL"
        print(f"\n{'='*70}\n{status}  {name}\n{'='*70}")
        tail = [l for l in text.splitlines() if l.strip()]
        print("\n".join(tail[-60:]))
        if bad:
            rc = 1
            print("\n!! problems:")
            for l in bad[:14]:
                print("   " + l.strip())
    sys.exit(rc)


if __name__ == "__main__":
    main()
