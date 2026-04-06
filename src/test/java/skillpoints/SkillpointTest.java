package skillpoints;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parameterized tests that run every test case against every algorithm.
 *
 * To add a new test case: add to {@link TestCases}.
 * To add a new algorithm: add to {@link #algorithms()}.
 *
 * For benchmarking, run: {@code ./gradlew jmh}
 */
public class SkillpointTest {

    // -- Per-algorithm tallies ---------------------------------------------

    private static final ConcurrentHashMap<String, AtomicInteger> passes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> failures = new ConcurrentHashMap<>();

    @AfterAll
    static void printSummary() {
        System.out.println(
                "\n══════════════════════════════════════════════════════════════════════════════════════");
        System.out.println("  Algorithm Test Summary");
        System.out.println(
                "══════════════════════════════════════════════════════════════════════════════════════");

        Map<String, long[]> summary = new LinkedHashMap<>();
        passes.forEach((algo, cnt) -> summary.computeIfAbsent(algo, k -> new long[2])[0] = cnt.get());
        failures.forEach((algo, cnt) -> summary.computeIfAbsent(algo, k -> new long[2])[1] = cnt.get());

        for (var entry : summary.entrySet()) {
            long p = entry.getValue()[0], f = entry.getValue()[1];
            System.out.printf("  %-25s  PASS: %3d   FAIL: %3d   TOTAL: %3d%n",
                    entry.getKey(), p, f, p + f);
        }
        System.out.println(
                "══════════════════════════════════════════════════════════════════════════════════════\n");
    }

    // -- Algorithms under test ---------------------------------------------

    static Stream<Named<SkillpointChecker>> algorithms() {
        return Stream.of(
                Named.of("WynnAlgorithm", new WynnAlgorithm()),
                Named.of("SCCGraphAlgorithm", new SCCGraphAlgorithm()),
                Named.of("OptimizedDFS", new OptimizedDFSChecker()),
                Named.of("WynnSolver", new WynnSolverAlgorithm()),
                Named.of("GreedyAlgorithm", new GreedyAlgorithm()));
    }

    // -- Test cases (shared with JMH benchmark in TestCases.java) -----------

    static Stream<Named<TestCases.TestCase>> testCases() {
        return TestCases.ALL.entrySet().stream()
                .map(e -> Named.of(e.getKey(), e.getValue()));
    }

    // -- The parameterized test --------------------------------------------

    static Stream<org.junit.jupiter.params.provider.Arguments> algorithmAndCase() {
        return algorithms()
                .flatMap(algo -> testCases()
                        .map(tc -> org.junit.jupiter.params.provider.Arguments.of(algo, tc)));
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("algorithmAndCase")
    void testEquipResult(SkillpointChecker checker, TestCases.TestCase tc) {
        String algoName = checker.getClass().getSimpleName();

        boolean[] result = checker.check(tc.items(), tc.assignedSkillpoints().clone());

        boolean passed = Arrays.equals(tc.expectedEquippable(), result);
        if (passed) {
            System.out.printf("[%s / %s] PASS%n", algoName, tc.name());
        } else {
            System.out.printf("[%s / %s] FAIL%n    expected: %s%n    actual:   %s%n",
                    algoName, tc.name(),
                    Arrays.toString(tc.expectedEquippable()), Arrays.toString(result));
        }

        if (passed) {
            passes.computeIfAbsent(algoName, k -> new AtomicInteger()).incrementAndGet();
        } else {
            failures.computeIfAbsent(algoName, k -> new AtomicInteger()).incrementAndGet();
        }

        assertArrayEquals(tc.expectedEquippable(), result,
                () -> "Case '" + tc.name() + "': expected " + Arrays.toString(tc.expectedEquippable())
                        + " but got " + Arrays.toString(result));
    }
}
