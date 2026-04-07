#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
GRADLE_USER_HOME_VALUE=${GRADLE_USER_HOME:-/tmp/wynnskillpointbench-gradle}
CORE=${CORE:-0}
REPEAT=${REPEAT:-7}
WARMUP=${WARMUP:-50}
ITERATIONS=${ITERATIONS:-1000}
EVENTS=${EVENTS:-cycles,instructions,branches,branch-misses,task-clock}
KEEP_RAW=${KEEP_RAW:-0}

cd "$ROOT"

CLASSPATH=$(GRADLE_USER_HOME="$GRADLE_USER_HOME_VALUE" ./gradlew -q printTestRuntimeClasspath)

if [ "$#" -gt 0 ]; then
  ALGORITHMS="$*"
else
  ALGORITHMS="WynnAlgorithm SCCGraphAlgorithm OptimizedDFS WynnSolver CascadeBound"
fi

RESULTS_FILE=$(mktemp)
trap 'rm -f "$RESULTS_FILE" "$STDOUT_FILE" "$STDERR_FILE"' EXIT
STDOUT_FILE=""
STDERR_FILE=""

echo "Cycle benchmark"
echo "core=$CORE repeat=$REPEAT warmup=$WARMUP iterations=$ITERATIONS"
echo

for algorithm in $ALGORITHMS; do
  STDOUT_FILE=$(mktemp)
  STDERR_FILE=$(mktemp)

  taskset -c "$CORE" perf stat \
    --repeat "$REPEAT" \
    --field-separator=, \
    --event "$EVENTS" \
    --output "$STDERR_FILE" \
    java \
      -XX:+PerfDisableSharedMem \
      -Dskillpoints.warmup="$WARMUP" \
      -Dskillpoints.iterations="$ITERATIONS" \
      -cp "$CLASSPATH" \
      skillpoints.CycleBenchmarkMain \
      "$algorithm" \
    >"$STDOUT_FILE"

  META_LINE=$(grep '^algorithm=' "$STDOUT_FILE" | tail -n 1)
  if [ -z "$META_LINE" ]; then
    echo "missing benchmark metadata for $algorithm" >&2
    exit 1
  fi

  CASES=$(printf '%s\n' "$META_LINE" | sed -n 's/.* cases=\([0-9][0-9]*\).*/\1/p')
  CHECKSUM=$(printf '%s\n' "$META_LINE" | sed -n 's/.* checksum=\([-0-9][0-9]*\).*/\1/p')
  TOTAL_INVOCATIONS=$(( (WARMUP + ITERATIONS) * CASES ))

  awk -F, -v algorithm="$algorithm" -v checksum="$CHECKSUM" -v total_invocations="$TOTAL_INVOCATIONS" '
    $3 == "cycles:u" { cycles = $1 + 0 }
    $3 == "instructions:u" { instructions = $1 + 0 }
    $3 == "branches:u" { branches = $1 + 0 }
    $3 == "branch-misses:u" { branch_misses = $1 + 0 }
    $3 == "task-clock:u" { task_clock_ms = $1 + 0 }
    END {
      if (cycles == 0) {
        exit 1
      }
      branch_miss_pct = branches == 0 ? 0 : (branch_misses * 100.0 / branches)
      printf "%s|%.0f|%.0f|%.0f|%.0f|%.4f|%.2f|%.2f|%.2f|%.2f|%s\n",
        algorithm,
        cycles,
        instructions,
        branches,
        branch_misses,
        task_clock_ms,
        cycles / total_invocations,
        instructions / total_invocations,
        branches / total_invocations,
        branch_miss_pct,
        checksum
    }
  ' "$STDERR_FILE" >> "$RESULTS_FILE"

  if [ "$KEEP_RAW" = "1" ]; then
    echo "[$algorithm raw stdout]"
    cat "$STDOUT_FILE"
    echo "[$algorithm raw perf]"
    cat "$STDERR_FILE"
    echo
  fi

  rm -f "$STDOUT_FILE" "$STDERR_FILE"
  STDOUT_FILE=""
  STDERR_FILE=""
done

awk -F'|' '
  BEGIN {
    printf "%-18s %12s %12s %12s %10s %12s %10s\n",
      "Algorithm", "Cycles", "Cycles/call", "Instr/call", "BrMiss%", "TaskClock", "Checksum"
    printf "%-18s %12s %12s %12s %10s %12s %10s\n",
      "------------------", "------------", "------------", "------------", "----------", "------------", "----------"
  }
  {
    algorithm = $1
    cycles[algorithm] = $2 + 0
    cycles_per_call[algorithm] = $7 + 0
    instr_call[algorithm] = $8 + 0
    branch_miss_pct[algorithm] = $10 + 0
    task_clock_ms[algorithm] = $6 + 0
    checksum[algorithm] = $11
    order[++count] = algorithm
  }
  END {
    for (i = 1; i <= count; i++) {
      algorithm = order[i]
      printf "%-18s %12.0f %12.2f %12.2f %9.2f%% %10.2f ms %10s\n",
        algorithm,
        cycles[algorithm],
        cycles_per_call[algorithm],
        instr_call[algorithm],
        branch_miss_pct[algorithm],
        task_clock_ms[algorithm],
        substr(checksum[algorithm], 1, 10)
    }
  }
' "$RESULTS_FILE"
