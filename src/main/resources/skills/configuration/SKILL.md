---
name: configuration
description: Full reference for metrics_config.yml configuration file.
  Use when user asks about config fields, operation modes, snapshot mode, repositories mode,
  branch filters, workflow filters, runs count, include_failed, discover section,
  skip_repos, skip_workflows, or wildcard patterns.
---

# Configuration Reference

## Top-level fields

| Field | Required | Default | Description |
|---|---|---|---|
| `mode` | **mandatory** | — | `snapshot` or `repositories` |
| `runs` | **mandatory** | — | Number of recent runs to fetch per workflow (positive integer) |
| `branches` | optional | `all` | Branch filter: exact name, wildcard (e.g. `release*`), list, or `all` |
| `include_failed` | optional | `false` | Also fetch failed/cancelled runs (not just successful) |

---

## `mode: snapshot` — with `discover:` section

Used by `snapshot_repositories.py` to auto-discover repos.

| Field | Required | Description |
|---|---|---|
| `url` | **mandatory** | GitHub URL with optional wildcard, e.g. `https://github.com/org/prefix-*` |
| `skip_repos` | optional | Exact repository names to exclude |
| `skip_workflows` | optional | Exact workflow names to exclude from every repo in this entry |

```yaml
mode: snapshot
runs: 40
branches: main

discover:
  - url: https://github.com/your-org/your-service-*
    skip_repos:
      - your-service-sandbox
      - your-service-archived
    skip_workflows:
      - Dependabot auto-merge
      - CodeQL
```

- Use `*` as a wildcard: `org/backend-*` matches all repos starting with `backend-`
- You can add multiple `discover` entries for different prefixes
- `skip_repos` and `skip_workflows` use **exact name matching**

---

## `mode: repositories` — with `repositories:` section

Repos and workflows are listed explicitly. No discovery step needed.

| Field | Required | Default | Description |
|---|---|---|---|
| `url` | **mandatory** | — | Exact GitHub repository URL |
| `workflow` | optional | all workflows | Single workflow name to fetch |
| `workflows` | optional | all workflows | List of workflow names to fetch |
| `branches` | optional | global `branches` | Per-repository branch filter override |
| `runs` | optional | global `runs` | Per-repository run count override |

```yaml
mode: repositories
runs: 20
branches:
  - main
  - hotfix*
include_failed: false

repositories:
  - url: https://github.com/org/my-repository
    workflow: "CI Pipeline"
    runs: 10

  - url: https://github.com/org/other-repository
    workflows:
      - "Build"
      - "Deploy"
    branches: main
```

---

## Branch filter patterns

| Pattern | Matches |
|---|---|
| `all` | Every branch (no filter) |
| `main` | Exact branch name `main` |
| `release*` | Any branch starting with `release` |
| list of patterns | Any branch matching at least one pattern (OR logic) |
