// Loads analytics according to assets/config.js.
// - Umami is cookieless, so it loads straight away.
// - The Google Ads tag uses Consent Mode: nothing is stored until the visitor accepts.
(function () {
  var config = window.SITE_CONFIG || {};
  var CONSENT_KEY = "site-ads-consent";

  function readConsent() {
    try { return localStorage.getItem(CONSENT_KEY); } catch (e) { return null; }
  }

  function saveConsent(value) {
    try { localStorage.setItem(CONSENT_KEY, value); } catch (e) { /* storage blocked: ask again next time */ }
  }

  function addScript(src, attrs) {
    var s = document.createElement("script");
    s.async = true;
    s.src = src;
    Object.keys(attrs || {}).forEach(function (k) { s.setAttribute(k, attrs[k]); });
    document.head.appendChild(s);
  }

  // Umami
  if (config.umami && config.umami.src && config.umami.websiteId) {
    addScript(config.umami.src, { "data-website-id": config.umami.websiteId, defer: "" });
  }

  // Google Ads
  var adsId = config.googleAdsId;
  if (!adsId) return;

  window.dataLayer = window.dataLayer || [];
  function gtag() { window.dataLayer.push(arguments); }
  window.gtag = gtag;
  gtag("consent", "default", {
    ad_storage: "denied",
    ad_user_data: "denied",
    ad_personalization: "denied",
    analytics_storage: "denied"
  });
  gtag("js", new Date());
  gtag("config", adsId);
  addScript("https://www.googletagmanager.com/gtag/js?id=" + encodeURIComponent(adsId));

  function grant() {
    gtag("consent", "update", { ad_storage: "granted", ad_user_data: "granted", ad_personalization: "granted" });
  }

  var choice = readConsent();
  if (choice === "granted") grant();

  // Count "early access" clicks as a conversion when a label is configured.
  if (config.googleAdsConversionLabel) {
    document.addEventListener("click", function (e) {
      var el = e.target.closest && e.target.closest("[data-conversion]");
      if (el) gtag("event", "conversion", { send_to: adsId + "/" + config.googleAdsConversionLabel });
    });
  }

  if (choice) return;
  function showBanner() {
    var box = document.createElement("div");
    box.className = "consent";
    box.setAttribute("role", "dialog");
    box.setAttribute("aria-label", "Cookie choice");
    box.innerHTML =
      '<p>We use a Google Ads cookie to see which of our ads bring people here. ' +
      'No cookies are set unless you accept. <a href="/invoice-maker-professional/privacy/#website">Details</a></p>' +
      '<div class="cta-row"><button type="button" class="accept">Accept</button>' +
      '<button type="button" class="decline">Decline</button></div>';
    box.querySelector(".accept").addEventListener("click", function () { saveConsent("granted"); grant(); box.remove(); });
    box.querySelector(".decline").addEventListener("click", function () { saveConsent("denied"); box.remove(); });
    document.body.appendChild(box);
  }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", showBanner);
  else showBanner();
})();
