---
name: setup
description: Guide for setting up and deploying FlowMetrix for the first time.
  Use when user asks how to get started, install, configure secrets, enable GitHub Pages,
  fork the repository, create a PAT token, or run the workflow for the first time.
---

# Setup Guide

## Step-by-step setup

### 1. Fork the repository
Fork `FlowMetrix` into your own GitHub organization. Each team gets their own fork with their own configuration.

### 2. Create the GitHub Actions secret
The tool needs a Personal Access Token (PAT) to read workflow data from your repositories.

Go to: **Settings › Secrets and variables › Actions › New repository secret**

| Name | Value |
|---|---|
| `METRICS_TOKEN` | A GitHub PAT with `repo` scope (private repos) or `public_repo` (public repos only) |

**How to create a PAT:**
GitHub › Settings › Developer settings › Personal access tokens › Fine-grained tokens or classic tokens. Select `repo` scope. Copy the token immediately — it is only shown once.

### 3. Enable GitHub Pages
Go to: **Settings › Pages**
- **Source:** `GitHub Actions`

The workflow deploys to Pages automatically after every run.

### 4. Configure `metrics_config.yml`
Edit the config file to tell FlowMetrix which repositories to watch. Minimum required fields:

```yaml
mode: snapshot    # or "repositories"
runs: 20          # how many recent runs to fetch per workflow
```

See the **configuration** skill for the full reference.

### 5. Run the workflow
Go to **Actions › Generate Metrics Report › Run workflow**

| Option | When to use |
|---|---|
| `rebuild_snapshot = false` (default) | Normal run — uses existing snapshot |
| `rebuild_snapshot = true` | Use when repos or workflows were added/removed |

### 6. Find your report URL
After the run completes:
1. Open the completed workflow run in Actions
2. Look at the job summary box at the bottom
3. The GitHub Pages URL is listed there

You can also find it permanently under **Settings › Pages**.

## First run note
The first run is the slowest — all job data must be fetched from the GitHub API. Subsequent runs only fetch new data (smart caching).
