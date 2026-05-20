---
name: execution-time
description: Deep explanation of how FlowMetrix calculates total, wait, and exec duration,
  including interval merging, overlap handling, edge cases, and practical interpretation.
  Use when user asks how exec_dur is computed, why exec differs from wall clock,
  what idle means, or how to optimize workflow duration.
---

# FlowMetrix Execution Time (Extended)

## Quick definition

`exec_dur` is the time where at least one workflow job is actively running.

Formula:

`exec_dur = total_dur - wait_dur`

Where:

- `total_dur = updated_at - run_started_at`
- `wait_dur = total run window - active job time`

---

## Data source and timestamps

FlowMetrix uses GitHub run/job timestamps from the REST API:

- Run start: `run_started_at` (fallback: `created_at`)
- Run end: `updated_at`
- Job interval: `started_at -> completed_at`

Only valid timestamps with `completed_at > started_at` are used for interval math.

---

## Exact calculation steps

1. Compute full run window:
   - `total_dur = seconds_between(run_started_at, updated_at)`

2. Build job intervals:
   - For each job, create interval `[job.started_at, job.completed_at]`
   - Ignore invalid intervals (missing/invalid timestamps or non-positive duration)

3. Sort intervals by start time.

4. Merge overlaps:
   - If next interval starts before current merged interval ends, merge them
   - This prevents counting parallel job time twice

5. Sum merged intervals:
   - `active_job_time = sum(end - start for merged intervals)`

6. Compute idle/wait:
   - `wait_dur = total_dur - active_job_time`
   - Clamped to `>= 0`

7. Compute execution:
   - `exec_dur = total_dur - wait_dur`

When `wait_dur` cannot be computed (e.g., no valid job intervals), FlowMetrix falls back to:

- `exec_dur = total_dur`

---

## Why overlap merging matters

Without merging, parallel jobs inflate runtime incorrectly.

Example:

- Job A: 10:00-10:10 (10 min)
- Job B: 10:05-10:15 (10 min)
- Naive sum = 20 min (wrong)
- Merged active window = 10:00-10:15 = 15 min (correct)

If run window is 10:00-10:20:

- `total_dur = 20 min`
- `active_job_time = 15 min`
- `wait_dur = 5 min`
- `exec_dur = 15 min`

---

## Interpretation guide

- High `exec_dur`, low `wait_dur`
  - Workflow itself is slow
  - Optimize jobs/steps, dependencies, build/test strategy, caching

- Low `exec_dur`, high `wait_dur`
  - Runner availability/queueing bottleneck
  - Review runner capacity, concurrency settings, queue pressure

- Both high
  - Mixed problem: capacity plus workflow performance

---

## What the report uses

FlowMetrix uses `exec_dur` for:

- Average duration
- Min/Max duration
- Trend chart values
- Overview comparison and "Needs attention" status

This keeps the main performance signal focused on actionable execution work, not infrastructure wait.

---

## Important caveat

GitHub UI may show queue metrics derived from internal signals not fully exposed via REST API.

So FlowMetrix `wait_dur` is a consistent approximation based on observable job intervals inside the run window. It is excellent for trend/comparison across workflows, even if it does not exactly match every number shown in GitHub UI.

