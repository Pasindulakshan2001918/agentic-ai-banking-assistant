import test from "node:test";
import assert from "node:assert/strict";
import { createApiClient } from "../src/api.js";
import { escapeHtml, formatMoney, humanize, safeParse, statusTag } from "../src/formatters.js";

test("escapeHtml encodes HTML-significant characters", () => {
  assert.equal(escapeHtml(`<script a="b">&'`), "&lt;script a=&quot;b&quot;&gt;&amp;&#39;");
});

test("formatMoney formats currency and handles missing values", () => {
  assert.equal(formatMoney("1250.50", "LKR"), "LKR 1,250.50");
  assert.equal(formatMoney(null), "—");
});

test("humanize and statusTag format backend enum values", () => {
  assert.equal(humanize("PAYMENT_PENDING"), "payment pending");
  assert.match(statusTag("PAYMENT_PENDING"), /status-payment-pending/);
});

test("safeParse returns null for invalid stored JSON", () => {
  assert.deepEqual(safeParse('{"ready":true}'), { ready: true });
  assert.equal(safeParse("{invalid"), null);
});

test("API client adds admin auth and reports HTTP errors", async (context) => {
  const originalFetch = globalThis.fetch;
  context.after(() => {
    globalThis.fetch = originalFetch;
  });

  let requestHeaders;
  globalThis.fetch = async (_url, options) => {
    requestHeaders = options.headers;
    return new Response(JSON.stringify({ ok: true }), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    });
  };
  const api = createApiClient({ getApiBase: () => "http://localhost/api/v1", getToken: () => "test-token" });
  const result = await (await api("/admin/test")).json();
  assert.deepEqual(result, { ok: true });
  assert.equal(requestHeaders.get("Authorization"), "Bearer test-token");
  assert.equal(requestHeaders.get("Accept"), "application/json");

  globalThis.fetch = async () => new Response(JSON.stringify({ error: { message: "Forbidden" } }), { status: 403 });
  await assert.rejects(api("/admin/blocked"), (error) => error.status === 403 && error.message === "Forbidden");
});
