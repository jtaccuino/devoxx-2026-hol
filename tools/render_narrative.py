#!/usr/bin/env python3
"""Render the Marp-style narrative markdown to self-contained HTML decks.

No Marp, no network: split on `---`, render each slide with python-markdown, and
wrap the result in a small keyboard-navigable deck. Run from the repo root:

    python3 tools/render_narrative.py
"""
import glob
import html
import os
import re

import markdown

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "narrative")
OUT = os.path.join(SRC, "web")

FRONT_MATTER = re.compile(r"\A---\s*\n.*?\n---\s*\n", re.S)
LEAD = re.compile(r"<!--\s*_class:\s*lead\s*-->")
COMMENT = re.compile(r"<!--.*?-->", re.S)

MD = ["tables", "fenced_code", "sane_lists", "attr_list"]

CSS = """
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
#back { position:fixed; left:1rem; top:.8rem; color:var(--dim); font-size:.85rem; text-decoration:none; }
#back:hover { color:var(--accent); }
@media print {
  .slide { display:block !important; page-break-after:always; box-shadow:none; border:none; }
  #back, #hud, #bar { display:none; }
}
"""

JS = """
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
"""

TEMPLATE = """<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title><style>{css}</style></head>
<body><a id="back" href="index.html">&#8592; all decks</a>
<div id="deck">{slides}</div><div id="bar"></div><div id="hud"></div>
<script>{js}</script></body></html>
"""


def slides_of(text):
    text = FRONT_MATTER.sub("", text)
    parts = re.split(r"(?m)^---\s*$", text)
    out = []
    for part in parts:
        part = part.strip("\n")
        if not part.strip():
            continue
        lead = bool(LEAD.search(part))
        body = COMMENT.sub("", part)
        out.append((lead, markdown.markdown(body, extensions=MD)))
    return out


def title_of(text):
    m = re.search(r"(?m)^#\s+(.*)$", FRONT_MATTER.sub("", text))
    return m.group(1).strip() if m else "deck"


def main():
    os.makedirs(OUT, exist_ok=True)
    decks = []
    for src in sorted(glob.glob(os.path.join(SRC, "*.md"))):
        name = os.path.basename(src)
        if name.startswith("_"):
            continue
        text = open(src, encoding="utf-8").read()
        out_name = os.path.splitext(name)[0] + ".html"
        slides = slides_of(text)
        body = "\n".join(
            f'<section class="slide{" lead" if lead else ""}">{htm}</section>'
            for lead, htm in slides)
        page = TEMPLATE.format(title=html.escape(title_of(text)), css=CSS,
                               slides=body, js=JS)
        with open(os.path.join(OUT, out_name), "w", encoding="utf-8") as f:
            f.write(page)
        decks.append((out_name, title_of(text), len(slides)))
        print(f"wrote narrative/web/{out_name}  ({len(slides)} slides)")

    items = "\n".join(
        f'<a class="card" href="{n}"><span class="t">{html.escape(t)}</span>'
        f'<span class="n">{c} slides</span></a>' for n, t, c in decks)
    index = f"""<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>devoxx-hol-2026 · narrative</title><style>{CSS}
#deck{{flex-direction:column}} .card{{display:flex;justify-content:space-between;align-items:center;
width:min(94vw,760px);margin:.5rem 0;padding:1.1rem 1.4rem;background:#131922;
border:1px solid var(--rule);border-radius:12px;text-decoration:none;color:var(--fg);}}
.card:hover{{border-color:var(--accent)}} .t{{font-size:1.25rem}} .n{{color:var(--dim)}}
.hero{{width:min(94vw,760px);margin-bottom:1rem}} </style></head>
<body><div id="deck"><div class="hero"><h1>Java is for Data Science, Too</h1>
<p style="color:var(--dim)">Devoxx Belgium 2026 — hands-on lab narrative.</p></div>
{items}</div></body></html>"""
    with open(os.path.join(OUT, "index.html"), "w", encoding="utf-8") as f:
        f.write(index)
    print("wrote narrative/web/index.html")


if __name__ == "__main__":
    main()
