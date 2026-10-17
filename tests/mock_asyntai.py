"""A stand-in for asyntai.com's connect flow, for the end-to-end test.

GET /wp-auth?platform=halo&state=S   records the platform and marks S connected
GET /connect-status.js?state=S&cb=F  answers F(null) until S is connected, then
                                     F({site_id, script_url, account_email})
GET /log                             JSON list of the /wp-auth calls seen

Run: python mock_asyntai.py 8097
"""
import json
import re
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

SITE = {
    "site_id": "asyntai_mockconnect1",
    "script_url": "https://widget.asyntai.com/static/js/chat-widget.js",
    "account_email": "halo-test@example.com",
}
connected = set()
auth_log = []


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def _send(self, status, body, content_type):
        data = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        url = urlparse(self.path)
        query = {k: v[0] for k, v in parse_qs(url.query).items()}
        if url.path == "/wp-auth":
            state = query.get("state", "")
            auth_log.append({"platform": query.get("platform"), "state": state, "site_url": query.get("site_url")})
            if state:
                connected.add(state)
            self._send(200, "<!doctype html><title>Signed in</title><p>Signed in. You can close this window.</p>"
                            "<script>setTimeout(function(){window.close()},300)</script>", "text/html")
        elif url.path == "/connect-status.js":
            callback = query.get("cb", "callback")
            if not re.match(r"^[A-Za-z_$][\w$]*$", callback):
                return self._send(400, "bad callback", "text/plain")
            data = SITE if query.get("state") in connected else None
            self._send(200, "%s(%s)" % (callback, json.dumps(data)), "application/javascript")
        elif url.path == "/log":
            self._send(200, json.dumps(auth_log), "application/json")
        else:
            self._send(404, "not found", "text/plain")


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8097
    ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
