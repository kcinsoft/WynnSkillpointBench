# WynnSkillpointBench

Benchmark and correctness testing for Wynncraft skillpoint equip-ordering algorithms.

## Current Standings

### Full Test

| Algorithm | PASS | FAIL | TOTAL |
|---|---|---|---|
| MyFirstAlgorithm | 26 | 0 | 26 |
| CascadeBoundChecker | 26 | 0 | 26 |
| WynnSolverAlgorithm | 26 | 0 | 26 |
| MySecondAlgorithm | 26 | 0 | 26 |
| WynnAlgorithm | 24 | 2 | 26 |
| SCCGraphAlgorithm | 26 | 0 | 26 |

### Equip Sequence Performance

| Algorithm | Mean(us/op) | Median | Worst | vs 1st |
|---|---:|---:|---:|---:|
| MySecondAlgorithm | 7.966 | 7.492 | 13.019 | 1.0x |
| MyFirstAlgorithm | 8.168 | 7.492 | 13.329 | 1.0x |
| CascadeBound | 11.396 | 10.817 | 13.416 | 1.4x |
| WynnAlgorithm | 41.007 | 39.947 | 99.208 | 5.1x |
| SCCGraphAlgorithm | 55.176 | 52.314 | 83.981 | 6.9x |
| WynnSolver | 55.315 | 21.025 | 311.931 | 6.9x |

### Unrepresentative Full Performance

| Algorithm | Mean(us/op) | Median | Worst | vs 1st |
|---|---:|---:|---:|---:|
| MySecondAlgorithm | 0.056 | 0.060 | 0.112 | 1.0x |
| MyFirstAlgorithm | 0.068 | 0.049 | 0.135 | 1.2x |
| WynnSolver | 0.142 | 0.081 | 0.309 | 2.6x |
| CascadeBound | 0.192 | 0.159 | 0.388 | 3.4x |
| WynnAlgorithm | 0.647 | 0.474 | 3.222 | 11.6x |
| SCCGraphAlgorithm | 0.946 | 0.834 | 3.361 | 17.0x |

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

**Adding an algorithm:** add an entry to `algorithms()` in `SkillpointTest.java` and to the `@Param` list in `SkillpointJMH.java`.

## Benchmark

Benchmarking uses [JMH](https://github.com/openjdk/jmh) (Java Microbenchmark Harness) for statistically rigorous results. JMH handles JIT warmup detection, fork isolation, dead-code elimination prevention, and reports confidence intervals.

Default config: 1 fork, 1×200ms warmup, 3×200ms measurement, average time in microseconds.

```bash
# Run all benchmarks (5 algos × 23 cases, ~1-1.5 min)
./gradlew jmh

# Clean first if you changed algorithm code (ensures no stale bytecode in the JMH jar)
./gradlew clean jmh

# Build the JMH jar for more control over parameters
./gradlew jmhJar

# Run specific algorithm(s) and/or case(s)
java -jar build/libs/*-jmh.jar -p algoName=WynnAlgorithm,WynnSolver -p caseName=case8_fullBuild_8items

# Quick iteration during development (fewer warmup/measurement iterations)
java -jar build/libs/*-jmh.jar -wi 2 -i 3 -p algoName=WynnSolver
```

Results are written to `build/results/jmh/results.json`.

### Equip Sequence Benchmark

`EquipSequenceJMH` simulates realistic item-by-item equipping. Instead of testing a single `check()` call with all items at once, it models how a player actually equips gear — one piece at a time, with the algorithm rerunning on each equip.

For each full-build (8-item) test case:
1. 8 seeded random permutations of equip order are generated
2. For each permutation, items are added incrementally (1 item, then 2, …, then all 8), calling `check()` at each step
3. Cache is preserved within a permutation but cleared between permutations

Each `@Benchmark` invocation runs all 8 permutations (64 total `check()` calls). To add a new scenario, add an 8-item case to `TestCases.java` and list it in the `@Param` annotation in `EquipSequenceJMH.java`.

```bash
# Run only equip-sequence benchmarks
./gradlew jmh -Pbm="EquipSequenceJMH"

# Run only the per-case benchmarks (original)
./gradlew jmh -Pbm="SkillpointJMH"

# Specific algo/case via the JMH jar
java -jar build/libs/*-jmh.jar -p algoName=WynnSolver -p caseName=case8_fullBuild_8items "EquipSequenceJMH"
```
