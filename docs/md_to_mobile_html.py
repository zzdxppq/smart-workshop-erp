"""Convert mobile-tech-stack.md to styled HTML (offline, single-file)."""
import re
import sys
import html
from pathlib import Path
import markdown
from markdown.extensions.toc import TocExtension
from markdown.extensions.fenced_code import FencedCodeExtension
from markdown.extensions.tables import TableExtension
from markdown.extensions.codehilite import CodeHiliteExtension

SRC = Path(r"E:\claude\smart-workshop-erp\docs\mobile-tech-stack.md")
DST = Path(r"E:\claude\smart-workshop-erp\docs\mobile-tech-stack.html")

CSS = r"""
:root{
  --c-bg:#ffffff;--c-bg-soft:#f7f8fa;--c-bg-code:#0d1117;--c-fg:#1f2328;--c-fg-soft:#57606a;
  --c-border:#d0d7de;--c-border-soft:#eaeef2;--c-accent:#0969da;--c-accent-soft:#ddf4ff;
  --c-success:#1a7f37;--c-warn:#9a6700;--c-danger:#cf222e;--c-tag:#8250df;
  --c-paper:#f6f8fa;
  --c-android:#3ddc84;  /* Android green */
  --c-ios:#007aff;      /* iOS blue */

  --font-sans:-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC","Hiragino Sans GB",
              "Microsoft YaHei","Helvetica Neue",Arial,sans-serif;
  --font-mono:"JetBrains Mono","SF Mono",Menlo,Consolas,"Courier New",monospace;
  --shadow:0 1px 3px rgba(31,35,40,.12);
}
*{box-sizing:border-box;margin:0;padding:0}
html{scroll-behavior:smooth}
body{margin:0;font-family:var(--font-sans);color:var(--c-fg);background:var(--c-bg);
     line-height:1.7;font-size:15px;-webkit-font-smoothing:antialiased}
a{color:var(--c-accent);text-decoration:none}
a:hover{text-decoration:underline}
code{font-family:var(--font-mono);font-size:90%;background:var(--c-bg-soft);
     padding:2px 6px;border-radius:4px;color:#cf222e}
pre code{background:transparent;color:#e6edf3;padding:0;font-size:13px}
pre{background:var(--c-bg-code);color:#e6edf3;padding:16px 18px;border-radius:8px;
    overflow-x:auto;line-height:1.5;box-shadow:var(--shadow);margin:14px 0}
table{border-collapse:collapse;width:100%;margin:14px 0;font-size:14px}
th,td{border:1px solid var(--c-border);padding:8px 12px;text-align:left;vertical-align:top}
th{background:var(--c-bg-soft);font-weight:600}
tr:nth-child(even) td{background:#fafbfc}
blockquote{margin:14px 0;padding:10px 16px;border-left:4px solid var(--c-accent);
           background:var(--c-accent-soft);color:var(--c-fg-soft);border-radius:0 4px 4px 0}
hr{border:0;border-top:1px solid var(--c-border-soft);margin:32px 0}
h1,h2,h3,h4,h5,h6{font-weight:600;line-height:1.3;margin-top:28px;margin-bottom:12px}
h1{font-size:30px;padding-bottom:10px;border-bottom:2px solid var(--c-border)}
h2{font-size:24px;padding-bottom:8px;border-bottom:1px solid var(--c-border-soft)}
h3{font-size:20px}
h4{font-size:17px;color:var(--c-fg-soft)}
ul,ol{padding-left:24px;margin:10px 0}
li{margin:4px 0}

/* Layout */
.layout{display:grid;grid-template-columns:280px 1fr;min-height:100vh}
aside.toc{position:sticky;top:0;align-self:start;height:100vh;overflow-y:auto;
         background:var(--c-bg-soft);border-right:1px solid var(--c-border-soft);
         padding:24px 20px;font-size:13px}
aside.toc h2{font-size:14px;margin:0 0 12px 0;padding:0;border:0;color:var(--c-fg-soft);
             text-transform:uppercase;letter-spacing:.5px}
aside.toc ul{list-style:none;padding:0;margin:0}
aside.toc li{margin:2px 0}
aside.toc a{color:var(--c-fg-soft);display:block;padding:3px 8px;border-radius:4px;line-height:1.4}
aside.toc a:hover{background:var(--c-accent-soft);color:var(--c-accent);text-decoration:none}
aside.toc .lvl-3{padding-left:20px;font-size:12px}
main.content{max-width:980px;padding:32px 48px 96px;margin:0 auto;width:100%}

.doc-header{padding:24px 32px;background:linear-gradient(135deg,#3ddc84 0%,#007aff 100%);
            color:#fff;border-radius:12px;margin-bottom:24px;box-shadow:var(--shadow)}
.doc-header h1{margin:0 0 8px 0;padding:0;border:0;color:#fff;font-size:28px}
.doc-header .meta{display:flex;flex-wrap:wrap;gap:18px;font-size:13px;opacity:.92}
.doc-header .meta span{display:inline-flex;align-items:center;gap:6px}

/* 平台标签 */
.pill{display:inline-block;font-size:12px;padding:2px 9px;border-radius:10px;
      font-weight:600;letter-spacing:.2px;line-height:18px}
.pill-android{background:#d4f5e0;color:#0a6b3a;border:1px solid #95e0bd}
.pill-ios{background:#d4e8ff;color:#003a7a;border:1px solid #99c2f0}
.pill-p0{background:#ffe0e0;color:#a40000;border-color:#ffb3b3}
.pill-p1{background:#fff4d6;color:#7a5800;border-color:#ffd97d}
.pill-p2{background:#e0eaff;color:#1e3a8a;border-color:#b3c7ff}
.pill-epic{background:#e6f4ff;color:#0a4a7a;border-color:#99d4ff}
.pill-risk-p0{background:#fde8e8;color:#8a1a1a;border-color:#f5b3b3}
.pill-risk-p1{background:#fff0d6;color:#7a4a00;border-color:#f0c674}
.pill-risk-p2{background:#e0f5ec;color:#0a6b3a;border-color:#95e0bd}
.pill-ok{background:#e0f5ec;color:#0a6b3a;border:1px solid #95e0bd}
.pill-warn{background:#fde8e8;color:#8a1a1a;border:1px solid #f5b3b3}
.pill-do{background:#fff0d6;color:#7a4a00;border:1px solid #f0c674}

/* Soul sticker */
.soul-sticker{background:linear-gradient(135deg,#fff5e6 0%,#ffe4b3 100%);
               border:1px solid #f0c674;border-left:6px solid #d97706;
               border-radius:8px;padding:18px 22px;margin:20px 0;box-shadow:var(--shadow)}
.soul-sticker .title{font-weight:700;font-size:18px;color:#7a4a00;margin-bottom:6px}
.soul-sticker .desc{color:#5a3500;line-height:1.7;font-size:14px}

/* Decision sticker (Android vs iOS comparison) */
.compare{background:linear-gradient(135deg,#d4f5e0 0%,#d4e8ff 100%);
         border:1px solid #95e0bd;border-left:6px solid var(--c-android);
         border-radius:8px;padding:18px 22px;margin:20px 0;box-shadow:var(--shadow)}
.compare .title{font-weight:700;font-size:16px;color:#0a4a3a;margin-bottom:8px}
.compare .desc{color:#0a3a2a;line-height:1.7;font-size:14px}

/* Q&A blockquote */
blockquote.qa{
  background:var(--c-bg-soft);
  border-left:4px solid var(--c-accent);
  color:var(--c-fg);
  font-size:14.5px;line-height:1.75;
  margin:14px 0;
}
blockquote.qa p{margin:2px 0}

/* 决策表格右侧状态 */
.status-ok{background:#d4f5e0;color:#0a6b3a;padding:2px 8px;border-radius:10px;font-size:12px;font-weight:600}
.status-no{background:#fde8e8;color:#8a1a1a;padding:2px 8px;border-radius:10px;font-size:12px;font-weight:600}
.status-pending{background:#fff0d6;color:#7a4a00;padding:2px 8px;border-radius:10px;font-size:12px;font-weight:600}

/* Back to top */
#back-to-top{position:fixed;bottom:32px;right:32px;width:44px;height:44px;
             background:var(--c-accent);color:#fff;border-radius:50%;display:none;
             align-items:center;justify-content:center;cursor:pointer;border:0;
             box-shadow:0 4px 12px rgba(9,105,218,.4);font-size:20px;z-index:99}
#back-to-top:hover{background:#0860c7}

@media print{
  aside.toc,#back-to-top{display:none}
  .layout{grid-template-columns:1fr}
  main.content{max-width:100%;padding:0}
  pre{background:#f5f5f5;color:#1f2328;box-shadow:none}
  a{color:#1f2328;text-decoration:underline}
}
@media (max-width:1024px){
  .layout{grid-template-columns:1fr}
  aside.toc{position:static;height:auto;border-right:0;border-bottom:1px solid var(--c-border-soft)}
  main.content{padding:20px}
}
"""

JS = r"""
(function(){
  var toc = document.getElementById('toc-list');
  var headings = document.querySelectorAll('main.content h2, main.content h3, main.content h4');
  headings.forEach(function(h){
    if(!h.id){
      h.id = h.textContent.trim()
        .toLowerCase()
        .replace(/[\s\W\-]+/g,'-')
        .replace(/^-+|-+$/g,'')
        .substring(0,80);
    }
    var li = document.createElement('li');
    li.className = 'lvl-' + h.tagName.charAt(1);
    var a = document.createElement('a');
    a.href = '#' + h.id;
    a.textContent = h.textContent.trim();
    li.appendChild(a);
    toc.appendChild(li);
  });
  var btn = document.getElementById('back-to-top');
  window.addEventListener('scroll', function(){
    btn.style.display = window.scrollY > 400 ? 'flex' : 'none';
  });
  btn.addEventListener('click', function(){ window.scrollTo({top:0, behavior:'smooth'}); });
  var links = toc.querySelectorAll('a');
  function highlight(){
    var fromTop = window.scrollY + 80;
    var current = null;
    headings.forEach(function(h){ if(h.offsetTop <= fromTop) current = h; });
    links.forEach(function(a){
      a.style.background = ''; a.style.color = '';
      if(current && a.getAttribute('href') === '#' + current.id){
        a.style.background = 'var(--c-accent-soft)';
        a.style.color = 'var(--c-accent)'; a.style.fontWeight = '600';
      }
    });
  }
  window.addEventListener('scroll', highlight); highlight();
  document.querySelectorAll('pre > code').forEach(function(c){
    var m = (c.className||'').match(/language-(\w+)/);
    if(m){ c.parentElement.setAttribute('data-lang', m[1]); }
  });
})();
"""


def preprocess(md_text: str) -> str:
    # 平台标签
    md_text = re.sub(r"\bAndroid\s+V1\.0\b", '<span class="pill pill-android">Android V1.0</span>', md_text)
    md_text = re.sub(r"\bAndroid\b(?!\s*[Vv])", r'<span class="pill pill-android">Android</span>', md_text)
    md_text = re.sub(r"\biOS\s+V1\.1\b", '<span class="pill pill-ios">iOS V1.1</span>', md_text)
    md_text = re.sub(r"\biOS\s+V1\.0\b", '<span class="pill pill-ios">iOS V1.0</span>', md_text)
    md_text = re.sub(r"\biOS\b(?!\s*[Vv])", r'<span class="pill pill-ios">iOS</span>', md_text)

    # 优先级
    md_text = re.sub(r"\bP0\b", r'<span class="pill pill-p0">P0</span>', md_text)
    md_text = re.sub(r"\bP1\b", r'<span class="pill pill-p1">P1</span>', md_text)
    md_text = re.sub(r"\bP2\b", r'<span class="pill pill-p2">P2</span>', md_text)
    md_text = re.sub(r"\bE[1-9]\b", r'<span class="pill pill-epic">\g<0></span>', md_text)

    # 风险等级
    md_text = re.sub(r"🔴\s*P0\b", r'<span class="pill pill-risk-p0">P0</span>', md_text)
    md_text = re.sub(r"🟡\s*P1\b", r'<span class="pill pill-risk-p1">P1</span>', md_text)
    md_text = re.sub(r"🟢\s*P2\b", r'<span class="pill pill-risk-p2">P2</span>', md_text)

    # 决策表状态
    md_text = re.sub(r"✅\s*\*\*(.+?)\*\*", r'<span class="status-ok">✅ \1</span>', md_text)
    md_text = re.sub(r"❌\s*\*\*(.+?)\*\*", r'<span class="status-no">❌ \1</span>', md_text)
    md_text = re.sub(r"🟡\s*\*\*(.+?)\*\*", r'<span class="status-pending">🟡 \1</span>', md_text)

    # 灵魂自评（"灵魂一致性自评" 章节）
    # 不做特殊处理，保持表格原样
    return md_text


def main():
    md = SRC.read_text(encoding="utf-8")
    md = preprocess(md)
    md_engine = markdown.Markdown(
        extensions=[
            "extra", TocExtension(toc_depth="2-3", anchorlink=False),
            FencedCodeExtension(), TableExtension(),
            CodeHiliteExtension(noclasses=True, guess_lang=False),
            "sane_lists", "nl2br",
        ],
        output_format="html5",
    )
    body = md_engine.convert(md)
    title = "移动端技术选型 · Smart Workshop ERP"
    page = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<title>{html.escape(title)}</title>
<style>{CSS}</style>
</head>
<body>
<div class="layout">
  <aside class="toc">
    <h2>目录 · Outline</h2>
    <ul id="toc-list"></ul>
    <p style="margin-top:24px;font-size:11px;color:var(--c-fg-soft);line-height:1.5">
      Android V1.0 主战场<br>
      iOS V1.1 二期预览<br>
      18 章 · 与客户开发交流
    </p>
  </aside>
  <main class="content">
    {body}
  </main>
</div>
<button id="back-to-top" title="返回顶部">↑</button>
<script>{JS}</script>
</body>
</html>
"""
    DST.write_text(page, encoding="utf-8")
    sys.stdout.reconfigure(encoding="utf-8")
    print(f"[OK] Mobile tech stack HTML: {DST}")
    print(f"     Source: {SRC} ({len(md):,} chars)")
    print(f"     Target: {DST} ({DST.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
