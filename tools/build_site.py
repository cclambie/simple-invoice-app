#!/usr/bin/env python3
"""Builds the Lambie Labs pages in website/ from content/.

content/pages/**/index.html  HTML fragments with front matter -> website/<same path>
content/writing/*.md         articles with front matter -> website/writing/<slug>/,
                             plus the writing index, one page per tag, and an Atom feed

Front matter is YAML between --- lines. Pages: title, description. Articles:

  title: Why the Fediverse matters
  date: 2026-10-14
  summary: One or two sentences for lists, the feed and link previews.
  tags: [fediverse, data-sovereignty]
  draft: true            # left out unless --drafts is passed
  mastodon: https://fosstodon.org/@craiglambie42/123  # the post that collects replies
  canonical: https://...  # only when the original lives somewhere else
  syndication: [https://craiglambie.com/...]           # copies elsewhere (POSSE)

Usage: tools/build_site.py [--drafts] [--out DIR]
The invoice app pages and the maps under work/projects/*/ are hand-made and left alone.
"""
import argparse
import datetime as dt
import html
import re
import shutil
from urllib.parse import quote
import sys
from pathlib import Path
from xml.sax.saxutils import escape as xml_escape

import markdown
import yaml

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "content"
SITE_URL = "https://labs.craiglambie.com"
SITE_NAME = "Lambie Labs"
AUTHOR = "Craig Lambie"
EMAIL = "labs@craiglambie.com"
MASTODON = "https://fosstodon.org/@craiglambie42"
MASTODON_HANDLE = "@craiglambie42@fosstodon.org"
GITHUB = "https://github.com/cclambie"

TAG_LABELS = {
    "fediverse": "Fediverse", "mastodon": "Mastodon", "activitypub": "ActivityPub",
    "data-sovereignty": "Data sovereignty", "democracy": "Democracy", "australia": "Australia",
    "journalism": "Journalism", "open-source": "Open source", "politicians-social": "Politicians.social",
    "government": "Government", "social-enterprise": "Social enterprise",
}


def tag_label(tag):
    return TAG_LABELS.get(tag, tag.replace("-", " ").capitalize())


def read_front_matter(path):
    text = path.read_text(encoding="utf-8")
    m = re.match(r"---\n(.*?)\n---\n(.*)", text, re.S)
    if not m:
        sys.exit(f"{path}: missing front matter")
    return yaml.safe_load(m.group(1)) or {}, m.group(2)


def page(title, description, path, body, *, head_extra="", og_type="website", full_title=None):
    url = SITE_URL + path
    t = full_title or (SITE_NAME if title == SITE_NAME else f"{title} · {SITE_NAME}")
    e = html.escape
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>{e(t)}</title>
  <meta name="description" content="{e(description)}">
  <link rel="canonical" href="{e(url)}">
  <meta name="theme-color" content="#f97316">
  <meta property="og:type" content="{og_type}">
  <meta property="og:site_name" content="{SITE_NAME}">
  <meta property="og:title" content="{e(title)}">
  <meta property="og:description" content="{e(description)}">
  <meta property="og:url" content="{e(url)}">
  <meta name="fediverse:creator" content="{MASTODON_HANDLE}">
  <link rel="me" href="{MASTODON}">
  <link rel="alternate" type="application/atom+xml" title="{SITE_NAME} writing" href="/writing/feed.xml">
  <link rel="icon" href="/assets/favicon-32.png" sizes="32x32">
  <link rel="stylesheet" href="/assets/site.css">
  <script src="/assets/config.js"></script>
  <script src="/assets/site.js" defer></script>
{head_extra}</head>
<body>
  <header class="site-header">
    <div class="wrap">
      <a class="brand" href="/">{SITE_NAME}</a>
      <nav aria-label="Main">
        <a href="/work/">Work</a>
        <a href="/writing/">Writing</a>
        <a href="/#elsewhere">Elsewhere</a>
      </nav>
    </div>
  </header>

  <main class="wrap doc">
{body}
  </main>

  <footer class="site-footer">
    <div class="wrap">
      <span>© {dt.date.today().year} {AUTHOR} · {SITE_NAME}</span>
      <span><a href="{MASTODON}" rel="me">Mastodon</a> · <a href="{GITHUB}" rel="me">GitHub</a> · <a href="/writing/feed.xml">Feed</a> · <a href="mailto:{EMAIL}">{EMAIL}</a></span>
    </div>
  </footer>
</body>
</html>
"""


def write(out, path, text):
    target = out / path.lstrip("/") / "index.html" if path.endswith("/") else out / path.lstrip("/")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8")


def host(url):
    return re.sub(r"^https?://(www\.)?", "", url).split("/")[0]


def long_date(d):
    return f"{d.day} {d:%B %Y}"


def tag_links(tags):
    return " ".join(f'<a class="tag" href="/writing/tags/{t}/" rel="tag">{html.escape(tag_label(t))}</a>' for t in tags)


def article_list(articles):
    if not articles:
        return "    <p>The first pieces are on the way.</p>"
    items = []
    for a in articles:
        items.append(f"""    <li class="post">
      <a class="post-title" href="{a['path']}">{html.escape(a['title'])}</a>
      <p class="post-meta"><time datetime="{a['date'].isoformat()}">{long_date(a['date'])}</time>{' · Draft' if a['draft'] else ''}</p>
      <p>{html.escape(a['summary'])}</p>
      <p class="tags">{tag_links(a['tags'])}</p>
    </li>""")
    return '    <ul class="posts">\n' + "\n".join(items) + "\n    </ul>"


def load_articles(include_drafts):
    md = markdown.Markdown(extensions=["extra", "sane_lists", "smarty"])
    articles = []
    for p in sorted((CONTENT / "writing").glob("*.md")):
        meta, body = read_front_matter(p)
        draft = bool(meta.get("draft"))
        if draft and not include_drafts:
            continue
        slug = meta.get("slug") or p.stem
        d = meta["date"]
        if isinstance(d, str):
            d = dt.date.fromisoformat(d)
        md.reset()
        articles.append({
            "slug": slug, "path": f"/writing/{slug}/", "title": meta["title"], "date": d,
            "summary": meta.get("summary", ""), "tags": meta.get("tags", []), "draft": draft,
            "mastodon": meta.get("mastodon"), "canonical": meta.get("canonical"),
            "syndication": meta.get("syndication", []), "html": md.convert(body),
            "order": meta.get("order", 0),
        })
    articles.sort(key=lambda a: (a["date"], a["order"]), reverse=True)
    return articles


def render_article(a):
    e = html.escape
    url = SITE_URL + a["path"]
    head = ""
    if a["draft"]:
        head += '  <meta name="robots" content="noindex">\n'
    head += f'  <meta property="article:published_time" content="{a["date"].isoformat()}">\n'
    for t in a["tags"]:
        head += f'  <meta property="article:tag" content="{e(tag_label(t))}">\n'
    head += '  <script src="/assets/article.js" defer></script>\n'
    syndicated = ""
    copies = [a["mastodon"]] if a["mastodon"] else []
    copies += a["syndication"]
    if copies:
        links = ", ".join(f'<a class="u-syndication" href="{e(u)}">{e(host(u))}</a>' for u in copies)
        syndicated = f'<p class="post-meta">Also posted to {links}</p>'
    comments = ""
    if a["mastodon"]:
        comments = f"""
    <section class="comments" id="comments" data-mastodon="{e(a['mastodon'])}">
      <h2>Comments</h2>
      <p class="reactions" hidden><span data-count="favourites">0</span> likes · <span data-count="reblogs">0</span> boosts · <span data-count="replies">0</span> replies</p>
      <p>Comments are replies to <a href="{e(a['mastodon'])}">this post on Mastodon</a>. Reply, like or boost it from any
        Fediverse account: open the post, or paste its address into your own server's search box.</p>
      <ol class="comment-list"></ol>
      <p class="comment-status">Loading comments…</p>
    </section>"""
    else:
        comments = """
    <section class="comments" id="comments">
      <h2>Comments</h2>
      <p>Comments open once this piece is posted to Mastodon.</p>
    </section>"""
    body = f"""    <article class="h-entry">
      <h1 class="p-name">{e(a['title'])}</h1>
      <p class="meta">By <a class="p-author h-card" href="/">{AUTHOR}</a> · <time class="dt-published" datetime="{a['date'].isoformat()}">{long_date(a['date'])}</time>{' · <strong>Draft</strong>' if a['draft'] else ''}</p>
      <p class="tags">{tag_links(a['tags'])}</p>
      <div class="e-content">
{a['html']}
      </div>
      <a class="u-url" href="{e(url)}" hidden></a>
      {syndicated}
      <div class="share" data-url="{e(url)}" data-title="{e(a['title'])}">
        <span>Share:</span>
        <button type="button" data-share="mastodon">Mastodon</button>
        <a data-share="bluesky" href="https://bsky.app/intent/compose?text={e(quote(a['title'] + ' ' + url))}" target="_blank" rel="noopener">Bluesky</a>
        <a data-share="linkedin" href="https://www.linkedin.com/sharing/share-offsite/?url={e(quote(url, safe=''))}" target="_blank" rel="noopener">LinkedIn</a>
        <a data-share="email" href="mailto:?subject={e(quote(a['title']))}&amp;body={e(quote(url, safe=''))}">Email</a>
        <button type="button" data-share="copy">Copy link</button>
      </div>
    </article>
{comments}"""
    # A canonical elsewhere replaces the one page() writes.
    text = page(a["title"], a["summary"], a["path"], body, head_extra=head, og_type="article")
    if a["canonical"]:
        text = text.replace(f'<link rel="canonical" href="{e(url)}">', f'<link rel="canonical" href="{e(a["canonical"])}">')
    return text


def atom_feed(articles):
    updated = max((a["date"] for a in articles), default=dt.date.today())
    entries = []
    for a in articles:
        url = SITE_URL + a["path"]
        cats = "".join(f'\n    <category term="{t}" label="{xml_escape(tag_label(t))}"/>' for t in a["tags"])
        entries.append(f"""  <entry>
    <title>{xml_escape(a['title'])}</title>
    <link href="{url}"/>
    <id>{url}</id>
    <published>{a['date'].isoformat()}T00:00:00Z</published>
    <updated>{a['date'].isoformat()}T00:00:00Z</updated>
    <summary>{xml_escape(a['summary'])}</summary>{cats}
    <content type="html">{xml_escape(a['html'])}</content>
  </entry>""")
    return f"""<?xml version="1.0" encoding="utf-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>{SITE_NAME}: writing</title>
  <subtitle>Data sovereignty, the Fediverse and open technology, by {AUTHOR}.</subtitle>
  <link href="{SITE_URL}/writing/"/>
  <link rel="self" href="{SITE_URL}/writing/feed.xml"/>
  <id>{SITE_URL}/writing/</id>
  <updated>{updated.isoformat()}T00:00:00Z</updated>
  <author><name>{AUTHOR}</name><uri>{SITE_URL}/</uri></author>
{chr(10).join(entries)}
</feed>
"""


def sitemap(out):
    urls = []
    for p in sorted(out.rglob("index.html")):
        text = p.read_text(encoding="utf-8", errors="replace")[:4000]
        if 'name="robots" content="noindex"' in text or 'http-equiv="refresh"' in text:
            continue
        rel = p.parent.relative_to(out).as_posix()
        urls.append(f"{SITE_URL}/" + (rel + "/" if rel != "." else ""))
    today = dt.date.today().isoformat()
    rows = "\n".join(f"  <url><loc>{u}</loc><lastmod>{today}</lastmod></url>" for u in urls)
    return f'<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n{rows}\n</urlset>\n'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--drafts", action="store_true", help="include draft articles (for previews)")
    ap.add_argument("--out", type=Path, default=ROOT / "website")
    args = ap.parse_args()
    out = args.out

    articles = load_articles(args.drafts)

    # Writing: rebuilt from scratch so removed articles and tags disappear.
    shutil.rmtree(out / "writing", ignore_errors=True)
    for a in articles:
        write(out, a["path"], render_article(a))
    write(out, "/writing/", page("Writing", "Writing on data sovereignty, the Fediverse and open technology by Craig Lambie.", "/writing/", f"""    <h1>Writing</h1>
    <p class="meta">Data sovereignty, the Fediverse and open technology. Follow along with the
      <a href="/writing/feed.xml">feed</a> or on Mastodon at <a href="{MASTODON}" rel="me">{MASTODON_HANDLE}</a>.</p>
    <p class="tags">{tag_links(sorted({t for a in articles for t in a['tags']}))}</p>
{article_list(articles)}"""))
    tags = sorted({t for a in articles for t in a["tags"]})
    for t in tags:
        tagged = [a for a in articles if t in a["tags"]]
        write(out, f"/writing/tags/{t}/", page(f"Writing tagged {tag_label(t)}", f"Writing about {tag_label(t)} by Craig Lambie.", f"/writing/tags/{t}/", f"""    <p class="post-meta"><a href="/writing/">Writing</a> › Tag</p>
    <h1>{html.escape(tag_label(t))}</h1>
{article_list(tagged)}"""))
    (out / "writing" / "feed.xml").write_text(atom_feed(articles), encoding="utf-8")

    # Pages, with the latest articles dropped into {{latest_writing}}.
    latest = article_list(articles[:3])
    for p in sorted((CONTENT / "pages").rglob("index.html")):
        meta, body = read_front_matter(p)
        rel = p.parent.relative_to(CONTENT / "pages").as_posix()
        path = "/" if rel == "." else f"/{rel}/"
        write(out, path, page(meta["title"], meta["description"], path, body.rstrip().replace("{{latest_writing}}", latest)))

    (out / "sitemap.xml").write_text(sitemap(out), encoding="utf-8")
    print(f"built {len(articles)} articles, {len(tags)} tags into {out}")


if __name__ == "__main__":
    main()
