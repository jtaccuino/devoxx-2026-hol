///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//MAIN readcheck
//DEPS dev.hardwood:hardwood-core:1.1.0.Beta1
//DEPS org.dflib:dflib:2.0.0-M6
//DEPS org.dflib:dflib-csv:2.0.0-M6

// Independent read-back verification of the exported Parquet with hardwood:
//   - prints the schema as hardwood sees it
//   - streams every row and counts them
//   - cross-checks the row count against the CSV (via dflib)
//
// Usage: jbang read-check.java

import dev.hardwood.InputFile;
import dev.hardwood.reader.ParquetFileReader;
import dev.hardwood.reader.RowReader;
import dev.hardwood.schema.ColumnSchema;
import dev.hardwood.schema.FileSchema;
import org.dflib.DataFrame;
import org.dflib.csv.Csv;

import java.nio.file.Files;
import java.nio.file.Path;

class readcheck {

    public static void main(String[] args) throws Exception {
        Path pq = Path.of("data", "out", "celestrak_gp_catalog.parquet");
        Path csv = Path.of("data", "out", "celestrak_gp_catalog.csv");
        if (!Files.exists(pq)) {
            System.err.println("parquet not found - run build.java first");
            System.exit(1);
        }

        try (ParquetFileReader fr = ParquetFileReader.open(InputFile.of(pq))) {
            FileSchema schema = fr.getFileSchema();
            System.out.println("hardwood schema (" + schema.getColumnCount() + " columns):");
            for (ColumnSchema c : schema.getColumns()) {
                System.out.println("  " + c.name());
            }

            long n = 0;
            try (RowReader rr = fr.rowReader()) {
                while (rr.hasNext()) {
                    rr.next();
                    n++;
                    if (n <= 3) {
                        String name = safeStr(rr, "object_name");
                        String tle3 = safeStr(rr, "tle3");
                        System.out.println("sample: catnr=" + rr.getLong("norad_cat_id")
                                + " name=" + name
                                + " tle3=" + (tle3 == null ? "<null>" : tle3.replace('\n', '|')));
                    }
                }
            }
            System.out.println("hardwood row count : " + n);

            DataFrame df = Csv.load(csv.toString());
            long csvRows = df.height();
            System.out.println("dflib CSV row count: " + csvRows);

            if (n != csvRows) {
                System.err.println("MISMATCH: parquet rows (" + n + ") != csv rows (" + csvRows + ")");
                System.exit(1);
            }
            System.out.println("OK: parquet read-back rows match the CSV catalog.");
        }
    }

    static String safeStr(RowReader rr, String col) {
        try {
            return rr.getString(col);
        } catch (Exception e) {
            return null;
        }
    }
}