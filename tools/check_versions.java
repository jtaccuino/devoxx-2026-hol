///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//SOURCES LabVersions.java

// Check that the gog4j version is consistent everywhere.
//
// tools/versions.properties is the source of truth, but tools/cp/*.java cannot
// read it (jbang parses those files with no templating), so they carry the same
// value literally. This fails with a non-zero exit code if they drift.
//
// Usage: jbang tools/check_versions.java

import java.util.List;

public class check_versions {

    public static void main(String[] args) {
        String version = LabVersions.gog4jVersion();
        System.out.println("versions.properties : gog4j " + version);
        System.out.println("                      " + LabVersions.gog4jRepoUrl());
        System.out.println("                      " + LabVersions.gog4jCoordinate("gog4j"));

        List<String> problems = LabVersions.mismatchedClasspathHolders();
        if (problems.isEmpty()) {
            System.out.println("\nOK: tools/cp/*.java all agree with " + version);
        } else {
            System.out.println("\nMISMATCH in tools/cp/*.java:");
            problems.forEach(p -> System.out.println("  " + p));
            System.exit(1);
        }
    }
}
