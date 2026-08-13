## 1. Baseline and migration fixtures

- [x] 1.1 Inventory the current Room database versions, exported schemas, database filenames, and registered migrations; identify the legacy table/column mapping required for FeedFavorite, HomeMenu, and RecentAtUser.
- [x] 1.2 Add Room migration test support and representative legacy fixtures based on the historical v1 entity definitions without changing production behavior.

## 2. Room data migration safety

- [x] 2.1 Bump FeedFavoriteDatabase to version 3 and add direct v1→v3 and v2→v3 migrations that normalize legacy `FeedFavorite` and current `FeedEntity` table layouts without changing the released v2 schema.
- [x] 2.2 Rewrite HomeMenu v2→v3 migration to preserve existing menu titles, order, and enabled state while adding the new position field.
- [x] 2.3 Rewrite RecentAtUser v1→v2 migration to preserve compatible users and handle duplicate usernames deterministically.
- [x] 2.4 Add migration assertions for final schema identity, columns, primary keys/indexes, row counts, and representative field values; verify no destructive fallback is configured.

## 3. Credential request boundary

- [x] 3.1 Add a single HTTPS and host allowlist helper derived from the existing API endpoint constants.
- [x] 3.2 Gate Token, Cookie, device identity, and application identity headers in AddCookiesInterceptor using the resolved request URL while preserving the required trusted-host API behavior.
- [x] 3.3 Add interceptor tests for trusted hosts, external absolute URLs, non-HTTPS URLs, and cross-host redirects, including assertions for all sensitive headers.

## 4. Credential backup policy

- [x] 4.1 Move API/device credential fields to a dedicated credentials SharedPreferences file and implement an idempotent migration from legacy settings keys.
- [x] 4.2 Exclude the credentials preference file from both cloud backup and device-transfer backup while retaining ordinary UI preferences.
- [x] 4.3 Add tests for first-run migration, repeated migration, legacy-key cleanup, and backup-rule coverage.

## 5. WebView lifecycle safety

- [x] 5.1 Remove the explicit process exit from WebViewActivity destruction and retain only normal WebView resource cleanup.
- [x] 5.2 Add or extend an instrumentation regression test proving that closing/recreating WebViewActivity does not terminate the host process or prevent reopening the page.

## 6. Network cancellation and HTTP errors

- [x] 6.1 Replace the NetworkRepo Call bridge with a cancellable adapter that cancels the underlying Retrofit Call and rethrows CancellationException.
- [x] 6.2 Make await/response reject non-2xx responses and empty required bodies while retaining non-sensitive status diagnostics.
- [x] 6.3 Add unit tests for success, empty body, 401/403/5xx responses, transport failure, and coroutine cancellation.

## 7. Verification and project records

- [x] 7.1 Run unit tests, lint, and available instrumentation tests; investigate all failures and record unavailable device evidence explicitly.
- [x] 7.2 Update docs/todo.md, docs/decisions.md, README.md, and docs/sessions/2026-08-13.md with the completed scope, decisions, verification evidence, and remaining pagination work.
