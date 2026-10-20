"""End-to-end test of the Asyntai plugin against a real, running Halo.

Halo must run with API basic auth on, for example:

    java -jar halo-2.27.0.jar --server.port=8095 --halo.work-dir=D:/halo-test/w227 \
        --halo.security.basic-auth.disabled=false

Run:
    python test_e2e.py --base http://127.0.0.1:8095 --password <admin password> \
        --jar ../build/libs/asyntai-ai-chatbot-1.0.0.jar [--setup]

--setup runs Halo's first-time setup (user "admin") on a fresh work directory.
The test installs or reuses the plugin, then checks the pages through the
ConfigMap, which is exactly what the "Connect Asyntai" tab and the settings
form write.
"""
import argparse
import base64
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from http.cookiejar import CookieJar

PLUGIN = "asyntai-ai-chatbot"
CONFIG_MAP = "asyntai-ai-chatbot-configmap"
LOADER = "data-asyntai-loader"

results = []


def check(name, ok, detail=""):
    results.append((name, bool(ok)))
    print(("PASS " if ok else "FAIL ") + name + (f"  [{detail}]" if detail and not ok else ""))


class Halo:
    def __init__(self, base, password):
        self.base = base.rstrip("/")
        self.auth = "Basic " + base64.b64encode(f"admin:{password}".encode()).decode()

    def request(self, method, path, body=None, headers=None, auth=True):
        headers = dict(headers or {})
        if auth:
            headers["Authorization"] = self.auth
        data = None
        if body is not None:
            if isinstance(body, (dict, list)):
                data = json.dumps(body).encode()
                headers.setdefault("Content-Type", "application/json")
            else:
                data = body
        req = urllib.request.Request(self.base + path, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return r.status, r.read().decode("utf-8", "replace")
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode("utf-8", "replace")

    def page(self, path):
        status, text = self.request("GET", path, auth=False)
        return status, text

    def config_map(self):
        status, text = self.request("GET", f"/api/v1alpha1/configmaps/{CONFIG_MAP}")
        return json.loads(text) if status == 200 else None

    def set_groups(self, **groups):
        for _ in range(5):
            cm = self.config_map()
            cm.setdefault("data", {})
            for key, value in groups.items():
                if value is None:
                    cm["data"].pop(key, None)
                else:
                    cm["data"][key] = json.dumps(value)
            status, _ = self.request("PUT", f"/api/v1alpha1/configmaps/{CONFIG_MAP}", cm)
            if status == 200:
                time.sleep(1.5)  # The setting fetcher refreshes on the change event.
                return
            time.sleep(1)
        raise RuntimeError("could not update the ConfigMap")


def setup(base, password):
    jar = CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    page = opener.open(base + "/system/setup", timeout=60).read().decode()
    token = re.search(r'name="_csrf"[^>]*value="([^"]+)"|value="([^"]+)"[^>]*name="_csrf"', page)
    form = {
        "siteTitle": "Halo 测试站",
        "username": "admin",
        "password": password,
        "email": "admin@example.com",
        "language": "zh-CN",
        "externalUrl": base,
    }
    if token:
        form["_csrf"] = token.group(1) or token.group(2)
    req = urllib.request.Request(base + "/system/setup", data=urllib.parse.urlencode(form).encode(), method="POST")
    try:
        opener.open(req, timeout=120)
    except urllib.error.HTTPError as e:
        if e.code >= 400:
            raise


def multipart(field, filename, content):
    boundary = uuid.uuid4().hex
    body = (
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"{field}\"; filename=\"{filename}\"\r\n"
        f"Content-Type: application/java-archive\r\n\r\n"
    ).encode() + content + f"\r\n--{boundary}--\r\n".encode()
    return body, {"Content-Type": f"multipart/form-data; boundary={boundary}"}


def install(halo, jar_path):
    status, _ = halo.request("GET", f"/apis/plugin.halo.run/v1alpha1/plugins/{PLUGIN}")
    if status != 200:
        with open(jar_path, "rb") as f:
            body, headers = multipart("file", jar_path.split("/")[-1], f.read())
        status, text = halo.request("POST", "/apis/api.console.halo.run/v1alpha1/plugins/install", body, headers)
        check("plugin installs", status == 200, f"{status} {text[:200]}")
        time.sleep(3)
    halo.request("PUT", f"/apis/api.console.halo.run/v1alpha1/plugins/{PLUGIN}/plugin-state", {"enable": True})
    phase = ""
    for _ in range(40):
        status, text = halo.request("GET", f"/apis/plugin.halo.run/v1alpha1/plugins/{PLUGIN}")
        m = re.search(r'"phase":"([A-Z]+)"', text)
        phase = m.group(1) if m else ""
        if phase == "STARTED":
            break
        time.sleep(1.5)
    check("plugin is started", phase == "STARTED", phase)
    for _ in range(20):
        if halo.config_map():
            break
        time.sleep(1)
    check("Halo creates the plugin ConfigMap", halo.config_map() is not None)


def check_connect_endpoint(halo, mock):
    """The console asks the plugin's own API; the plugin asks Asyntai on the server."""
    api = "/apis/console.api.asyntai.halo.run/v1alpha1/connections/"
    state = "halo_" + uuid.uuid4().hex[:16]
    status, text = halo.request("GET", api + state)
    check("connect API: not connected yet", status == 200 and json.loads(text).get("connected") is False,
          f"{status} {text[:120]}")
    status, _ = halo.request("GET", api + "halo_BAD")
    check("connect API: bad state is refused", status == 400, str(status))
    status, text = halo.request("GET", api + state, auth=False)
    # Halo answers 401/403, or redirects to the login page (urllib follows it).
    check("connect API: needs a login", status in (401, 403) or '"connected"' not in text, str(status))
    urllib.request.urlopen(f"{mock}/wp-auth?platform=halo&state={state}", timeout=10).read()
    status, text = halo.request("GET", api + state)
    data = json.loads(text) if status == 200 else {}
    check("connect API: connected after sign-in", data.get("connected") is True
          and data.get("siteId", "").startswith("asyntai_") and "scriptUrl" in data, f"{status} {text[:160]}")
    status, text = halo.request("GET", "/apis/api.console.halo.run/v1alpha1/ui-plugins/-/bundle.js")
    if status == 404:
        status, text = halo.request("GET", "/apis/api.console.halo.run/v1alpha1/plugins/-/bundle.js")
    check("console bundle loads no remote connect script", "connect-status.js" not in text
          and "console.api.asyntai.halo.run" in text)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True)
    ap.add_argument("--password", required=True)
    ap.add_argument("--jar", required=True)
    ap.add_argument("--setup", action="store_true")
    ap.add_argument("--mock", default="", help="mock Asyntai base URL, for example http://127.0.0.1:8097 "
                    "(Halo must run with -Dasyntai.origin set to the same URL)")
    args = ap.parse_args()

    if args.setup:
        setup(args.base, args.password)
    halo = Halo(args.base, args.password)
    status, _ = halo.request("GET", "/apis/api.console.halo.run/v1alpha1/plugins")
    check("admin API login works", status == 200, str(status))
    install(halo, args.jar)

    status, text = halo.request("GET", f"/apis/api.console.halo.run/v1alpha1/plugins/{PLUGIN}/setting")
    check("settings form is registered", status == 200 and "widgetId" in text, str(status))
    # Halo 2.21+ serves the bundle under ui-plugins, older versions under plugins.
    status, text = halo.request("GET", "/apis/api.console.halo.run/v1alpha1/ui-plugins/-/bundle.js")
    if status == 404:
        status, text = halo.request("GET", "/apis/api.console.halo.run/v1alpha1/plugins/-/bundle.js")
    check("console bundle has the Connect tab",
          status == 200 and "plugin:self:tabs:create" in text and "asyntai-connect" in text, str(status))

    if args.mock:
        check_connect_endpoint(halo, args.mock)

    pages = ["/", "/archives", "/archives/hello-halo", "/tags"]

    # Not connected: no widget anywhere.
    halo.set_groups(basic={"enabled": True}, connection=None)
    for p in pages:
        s, html = halo.page(p)
        check(f"not connected: no loader on {p}", s == 200 and LOADER not in html, str(s))

    # Connected (what the Connect tab saves).
    halo.set_groups(basic={"enabled": True, "widgetId": "asyntai_e2etest1"},
                    connection={"accountEmail": "e2e@example.com",
                                "scriptUrl": "https://widget.asyntai.com/static/js/chat-widget.js"})
    for p in pages:
        s, html = halo.page(p)
        check(f"connected: loader on {p}", s == 200 and html.count(LOADER) == 1 and "asyntai_e2etest1" in html, str(s))
    s, html = halo.page("/")
    head = html.split("</head>")[0] if "</head>" in html else ""
    check("loader sits in <head>", LOADER in head)
    check("loader waits for the load event", 'window.addEventListener("load",l)' in html)
    check("default widget URL is used", '"https://widget.asyntai.com/static/js/chat-widget.js"' in html)

    # Console and user center stay clean.
    for p in ["/console", "/uc"]:
        s, html = halo.request("GET", p)
        check(f"no loader on {p}", LOADER not in html, str(s))

    # Off switch from the settings form.
    halo.set_groups(basic={"enabled": False, "widgetId": "asyntai_e2etest1"})
    s, html = halo.page("/")
    check("enabled=false hides the widget", LOADER not in html)
    halo.set_groups(basic={"enabled": "false", "widgetId": "asyntai_e2etest1"})
    s, html = halo.page("/")
    check('enabled="false" hides the widget', LOADER not in html)

    # Bad values never reach the page.
    halo.set_groups(basic={"enabled": True, "widgetId": 'x"><script>alert(1)</script>'})
    s, html = halo.page("/")
    check("bad widget ID is ignored", LOADER not in html and "alert(1)" not in html)
    halo.set_groups(basic={"enabled": True, "widgetId": "asyntai_e2etest1"},
                    connection={"scriptUrl": "https://evil.example/x.js"})
    s, html = halo.page("/")
    check("untrusted script URL is replaced", "evil.example" not in html and "widget.asyntai.com" in html)
    halo.set_groups(connection={"scriptUrl": "https://beta.asyntai.com/static/js/chat-widget.js"})
    s, html = halo.page("/")
    check("asyntai.com subdomain script URL is kept", "beta.asyntai.com" in html)

    # Disconnect (what the tab saves).
    halo.set_groups(basic={"enabled": True, "widgetId": ""}, connection={})
    s, html = halo.page("/")
    check("disconnect removes the widget", LOADER not in html)

    passed = sum(1 for _, ok in results if ok)
    print(f"\n{passed}/{len(results)} passed")
    sys.exit(0 if passed == len(results) else 1)


if __name__ == "__main__":
    main()
