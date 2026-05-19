---
name: local-dev
description: Instructions for running FlowMetrix locally on a developer machine.
  Use when user asks how to run the tool locally, install dependencies, use the CLI,
  run tests, use verbose mode, or understand the project structure and output files.
---

# Local Development

## Prerequisites

```bash
pip install requests pyyaml
```

You also need a GitHub Personal Access Token with `repo` scope (or `public_repo` for public repos):

```bash
export GITHUB_TOKEN=ghp_your_token_here
```

---

## Running locally

### `mode: snapshot`
Two steps — discover repos first, then generate the report:

```bash
python3 snapshot_repositories.py metrics_config.yml
python3 create_workflow_metrics.py metrics_config.yml
```

### `mode: repositories`
Single step — repos are defined in the config:

```bash
python3 create_workflow_metrics.py metrics_config.yml
```

### Open the report
```bash
open report/index.html
```

---

## Verbose output
Add `--verbose` or `-v` to see API call details and cache hit/miss info:

```bash
python3 create_workflow_metrics.py metrics_config.yml --verbose
```

---

## Output locations

| Output | Path |
|---|---|
| Summary page | `report/index.html` |
| Per-workflow pages | `report/workflows/<slug>.html` |
| Repository snapshot | `cache/snapshot.yml` |
| Run data cache | `cache/workflow_cache.json` |

Output paths are hardcoded — they cannot be changed via config.

---

## Project structure

```
create_workflow_metrics.py     # main entry point
snapshot_repositories.py       # repo discovery entry point
metrics_config.yml             # unified config file

src/
├── config.py                  # config loading & validation
├── models.py                  # RunRecord, WorkflowResult, RepoEntry
├── cache.py                   # RunsCache, load_repo_snapshot()
├── api.py                     # GitHubClient — all GitHub REST calls
├── analysis.py                # duration & idle time calculation
├── render_html.py             # HTML/JS report generation
├── utils.py                   # formatting helpers
└── main.py                    # orchestration

cache/                         # gitignored locally, committed in CI
report/                        # generated output
```

---

## Running tests

```bash
# Full test suite
python3 -m pytest

# Single test file
python3 -m pytest tests/test_analysis.py

# Single test by name
python3 -m pytest tests/test_analysis.py::test_calc_idle_time
```
