// Article pages: share buttons, and comments pulled from the article's Mastodon post.
// Replies, likes and boosts live on Mastodon; this page only reads them from the public API,
// so the site needs no server, database or comment account of its own.
(function () {
  var INSTANCE_KEY = "mastodon-instance";

  function store(key, value) {
    try { if (value === undefined) return localStorage.getItem(key); localStorage.setItem(key, value); } catch (e) { return null; }
  }

  // ---------- Share ----------
  var share = document.querySelector(".share");
  if (share) {
    var url = share.getAttribute("data-url");
    var title = share.getAttribute("data-title");

    share.querySelector('[data-share="mastodon"]').addEventListener("click", function () {
      var instance = window.prompt("Your Mastodon server (for example mastodon.social):", store(INSTANCE_KEY) || "");
      if (!instance) return;
      instance = instance.trim().replace(/^https?:\/\//, "").replace(/^@?[^@]*@/, "").replace(/\/.*$/, "");
      if (!/^[a-z0-9.-]+\.[a-z]{2,}$/i.test(instance)) { window.alert("That doesn't look like a server name."); return; }
      store(INSTANCE_KEY, instance);
      window.open("https://" + instance + "/share?text=" + encodeURIComponent(title + " " + url), "_blank", "noopener");
    });

    share.querySelector('[data-share="copy"]').addEventListener("click", function (e) {
      var btn = e.currentTarget;
      function done() { btn.textContent = "Copied"; setTimeout(function () { btn.textContent = "Copy link"; }, 2000); }
      if (navigator.clipboard) navigator.clipboard.writeText(url).then(done, function () { window.prompt("Copy this link:", url); });
      else window.prompt("Copy this link:", url);
    });

    // Phones: offer the system share sheet too.
    if (navigator.share) {
      var native = document.createElement("button");
      native.type = "button";
      native.textContent = "More…";
      native.addEventListener("click", function () { navigator.share({ title: title, url: url }).catch(function () {}); });
      share.appendChild(native);
    }
  }

  // ---------- Comments from Mastodon ----------
  var box = document.querySelector(".comments[data-mastodon]");
  if (!box) return;
  var status = box.querySelector(".comment-status");
  var list = box.querySelector(".comment-list");
  var m = box.getAttribute("data-mastodon").match(/^https:\/\/([^/]+)\/@[^/]+\/(\d+)/);
  if (!m) { status.textContent = "Comments are unavailable."; return; }
  var api = "https://" + m[1] + "/api/v1/statuses/" + m[2];

  // Mastodon sends post bodies as HTML. Keep only plain text, line breaks and safe links.
  function clean(htmlText) {
    var doc = new DOMParser().parseFromString(htmlText, "text/html");
    var out = document.createDocumentFragment();
    function walk(node, parent) {
      node.childNodes.forEach(function (n) {
        if (n.nodeType === 3) { parent.appendChild(document.createTextNode(n.textContent)); return; }
        if (n.nodeType !== 1) return;
        var tag = n.tagName.toLowerCase();
        if (tag === "br") { parent.appendChild(document.createElement("br")); return; }
        if (tag === "p") { var p = document.createElement("p"); walk(n, p); parent.appendChild(p); return; }
        if (tag === "a" && /^https?:\/\//.test(n.getAttribute("href") || "")) {
          var a = document.createElement("a");
          a.href = n.getAttribute("href");
          a.rel = "nofollow ugc noopener";
          a.target = "_blank";
          walk(n, a);
          parent.appendChild(a);
          return;
        }
        if (n.classList && n.classList.contains("invisible")) return; // Mastodon's hidden URL parts
        walk(n, parent);
      });
    }
    walk(doc.body, out);
    return out;
  }

  function text(tag, cls, value) {
    var el = document.createElement(tag);
    if (cls) el.className = cls;
    el.textContent = value;
    return el;
  }

  function render(post, depth) {
    var li = document.createElement("li");
    li.className = "comment";
    li.style.setProperty("--depth", Math.min(depth, 4));
    var head = document.createElement("p");
    head.className = "comment-head";
    var who = document.createElement("a");
    who.href = post.account.url;
    who.rel = "nofollow ugc noopener";
    who.target = "_blank";
    who.textContent = post.account.display_name || post.account.username;
    head.appendChild(who);
    head.appendChild(text("span", "comment-handle", " @" + post.account.acct));
    var when = document.createElement("a");
    when.href = post.url;
    when.className = "comment-date";
    when.rel = "nofollow ugc noopener";
    when.target = "_blank";
    when.textContent = new Date(post.created_at).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
    head.appendChild(when);
    li.appendChild(head);
    var body = document.createElement("div");
    body.className = "comment-body";
    if (post.spoiler_text) body.appendChild(text("p", "comment-cw", "Content warning: " + post.spoiler_text));
    body.appendChild(clean(post.content));
    li.appendChild(body);
    return li;
  }

  function getJSON(u) {
    return fetch(u, { headers: { Accept: "application/json" } }).then(function (r) {
      if (!r.ok) throw new Error(r.status);
      return r.json();
    });
  }

  Promise.all([getJSON(api), getJSON(api + "/context")]).then(function (res) {
    var root = res[0], replies = res[1].descendants || [];
    var counts = box.querySelector(".reactions");
    counts.querySelector('[data-count="favourites"]').textContent = root.favourites_count;
    counts.querySelector('[data-count="reblogs"]').textContent = root.reblogs_count;
    counts.querySelector('[data-count="replies"]').textContent = replies.length;
    counts.hidden = false;

    // Thread order with indentation: walk replies from the root post.
    var byParent = {};
    replies.forEach(function (r) {
      if (r.visibility !== "public" && r.visibility !== "unlisted") return;
      (byParent[r.in_reply_to_id] = byParent[r.in_reply_to_id] || []).push(r);
    });
    function add(id, depth) {
      (byParent[id] || []).forEach(function (r) { list.appendChild(render(r, depth)); add(r.id, depth + 1); });
    }
    add(root.id, 0);
    status.textContent = list.children.length ? "" : "No comments yet. Be the first to reply on Mastodon.";
  }).catch(function () {
    status.textContent = "Comments couldn't be loaded. You can still read and reply to them on Mastodon.";
  });
})();
