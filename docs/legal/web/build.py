"""Rebuild the paste-ready legal pages in docs/legal/web from the Markdown sources.

Run from the repo root after editing either document (needs pandoc):
    python3 docs/legal/web/build.py docs/legal
"""
import subprocess, re, sys, os
os.chdir(sys.argv[1])
css = """.dyw-legal{max-width:720px;margin:0 auto;padding:24px 16px;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;font-size:17px;line-height:1.6;color:#1d2a35}
.dyw-legal h1{font-size:1.9em;line-height:1.25;margin:0 0 .4em}
.dyw-legal h2{font-size:1.3em;margin:1.8em 0 .5em}
.dyw-legal a{color:#0a72b8}
.dyw-legal table{border-collapse:collapse;width:100%;margin:1em 0;font-size:.95em}
.dyw-legal th,.dyw-legal td{border:1px solid #d5dde3;padding:8px 10px;text-align:left;vertical-align:top}
.dyw-legal th{background:#eef4f8}
.dyw-legal ul{padding-left:1.3em}"""
dark = "body{margin:0;background:#fff}@media (prefers-color-scheme:dark){body{background:#121a20}body .dyw-legal{color:#e6edf2}body .dyw-legal a{color:#6cc0f5}body .dyw-legal th,body .dyw-legal td{border-color:#3a4a56}body .dyw-legal th{background:#22313c}}"
for slug, src, title in (("privacy","privacy-policy.md","Privacy Policy"),("terms","terms-of-use.md","Terms of Use")):
    body = subprocess.run(["pandoc",src,"-f","gfm","-t","html5","--wrap=none"],capture_output=True,text=True,check=True).stdout
    body = re.sub(r' id="[^"]*"','',body)
    snippet = f'<div class="dyw-legal">\n<style>\n{css}\n</style>\n{body}</div>\n'
    open(f"web/{slug}-snippet.html","w").write(snippet)
    open(f"web/{slug}.html","w").write(f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Drink Your Water — {title}</title>
<meta name="description" content="{title} for the Drink Your Water apps for Android and iPhone.">
<style>{dark}</style>
</head>
<body>
{snippet}</body>
</html>
""")
