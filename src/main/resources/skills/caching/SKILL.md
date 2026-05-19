---
name: caching
description: Explains how FlowMetrix caching works, what is cached and when.
  Use when user asks about caching, cache files, snapshot.yml, workflow_cache.json,
  stale data, cache refresh, when data is re-fetched, rate limits, or first run being slow.
---

# Caching

## Two cache files

### `cache/snapshot.yml` — repository snapshot
- Contains the list of discovered repositories and their workflow names
- Built by `snapshot_repositories.py` (snapshot mode only)
- **Not rebuilt automatically** — only refreshed when you run the workflow with `rebuild_snapshot: true`
- Should be committed to the repository so the report workflow can use it without re-discovery

**When to rebuild:**
- You added or removed repositories matching your wildcard pattern
- A workflow was added or renamed in one of your repos
- You changed the `discover:` section in `metrics_config.yml`

### `cache/workflow_cache.json` — run data cache
- Contains per-run job data: job names, durations, step timings
- **Append-only** — completed runs are cached forever and never re-fetched
- Only new runs (not yet in cache) are fetched from the GitHub API
- Should be committed to the repository so it persists across workflow runs

---

## Why caching matters

- **First run is always slow** — every run must be fetched from the API
- **Subsequent runs are fast** — only new runs are fetched; cached runs cost zero API calls
- **Rate limits** — GitHub allows 5,000 API requests/hour per token. With many repos and a cold cache this limit can be hit. The cache prevents this on subsequent runs.

---

## Cache location

```
cache/
├── snapshot.yml           # repo & workflow discovery (committed)
└── workflow_cache.json    # per-run job data (committed, append-only)
```

---

## Forcing a cache refresh

| What to refresh | How |
|---|---|
| Repository list (snapshot) | Run workflow with `rebuild_snapshot: true` |
| Run data for a specific workflow | Delete the entry from `workflow_cache.json` and re-run |
| Everything | Delete both cache files and re-run with `rebuild_snapshot: true` |
