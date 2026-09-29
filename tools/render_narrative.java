///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.commonmark:commonmark:0.24.0
//DEPS org.commonmark:commonmark-ext-gfm-tables:0.24.0

// Render the Marp-style narrative markdown into ONE self-contained HTML deck
// with navigation: prev/next, a "back to overview" button and a contents menu.
//
// Each narrative/*.md is split on `---`, rendered with CommonMark in file order,
// and concatenated into narrative/web/index.html. The `title:` in a file's
// front matter becomes its entry in the contents menu.
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
import java.util.ArrayList;
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
    static final Pattern FM_TITLE = Pattern.compile("(?m)^title:\\s*(.*)$");

    record Section(String title, int start) {}

    public static void main(String[] args) throws IOException {
        List<org.commonmark.Extension> extensions = List.of(TablesExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();

        Files.createDirectories(OUT);
        StringBuilder slides = new StringBuilder();
        List<Section> sections = new ArrayList<>();
        String deckTitle = null;
        int count = 0;

        try (Stream<Path> files = Files.list(SRC)) {
            for (Path src : files.filter(p -> p.toString().endsWith(".md"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList()) {
                String name = src.getFileName().toString();
                if (name.startsWith("_")) continue;

                String text = Files.readString(src, StandardCharsets.UTF_8);
                if (deckTitle == null) deckTitle = firstHeading(text);
                sections.add(new Section(sectionTitle(text, name), count));

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

        String js = JS.replace("{{SECTIONS}}", sectionsJson(sections));
        String page = TEMPLATE
                .replace("{{TITLE}}", escape(deckTitle == null ? "devoxx-hol-2026" : deckTitle))
                .replace("{{CSS}}", CSS)
                .replace("{{SLIDES}}", slides.toString())
                .replace("{{JS}}", js);
        Files.writeString(OUT.resolve("index.html"), page, StandardCharsets.UTF_8);
        System.out.println("wrote narrative/web/index.html  (" + count + " slides)");
        for (Section s : sections) {
            System.out.println("  section  " + String.format("%3d", s.start() + 1) + "  " + s.title());
        }

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

    static String sectionTitle(String text, String fallback) {
        Matcher fm = FRONT_MATTER.matcher(text);
        if (fm.find()) {
            Matcher t = FM_TITLE.matcher(fm.group());
            if (t.find()) return t.group(1).strip();
        }
        String heading = firstHeading(text);
        return heading == null ? fallback : heading;
    }

    static String firstHeading(String text) {
        Matcher m = TITLE.matcher(FRONT_MATTER.matcher(text).replaceFirst(""));
        return m.find() ? m.group(1).strip() : null;
    }

    static String sectionsJson(List<Section> sections) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"t\":\"").append(jsEscape(s.title())).append("\",\"i\":").append(s.start()).append('}');
        }
        return sb.append(']').toString();
    }

    static String jsEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("<", "\\u003c");
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
            #nav { position:fixed; right:1rem; bottom:.8rem; display:flex; gap:.4rem; align-items:center; }
            #nav #sec { color:var(--dim); font-size:.85rem; margin-right:.35rem; }
            #nav button { background:#1a222d; color:var(--fg); border:1px solid var(--rule); border-radius:8px;
              padding:.35rem .62rem; cursor:pointer; font-size:.95rem; line-height:1; }
            #nav button:hover { border-color:var(--accent); color:#fff; }
            #toc { position:fixed; inset:0; background:rgba(5,8,12,.74); display:none;
              align-items:center; justify-content:center; z-index:10; }
            #toc.open { display:flex; }
            #toc .box { background:#131922; border:1px solid var(--rule); border-radius:14px;
              padding:1.1rem 1.6rem 1.4rem; min-width:min(90vw,540px); max-height:82vh; overflow:auto; }
            #toc h2 { margin:.2rem 0 .8rem; color:var(--accent); border:none; }
            #toc ol { margin:0; padding-left:1.3rem; }
            #toc li { margin:.4rem 0; }
            #toc a { color:var(--fg); text-decoration:none; cursor:pointer; font-size:1.12rem; }
            #toc a:hover { color:var(--accent); }
            .slide .grid { display:grid; gap:.8rem; margin:.9rem 0; }
            .slide .grid.two { grid-template-columns:1fr 1fr; }
            .slide .grid.three { grid-template-columns:repeat(3,1fr); }
            .slide .card { background:#0f1620; border:1px solid var(--rule);
              border-radius:12px; padding:1rem 1.1rem; }
            .slide .card .n { display:block; font-size:1.9rem; font-weight:700; color:var(--accent); line-height:1.1; }
            .slide .card .l { display:block; color:var(--dim); font-size:1rem; margin-top:.3rem; }
            .slide .card .w { display:block; color:#6f8199; font-size:.8rem; margin-top:.45rem;
              text-transform:uppercase; letter-spacing:.09em; }
            .slide .tile { background:#0f1620; border:1px solid var(--rule); border-radius:12px; padding:.85rem 1rem; }
            .slide .tile b { display:block; color:var(--accent); font-size:1.05rem; margin-bottom:.25rem; }
            .slide .tile .d { color:var(--dim); font-size:.95rem; }
            .slide a { color:var(--accent); text-decoration:none; }
            .slide a:hover { text-decoration:underline; }
            .slide .note { color:var(--dim); font-size:1.05rem; margin-top:.9rem; }
            .slide .bars { display:flex; flex-direction:column; gap:.45rem; }
            .slide .bar { display:flex; align-items:center; gap:.6rem; }
            .slide .bar .lab { width:4.6rem; color:var(--dim); font-size:.95rem; }
            .slide .bar .track { flex:1; background:#0b0f14; border:1px solid var(--rule);
              border-radius:6px; overflow:hidden; height:1.25rem; }
            .slide .bar .fill { display:block; height:100%; min-width:3px;
              background:linear-gradient(90deg,#2e6ea5,#4aa3df); }
            .slide .bar .fill.warm { background:linear-gradient(90deg,#a5642e,#df9a4a); }
            .slide .bar .val { width:4.5rem; text-align:right; font-size:.95rem; font-variant-numeric:tabular-nums; }
            .slide .tool { display:flex; align-items:center; gap:.85rem; background:#0f1620;
              border:1px solid var(--rule); border-radius:12px; padding:.7rem .95rem; }
            .slide .tool .ico { flex:0 0 auto; width:30px; height:30px; color:var(--accent); }
            .slide .tool .ico svg { width:30px; height:30px; display:block; }
            .slide .tool .txt b { display:block; color:var(--fg); font-size:1.02rem; }
            .slide .tool .txt span { color:var(--dim); font-size:.88rem; }
            .slide .sep { display:flex; align-items:center; gap:.8rem; margin:.9rem 0 .7rem;
              color:var(--dim); font-size:.9rem; }
            .slide .sep::before, .slide .sep::after { content:""; height:1px; background:var(--rule); flex:1; }
            .slide .alt .tool { border-color:#6d5bd0; background:#141129; }
            .slide .alt .tool .ico { color:#a892ff; }
            .slide .badge { margin-left:auto; font-size:.72rem; text-transform:uppercase; letter-spacing:.09em;
              color:#b3a3ff; border:1px solid #3a2f66; border-radius:999px; padding:.15rem .55rem; white-space:nowrap; }
            @media print {
              .slide { display:block !important; page-break-after:always; box-shadow:none; border:none; }
              #nav, #bar, #toc { display:none; }
            }
            """;

    static final String JS = """
            const sections = {{SECTIONS}};
            const slides = [...document.querySelectorAll('.slide')];
            const toc = document.getElementById('toc');
            const secEl = document.getElementById('sec');
            let i = 0;
            function sectionOf(n){ let s = 0; for (let k = 0; k < sections.length; k++) if (sections[k].i <= n) s = k; return s; }
            function show(n){
              i = Math.max(0, Math.min(slides.length - 1, n));
              slides.forEach((s, k) => s.classList.toggle('active', k === i));
              document.getElementById('bar').style.width = ((i + 1) / slides.length * 100) + '%';
              secEl.textContent = sections[sectionOf(i)].t + '  ·  ' + (i + 1) + '/' + slides.length;
              history.replaceState(null, '', '#' + (i + 1));
            }
            function toggleToc(){ toc.classList.toggle('open'); }
            const list = document.getElementById('tocList');
            sections.forEach(s => {
              const li = document.createElement('li');
              const a = document.createElement('a');
              a.textContent = s.t;
              a.onclick = () => { show(s.i); toc.classList.remove('open'); };
              li.appendChild(a); list.appendChild(li);
            });
            document.getElementById('prev').onclick = () => show(i - 1);
            document.getElementById('next').onclick = () => show(i + 1);
            document.getElementById('home').onclick = () => show(0);
            document.getElementById('contents').onclick = toggleToc;
            toc.onclick = e => { if (e.target === toc) toc.classList.remove('open'); };
            addEventListener('keydown', e => {
              if (['ArrowRight','ArrowDown',' ','PageDown','Enter'].includes(e.key)) { e.preventDefault(); show(i + 1); }
              if (['ArrowLeft','ArrowUp','PageUp','Backspace'].includes(e.key)) { e.preventDefault(); show(i - 1); }
              if (e.key === 'Home') show(0);
              if (e.key === 'End') show(slides.length - 1);
              if (e.key === 'o' || e.key === 'O') show(0);
              if (e.key === 'c' || e.key === 'C') toggleToc();
              if (e.key === 'Escape') toc.classList.remove('open');
            });
            addEventListener('click', e => {
              if (e.target.closest('#nav') || e.target.closest('#toc')) return;
              show(e.clientX < innerWidth * 0.28 ? i - 1 : i + 1);
            });
            const start = parseInt(location.hash.slice(1), 10);
            show(isNaN(start) ? 0 : start - 1);
            """;

    static final String TEMPLATE = """
            <!doctype html>
            <html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>{{TITLE}}</title><style>{{CSS}}</style></head>
            <body>
            <div id="deck">{{SLIDES}}</div>
            <div id="bar"></div>
            <nav id="nav">
              <span id="sec"></span>
              <button id="home" title="back to the overview (o)">&#8962; overview</button>
              <button id="prev" title="previous (left arrow)">&#8249; prev</button>
              <button id="next" title="next (right arrow)">next &#8250;</button>
              <button id="contents" title="contents (c)">&#9776; contents</button>
            </nav>
            <div id="toc"><div class="box"><h2>Contents</h2><ol id="tocList"></ol></div></div>
            <script>{{JS}}</script></body></html>
            """;
}
