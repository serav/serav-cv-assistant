---
name: report-reading
description: Explains how to read and interpret the FlowMetrix HTML report.
  Use when user asks about the report, overview table, trend chart, color coding,
  warning icons, avg duration bar, job breakdown, step durations, idle time column,
  exec time, what green or red trend means, what the numbers mean, or how to identify slow workflows.
---

# Reading the Report

## Summary page (`index.html`)

The summary page shows all workflows across all repositories in one overview table.

### Overview table columns

| Column | What it shows |
|---|---|
| **Repository** | `owner/repo` — click to open on GitHub |
| **Workflow** | Workflow name |
| **Runs** | `fetched / total` — e.g. `40 / 312` means 40 runs were analysed out of 312 total |
| **Avg Duration** | Average exec time across fetched runs, shown as a colored bar |
| **Min** | Fastest run in the dataset |
| **Max** | Slowest run in the dataset |

### Avg Duration color coding

The bar color is relative to the **global average** across all workflows:

| Color | Meaning |
|---|---|
| 🟢 Green | At or below the global average — healthy |
| 🟠 Orange | Up to 25% above the global average — worth watching |
| 🔴 Red + ⚠️ | More than 25% above the global average — needs attention |

The **global average** is shown at the top of the table (e.g. `global avg duration: 04:32`).

Rows with a red bar also have a **light orange background** to make them easy to spot.

---

## Workflow detail page

Click any row in the overview table to open the workflow detail page.

### Summary cards
At the top: **Runs analysed**, **Avg duration** (with min and max).

### Trend chart
Shows exec time per run over time, with a **linear trend line**:

| Trend line color | Meaning |
|---|---|
| 🟢 Green | Workflow is getting faster over time |
| 🔴 Red | Workflow is getting slower over time |

The trend line is calculated using linear regression across all fetched runs.

### Run table
Each row is one run. Columns: Run #, Status, Branch, Exec Time, Idle Time.

- **Exec Time** — actual job execution time (what you can optimize)
- **Idle ⏳** — time when no job was running (runner queue wait, gaps between jobs)

Click a row to expand it and see per-job durations for that run.

### Average Job Durations table
Shows per-job statistics across all runs:

| Column | Meaning |
|---|---|
| **Job** | Job name |
| **Avg Duration** | Average duration shown as a bar (relative to the slowest job) |
| **Min / Max** | Fastest and slowest execution of that job |
| **Samples** | How many runs included this job (skipped jobs are excluded) |

The **⏳ Idle** row at the top shows average runner wait time — if this is large, the bottleneck is runner availability, not your workflow code.

Click the **▶** button next to a job name to expand **step-level durations** for that job.

---

## Key insight: Exec Time vs Idle Time

- **Exec Time** = time jobs were actually running → you can optimize this
- **Idle Time** = time waiting for runners or between jobs → you cannot optimize this in your workflow code

If Idle is large, consider using self-hosted runners or reducing workflow concurrency.
