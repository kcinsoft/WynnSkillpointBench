# WynnSkillpointBench

Benchmark and correctness testing for Wynncraft skillpoint equip-ordering algorithms.

## Current Standings

### Full Test

| Algorithm | PASS | FAIL | TOTAL |
|---|---|---|---|
| MyFirstAlgorithm | 26 | 0 | 26 |
| CascadeBoundChecker | 26 | 0 | 26 |
| WynnSolverAlgorithm | 26 | 0 | 26 |
| OurSecondAlgorithm | 26 | 0 | 26 |
| MySecondAlgorithm | 26 | 0 | 26 |
| WynnAlgorithm | 24 | 2 | 26 |
| SCCGraphAlgorithm | 26 | 0 | 26 |
| TheThirdAlgorithm | 26 | 0 | 26 |

### Equip Sequence Performance

| Algorithm | Mean(us/op) | Median | Worst | vs 1st |
|---|---:|---:|---:|---:|
| TheThirdAlgorithm | 4.808 | 4.580 | 6.834 | 1.0x |
| OurSecondAlgorithm | 5.838 | 5.364 | 9.686 | 1.2x |
| MyFirstAlgorithm | 8.243 | 7.867 | 12.457 | 1.7x |
| MySecondAlgorithm | 8.480 | 7.946 | 13.785 | 1.8x |
| CascadeBound | 11.432 | 11.157 | 13.406 | 2.4x |
| WynnAlgorithm | 35.366 | 24.262 | 76.917 | 7.4x |
| WynnSolver | 53.988 | 20.766 | 306.829 | 11.2x |
| SCCGraphAlgorithm | 57.009 | 52.482 | 90.986 | 11.9x |

### Unrepresentative Full Performance

| Algorithm | Mean(us/op) | Median | Worst | vs 1st |
|---|---:|---:|---:|---:|
| TheThirdAlgorithm | 0.047 | 0.036 | 0.105 | 1.0x |
| MySecondAlgorithm | 0.051 | 0.055 | 0.108 | 1.1x |
| OurSecondAlgorithm | 0.054 | 0.058 | 0.103 | 1.1x |
| MyFirstAlgorithm | 0.065 | 0.048 | 0.132 | 1.4x |
| WynnSolver | 0.137 | 0.084 | 0.285 | 2.9x |
| CascadeBound | 0.183 | 0.149 | 0.349 | 3.9x |
| WynnAlgorithm | 0.583 | 0.468 | 2.124 | 12.4x |
| SCCGraphAlgorithm | 0.969 | 0.809 | 3.518 | 20.6x |

## Skill Point Algorithm Bounty

Wynncraft is seeking an optimized **Skill Point allocation algorithm** capable of efficiently validating and maximizing equipment combinations under strict performance constraints.

A bounty reward of **up to 100 in-game shares** will be granted for a successful solution.
Exceptional implementations may qualify for a higher reward.

### Objective

Design an algorithm that:
- Accepts a given set of equipment items
- Evaluates viable combinations
- Returns the **combination containing the highest number of valid items**

### Requirements

Your solution must be written in **Java** so we can evaluate on a real scenario.

#### Performance
- **Worst-case execution time:** ≤ 200,000 nanoseconds
- **Target average execution time:** ≤ 70,000 nanoseconds

#### Functional Constraints
- Each piece of equipment has **skill point requirements** that must be validated
- Equipment may **add or subtract skill points** when equipped
- Skill points from equipment **must not recursively enable other equipment** (no bootstrapping between items)

#### Validation Rules
- A piece of equipment is considered **valid** only if all its requirements are met at the time of evaluation
- The algorithm must determine validity across the full combination, **the order of items should not be relevant**
- In case of a combination tie, the combination with the **highest total given skill points** should win

### Example Edge Cases

TODO

---

## Problem

Given a set of items (each with skillpoint requirements and bonuses) and a player's assigned skillpoints, determine which items can be simultaneously equipped. Items must be equipped in some order where each item's requirements are met at equip time, and no item's requirements are violated by later items' negative bonuses.

## Algorithms

| Class | Approach | Worst-case Time |
|-------|----------|-----------------|
| `WynnAlgorithm` | Greedy positives + 2^n negative-mask enumeration | O(n² · 2^q), q = negative items. All-negative worst case: O(n² · 2^n) |
| `SCCGraphAlgorithm` | Dependency graph → Kosaraju SCC → permute within SCCs | O(n · ∏mᵢ!) across SCC sizes mᵢ. Single-SCC worst case: O(n · n!) |
| `OptimizedDFS` | DFS with dominance pruning + bitmask memoization | O(m · 2^m), m = non-free items after preprocessing (hard-coded m ≤ 8) |
| `WynnSolverAlgorithm` | Free-item activation + backtracking over activation orderings with cascade validity | O(n · k!), k = non-free items. Worst case: O(n · n!) but pruning + early exit keep real builds fast |
| `GreedyAlgorithm` | Greedy with minimum tracking + negative-bonus adjusted requirements | O(n²) |
| `ExactMaskDpChecker` | Precompute sustainability for all masks + BFS over reachable masks | O(n · 2^n) |
| `CascadeBoundChecker` | Forced-closure for safe items + DFS with bitmask memoization over branch items | O(f² · b · 2^n), f = forced (safe) items, b = branch (negative/risky) items. All-branch worst case: O(n · 2^n) |
| `MyFirstAlgorithm` | Greedy fast path + BFS bitmask DP fallback with sustainability checks | O(m² · 2^m), m = non-free items (hard-capped m ≤ 8). Greedy-only best case: O(n²) |

All algorithms extending `SkillpointChecker` implement:
```java
boolean[] check(WynnItem[] items, int[] assignedSkillpoints)
```
Returns a boolean array indicating which items can be equipped.

## Requirements

- **Java 21** (Gradle needs 21)

## Build & Run

```bash
# Run tests
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test

# Run Main.java (manual test harness)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew run
```

If you have Java 21 as your default, you can omit the `JAVA_HOME=` prefix.

## Tests

Tests are in `src/test/java/skillpoints/SkillpointTest.java`. They use JUnit 5 parameterized tests — every test case runs against every algorithm.

**Adding a test case:** add an entry to `TestCases.java` (shared between tests and benchmarks).

**Adding an algorithm:**

1. Create your class in `src/main/java/skillpoints/`, extending `SkillpointChecker`
2. Add a `REGISTRY.put(...)` entry to `AlgorithmRegistry.java`
3. Run tests and benchmarks:
   ```bash
   ./gradlew test
   ./gradlew jmhRun -Palgo=YourAlgorithm
   ./gradlew jmhRun -Palgo=YourAlgorithm -Pbm=EquipSequenceJMH
   ```

## Benchmark

Benchmarking uses [JMH](https://github.com/openjdk/jmh) (Java Microbenchmark Harness) for statistically rigorous results. JMH handles JIT warmup detection, fork isolation, dead-code elimination prevention, and reports confidence intervals.

Default config: 1 fork, 1×200ms warmup, 3×200ms measurement, average time in microseconds.

```bash
# Run all benchmarks (7 algos × 23 cases, ~1-1.5 min)
./gradlew jmh

# Clean first if you changed algorithm code (ensures no stale bytecode in the JMH jar)
./gradlew clean jmh

# Run specific algorithm(s) — with summary report
./gradlew jmhRun -Palgo=WynnAlgorithm

# Run specific algorithm(s) and/or case(s)
./gradlew jmhRun -Palgo=WynnAlgorithm,WynnSolver -Pcase=case8_fullBuild_8items

# Run only a specific benchmark class (e.g. EquipSequenceJMH or SkillpointJMH)
./gradlew jmhRun -Palgo=WynnSolver -Pbm=EquipSequenceJMH
```

Results are written to `build/results/jmh/results.json`. Both `jmh` and `jmhRun` print a summary report.

### Equip Sequence Benchmark

`EquipSequenceJMH` simulates realistic item-by-item equipping. Instead of testing a single `check()` call with all items at once, it models how a player actually equips gear — one piece at a time, with the algorithm rerunning on each equip.

For each full-build (8-item) test case:
1. 8 seeded random permutations of equip order are generated
2. For each permutation, items are added incrementally (1 item, then 2, …, then all 8), calling `check()` at each step
3. Cache is preserved within a permutation but cleared between permutations

Each `@Benchmark` invocation runs all 8 permutations (64 total `check()` calls). To add a new scenario, add an 8-item case to `TestCases.java` and list it in the `@Param` annotation in `EquipSequenceJMH.java`.

```bash
# Run only equip-sequence benchmarks
./gradlew jmhRun -Pbm=EquipSequenceJMH

# Run only the per-case benchmarks (original)
./gradlew jmhRun -Pbm=SkillpointJMH

# Specific algo + equip sequence
./gradlew jmhRun -Palgo=WynnSolver -Pcase=case8_fullBuild_8items -Pbm=EquipSequenceJMH
```
