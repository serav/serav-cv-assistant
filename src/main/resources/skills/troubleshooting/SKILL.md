---
name: troubleshooting
description: Diagnose and fix common FlowMetrix problems and errors.
  Use when user reports an error, empty report, no results, 401 unauthorized,
  404 not found, rate limit exceeded, stale data, wrong workflow name,
  missing snapshot, or large idle time values.
---

# Troubleshooting

## Report is empty / "No workflow results found"

| Cause | Fix |
|---|---|
| Snapshot is missing or empty | Run `snapshot_repositories.py` first (snapshot mode), or run with `rebuild_snapshot: true` |
| No runs match the branch filter | Check `branches:` in config — try `branches: all` to confirm data exists |
| Workflow name mismatch | Workflow names must match exactly. Check available names in the GitHub Actions tab |
| Wrong mode | If using `mode: repositories`, make sure `repositories:` section is present with valid `url` entries |
| Token has no access to the repo | Verify the PAT has `repo` scope and can access the repository |

---

## 401 Unauthorized

The GitHub token is invalid or expired.
- Check that `METRICS_TOKEN` secret is set correctly in **Settings › Secrets and variables › Actions**
- Regenerate the PAT if it has expired
- Locally: verify `GITHUB_TOKEN` environment variable is set

---

## 404 Not Found

- The repository URL is wrong or the repo is private and the token lacks `repo` scope
- Double-check the `url` field in `metrics_config.yml`
- For private repos: the PAT needs `repo` scope, not just `public_repo`

---

## Rate limit hit (403 / 429)

GitHub allows 5,000 API requests/hour per token.
- The tool automatically retries with backoff when rate-limited
- **First run is the most expensive** — all job data must be fetched cold
- Subsequent runs are much cheaper due to caching
- If you hit limits regularly, reduce `runs:` in the config

---

## Idle time is very large

If the `Idle ⏳` column is large, the bottleneck is **runner availability**, not your workflow code. This means GitHub-hosted runners were queued — you cannot optimize this from within the workflow.

Options:
- Use self-hosted runners
- Reduce concurrency to avoid runner contention
- Run workflows at off-peak times

---

## Snapshot is stale (missing new repos or workflows)

The snapshot is only rebuilt when explicitly requested.

Run the workflow with `rebuild_snapshot: true`, or locally:
```bash
python3 snapshot_repositories.py metrics_config.yml
```

---

## Workflow name not found

The tool prints a warning listing all available workflow names:
```
WARNING: No workflow matching 'My CI'. Available:
  CI Pipeline
  Deploy to Production
  CodeQL
```

Update `metrics_config.yml` to use the exact name shown.

---

## Report shows old / cached data

The workflow run cache (`cache/workflow_cache.json`) is append-only. To force a re-fetch:
1. Delete the specific run entry from `cache/workflow_cache.json`, or
2. Delete the entire file and re-run (will re-fetch everything — slow on first run)
