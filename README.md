# WynnSkillpointBench

Benchmark and correctness testing for Wynncraft skillpoint equip-ordering algorithms.

## Current Standings

<img width="599" height="173" alt="image" src="https://github.com/user-attachments/assets/25dd15ce-179f-447e-98c9-3f76c8eca7f0" />

<img width="628" height="183" alt="image" src="https://github.com/user-attachments/assets/dde6c133-4f1e-4cef-a7ce-88f8fe7dbcd2" />

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

## Algorithms (Current Standings)

| Class | Approach | Worst-case Time | Status |
|-------|----------|-----------------|--------|
| `WynnAlgorithm` | Greedy positives + 2^n negative-mask enumeration | O(n² · 2^q), q = negative items. All-negative worst case: O(n² · 2^n) | 22/23, ~0.068ms total |
| `SCCGraphAlgorithm` | Dependency graph → Kosaraju SCC → permute within SCCs | O(n · ∏mᵢ!) across SCC sizes mᵢ. Single-SCC worst case: O(n · n!) | 14/23, ~0.093ms total |
| `OptimizedDFS` | DFS with dominance pruning + bitmask memoization | O(m · 2^m), m = non-free items after preprocessing (hard-coded m ≤ 8) | 20/23, ~0.096ms total |
| `WynnSolverAlgorithm` | Free-item activation + backtracking over activation orderings with cascade validity | O(n · k!), k = non-free items. Worst case: O(n · n!) but pruning + early exit keep real builds fast | 23/23, ~0.015ms total avg |

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

**Adding a test case:** add an entry to `testCases()`.

**Adding an algorithm:** add an entry to `algorithms()`.

## Benchmark

`SkillpointBenchmark.java` runs all test cases against all algorithms 1000 times (with 50 warmup iterations for JIT) and reports average runtimes per algorithm and per case.

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test --tests "skillpoints.SkillpointBenchmark"
```
