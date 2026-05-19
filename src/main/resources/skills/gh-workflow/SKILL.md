---
name: gh-workflow
description: Explains how the GitHub Actions workflow works in FlowMetrix.
  Use when user asks about the workflow file, the Generate Metrics Report workflow,
  job order, rebuild_snapshot checkbox, auto-commit, GitHub Pages deployment,
  required permissions, why a job was skipped, or how the snapshot and cache are persisted.
---

# GitHub Actions Workflow

## Overview

The workflow file is `.github/workflows/metrics.yml` and is named **Generate Metrics Report**.
It is triggered **manually only** (`workflow_dispatch`) — it does not run on push or schedule by default.

---

## Three jobs, in order

```
check-snapshot
      │
      ▼
build-snapshot  ──(skipped if snapshot exists and rebuild not requested)
      │
      ▼
generate-report ──► commits cache ──► deploys to GitHub Pages
```

### Job 0: `check-snapshot`
Checks whether `cache/snapshot.yml` already exists in the repository.
Passes the result (`true`/`false`) to the next job as an output variable.

### Job 1: `build-snapshot`
**Runs only when:**
- `rebuild_snapshot = true` was checked, **or**
- `cache/snapshot.yml` does not exist yet (first run)

**Skipped when:**
- `rebuild_snapshot = false` (default) **and** snapshot file already exists

What it does:
1. Runs `snapshot_repositories.py metrics_config.yml`
2. Commits `cache/snapshot.yml` back to the repository with `[skip ci]`

### Job 2: `generate-report`
Always runs after Job 1 (whether it ran or was skipped).

What it does:
1. Runs `create_workflow_metrics.py metrics_config.yml` → writes `report/`
2. Copies `report/` to `docs/` (GitHub Pages source)
3. Commits `cache/workflow_cache.json` back to the repository with `[skip ci]`
4. Uploads `docs/` as a Pages artifact and deploys to GitHub Pages

---

## The `rebuild_snapshot` input

| Value | Effect |
|---|---|
| `false` (default) | Normal run — uses existing `cache/snapshot.yml`, only fetches new run data |
| `true` | Forces `build-snapshot` to run — rediscovers all repos and workflows |

**When to set it to `true`:**
- You added or removed repositories from the `discover:` section
- A workflow was renamed or added in one of your monitored repos

---

## Auto-commits

The workflow commits two files back to the repository automatically:

| File | Committed by | When |
|---|---|---|
| `cache/snapshot.yml` | `build-snapshot` job | Only when snapshot was rebuilt |
| `cache/workflow_cache.json` | `generate-report` job | After every run (if new data was fetched) |

Both commits use `[skip ci]` to prevent triggering the workflow again.

---

## Required permissions

The workflow declares these permissions to enable commits and Pages deployment:

```yaml
permissions:
  contents: write    # to commit cache files back to the repo
  pages: write       # to deploy to GitHub Pages
  id-token: write    # required by actions/deploy-pages
```

---

## Finding the report URL

After the workflow completes, the GitHub Pages URL appears:
1. In the workflow run summary (bottom of the run page)
2. Permanently under **Settings › Pages**

The URL is set via the `github-pages` environment on the `generate-report` job.
