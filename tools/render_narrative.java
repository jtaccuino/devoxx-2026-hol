///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.commonmark:commonmark:0.24.0
//DEPS org.commonmark:commonmark-ext-gfm-tables:0.24.0

// Render the Marp-style narrative markdown into ONE self-contained HTML deck.
// No Marp, no network: every narrative/*.md is split on `---`, rendered with
// CommonMark in file order, and concatenated into narrative/web/index.html.
//
// Usage: jbang tools/render_narrative.java

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class render_narrative {

    static final Path SRC = Path.of("narrative");
    static final Path OUT = SRC.resolve("web");

    static final Pattern FRONT_MATTER = Pattern.compile("\\A---\\s*\\n.*?\\n---\\s*\\n", Pattern.DOTALL);
    static final Pattern SLIDE_SPLIT = Pattern.compile("(?m)^---\\s*$");
    static final Pattern LEAD = Pattern.compile("<!--\\s*_class:\\s*lead\\s*-->");
    static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    static final Pattern TITLE = Pattern.compile("(?m)^#\\s+(.*)$");

    public static void main(String[] args) throws IOException {
        List<org.commonmark.Extension> extensions = List.of(TablesExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();

        Files.createDirectories(OUT);
        StringBuilder slides = new StringBuilder();
        String title = null;
        int count = 0;

        try (Stream<Path> files = Files.list(SRC)) {
            for (Path src : files.filter(p -> p.toString().endsWith(".md"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList()) {
                String name = src.getFileName().toString();
                if (name.startsWith("_")) continue;

                String text = Files.readString(src, StandardCharsets.UTF_8);
                if (title == null) title = titleOf(text);
                String body = FRONT_MATTER.matcher(text).replaceFirst("");

                for (String part : SLIDE_SPLIT.split(body)) {
                    if (part.strip().isEmpty()) continue;
                    boolean lead = LEAD.matcher(part).find();
                    Node doc = parser.parse(COMMENT.matcher(part).replaceAll(""));
                    slides.append("<section class=\"slide").append(lead ? " lead" : "")
                          .append("\">").append(renderer.render(doc)).append("</section>\n");
                    count++;
                }
            }
        }

        String page = TEMPLATE
                .replace("{{TITLE}}", escape(title == null ? "devoxx-hol-2026" : title))
                .replace("{{CSS}}", CSS)
                .replace("{{SLIDES}}", slides.toString())
                .replace("{{JS}}", JS);
        Files.writeString(OUT.resolve("index.html"), page, StandardCharsets.UTF_8);
        System.out.println("wrote narrative/web/index.html  (" + count + " slides)");

        // drop any per-deck HTML left over from the earlier, multi-file layout
        try (Stream<Path> stale = Files.list(OUT)) {
            for (Path p : stale.filter(x -> x.toString().endsWith(".html"))
                    .filter(x -> !x.getFileName().toString().equals("index.html"))
                    .toList()) {
                Files.deleteIfExists(p);
                System.out.println("removed stale " + p);
            }
        }
    }

    static String titleOf(String text) {
        Matcher m = TITLE.matcher(FRONT_MATTER.matcher(text).replaceFirst(""));
        return m.find() ? m.group(1).strip() : "deck";
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static final String CSS = """
            :root { --bg:#0e1116; --fg:#e8edf3; --dim:#9fb0c3; --accent:#4aa3df; --rule:#243040; }
            * { box-sizing: border-box; }
            html, body { margin:0; height:100%; background:var(--bg); color:var(--fg);
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Inter, Roboto, Helvetica, Arial, sans-serif; }
            #deck { height:100%; display:flex; align-items:center; justify-content:center; }
            .slide { display:none; width:min(94vw, 1180px); aspect-ratio:16/9; padding:3.2rem 4rem;
              background:#131922; border:1px solid var(--rule); border-radius:14px;
              overflow:auto; box-shadow:0 10px 40px rgba(0,0,0,.45); }
            .slide.active { display:block; }
            .slide.lead { display:none; }
            .slide.lead.active { display:flex; flex-direction:column; align-items:center; justify-content:center; text-align:center; }
            .slide h1 { font-size:2.1rem; margin:0 0 .6rem; letter-spacing:-.01em; }
            .slide.lead h1 { font-size:3rem; }
            .slide h2 { font-size:1.55rem; color:var(--accent); border-bottom:1px solid var(--rule); padding-bottom:.35rem; }
            .slide h3 { font-size:1.2rem; color:var(--dim); font-weight:600; }
            .slide p, .slide li { font-size:1.22rem; line-height:1.55; }
            .slide ul, .slide ol { padding-left:1.3rem; }
            .slide strong { color:#fff; }
            .slide code { background:#0b0f14; border:1px solid var(--rule); border-radius:5px;
              padding:.08em .38em; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size:.92em; }
            .slide pre { background:#0b0f14; border:1px solid var(--rule); border-radius:9px;
              padding:1rem 1.15rem; overflow:auto; }
            .slide pre code { background:none; border:none; padding:0; font-size:1rem; line-height:1.45; }
            .slide table { border-collapse:collapse; width:100%; margin:.5rem 0; }
            .slide th, .slide td { border:1px solid var(--rule); padding:.45rem .7rem; text-align:left; font-size:1.05rem; }
            .slide th { background:#1a222d; color:var(--dim); }
            .slide blockquote { border-left:3px solid var(--accent); margin:.6rem 0; padding:.2rem 1rem;
              color:var(--dim); background:#10161e; border-radius:0 8px 8px 0; }
            #bar { position:fixed; left:0; bottom:0; height:3px; background:var(--accent); transition:width .18s; }
            #hud { position:fixed; right:1rem; bottom:.7rem; color:var(--dim); font-size:.85rem; }
            @media print {
              .slide { display:block !important; page-break-after:always; box-shadow:none; border:none; }
              #hud, #bar { display:none; }
            }
            """;

    static final String JS = """
            const slides=[...document.querySelectorAll('.slide')];
            let i=0;
            function show(n){ i=Math.max(0,Math.min(slides.length-1,n));
              slides.forEach((s,k)=>s.classList.toggle('active',k===i));
              document.getElementById('bar').style.width=((i+1)/slides.length*100)+'%';
              document.getElementById('hud').textContent=(i+1)+' / '+slides.length;
              history.replaceState(null,'','#'+(i+1)); }
            addEventListener('keydown',e=>{
              if(['ArrowRight','ArrowDown',' ','PageDown','Enter'].includes(e.key)){e.preventDefault();show(i+1);}
              if(['ArrowLeft','ArrowUp','PageUp','Backspace'].includes(e.key)){e.preventDefault();show(i-1);}
              if(e.key==='Home')show(0); if(e.key==='End')show(slides.length-1); });
            addEventListener('click',e=>{ show(e.clientX < innerWidth*0.28 ? i-1 : i+1); });
            const start=parseInt(location.hash.slice(1),10); show(isNaN(start)?0:start-1);
            """;

    static final String TEMPLATE = """
            <!doctype html>
            <html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>{{TITLE}}</title><style>{{CSS}}</style></head>
            <body><div id="deck">{{SLIDES}}</div><div id="bar"></div><div id="hud"></div>
            <script>{{JS}}</script></body></html>
            """;
}
