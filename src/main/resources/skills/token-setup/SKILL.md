---
name: Token Setup
description: Guide for creating a GitHub Personal Access Token (PAT) for FlowMetrix and how to configure SSO authorisation if the target organisation enforces SAML SSO.
---

# Setup Guide

## Step-by-step setup

## Step 1 — Create the PAT

1. Go to: **GitHub › Settings › Developer settings › Personal access tokens**
2. Choose **Fine-grained tokens** (recommended) or **Tokens (classic)**.
3. For **classic tokens**, select the following scopes:
   - `repo` — required for private repositories (grants read access to Actions data)
   - `public_repo` — sufficient if all repositories are public
4. Set an expiration date (GitHub recommends 90 days or less).
5. Click **Generate token**.
6. **Copy the token immediately** — GitHub only shows it once.

---

## Step 2 — Add the token as a repository secret

1. Go to your FlowMetrix fork on GitHub.
2. Navigate to: **Settings › Secrets and variables › Actions › New repository secret**
3. Create a secret with:
   - **Name:** `METRICS_TOKEN`
   - **Value:** the token you just copied

---

## Step 3 — Configure SSO (required for SSO-enforced organisations)

If the target repositories belong to a GitHub organisation that enforces **SAML Single Sign-On (SSO)**, the token must be explicitly authorised for that organisation — even if it has the correct scopes.

Without SSO authorisation, the GitHub API returns `403` or `404` for all organisation repositories.

### How to authorise:

1. Go to: **GitHub › Settings › Developer settings › Personal access tokens**
2. Find the token you just created.
3. Click **Configure SSO** next to the token.
4. Click **Authorise** next to every organisation whose repositories FlowMetrix should access.
5. Complete the SSO login if prompted.

> You must repeat this step every time you regenerate or rotate the token.

---

## Symptoms of a missing or misconfigured token

| Symptom | Likely cause |
|---|---|
| `403 Forbidden` on all API calls | Token lacks `repo` scope, or SSO not authorised |
| `404 Not Found` for org repositories | SSO not authorised for the organisation |
| `401 Unauthorized` | Token is expired or invalid |
| Empty report with no workflows | Token created but not saved as `METRICS_TOKEN` secret |
| `ERROR: No repositories configured` + all repos show `SKIP … no workflows in snapshot` | SSO not authorised — token exists but was never authorised for the organisation (see below) |

---

## Troubleshooting: "No repositories configured" / all repos skipped

**Full error pattern:**
```
ERROR: No repositories configured.
Mode: snapshot-driven (repos & workflows from cache/snapshot.yml)
     Check your config file — 'repositories' list is empty or missing.
SKIP <org>/<repo> — no workflows in snapshot
...
→ 0 repository(s) loaded from snapshot
Error: Process completed with exit code 1.
```

**Root cause:** The `METRICS_TOKEN` secret exists and has the correct scopes, but **SSO authorisation was never completed** for the target organisation. The GitHub API silently returns empty results for all organisation repositories instead of a `403`, which causes the snapshot to be empty and the report to produce no data.

**Fix:**

1. Go to: **GitHub › Settings › Developer settings › Personal access tokens**
2. Find the token used as `METRICS_TOKEN`.
3. Click **Configure SSO** next to it.
4. Click **Authorise** next to every organisation whose repositories FlowMetrix should access.
5. Complete the SSO login if prompted.
6. Re-run the `snapshot_repositories.py` step (or re-trigger the workflow) — repositories with workflows should now appear.

> This is the most common cause of a completely empty snapshot. The token appears valid, no errors are thrown during authentication, but the GitHub API returns no repositories because the SSO session is not linked.

---

## Summary

| Task | Required |
|---|---|
| Create PAT with `repo` scope | ✅ Always |
| Add as `METRICS_TOKEN` secret | ✅ Always |
| Configure SSO for organisation | ✅ If org enforces SAML SSO |

