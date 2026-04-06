package skillpoints;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parameterized tests that run every test case against every algorithm.
 *
 * To add a new test case: add to {@link #testCases()}.
 * To add a new algorithm: add to {@link #algorithms()}.
 */
public class SkillpointTest {

    // -- Per-algorithm tallies ---------------------------------------------

    private static final ConcurrentHashMap<String, AtomicInteger> passes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> failures = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> totalTimeNs = new ConcurrentHashMap<>();

    @AfterAll
    static void printSummary() {
        System.out.println(
                "\n══════════════════════════════════════════════════════════════════════════════════════");
        System.out.println("  Algorithm Test Summary");
        System.out.println(
                "══════════════════════════════════════════════════════════════════════════════════════");

        // Collect all algorithm names
        Map<String, long[]> summary = new LinkedHashMap<>();
        passes.forEach((algo, cnt) -> summary.computeIfAbsent(algo, k -> new long[3])[0] = cnt.get());
        failures.forEach((algo, cnt) -> summary.computeIfAbsent(algo, k -> new long[3])[1] = cnt.get());
        totalTimeNs.forEach((algo, ns) -> summary.computeIfAbsent(algo, k -> new long[3])[2] = ns.get());

        for (var entry : summary.entrySet()) {
            long p = entry.getValue()[0], f = entry.getValue()[1], ns = entry.getValue()[2];
            System.out.printf("  %-25s  PASS: %3d   FAIL: %3d   TOTAL: %3d   TIME: %8.3f ms%n",
                    entry.getKey(), p, f,
                    p + f, ns / 1e6);
        }
        System.out.println(
                "══════════════════════════════════════════════════════════════════════════════════════\n");
    }

    // -- Test-case record --------------------------------------------------

    record TestCase(
            String name,
            WynnItem[] items,
            int[] assignedSkillpoints,
            boolean[] expectedEquippable // which items should end up equipped
    ) {
    }

    // -- Algorithms under test ---------------------------------------------

    static Stream<Named<SkillpointChecker>> algorithms() {
        return Stream.of(
                Named.of("WynnAlgorithm", new WynnAlgorithm()),
                Named.of("SCCGraphAlgorithm", new SCCGraphAlgorithm()),
                Named.of("OptimizedDFS", new OptimizedDFSChecker()),
                Named.of("WynnSolver", new WynnSolverAlgorithm()));
    }

    // -- Test cases ---------------------------------------------------------

    static Stream<Named<TestCase>> testCases() {
        return Stream.of(

                // shatterglass → granitic mettle → earth breaker
                // SP [68,0,0,33,0] → +[7,0,0,-3,0] → +[0,0,0,10,0] → +[9,0,0,8,0]
                Named.of("case1_optimal", new TestCase(
                        "case1_optimal",
                        new WynnItem[] {
                                new WynnItem(new int[] { 50, 0, 0, 40, 0 },
                                        new int[] { 9, 0, 0, 8, 0 }), // earth
                                                                      // breaker
                                new WynnItem(new int[] { 75, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 10, 0 }), // Granitic
                                                                       // Mettle
                                new WynnItem(new int[] { 50, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, -3, 0 }), // Shatterglass
                        },
                        new int[] { 68, 0, 0, 33, 0 },
                        new boolean[] { true, true, true })),

                // shatterglass → earth breaker → granitic mettle
                // SP [59,0,0,43,0] → +[7,0,0,-3,0] → +[9,0,0,8,0] → +[0,0,0,10,0]
                Named.of("case1_subopt_assign", new TestCase(
                        "case1_subopt_assign",
                        new WynnItem[] {
                                new WynnItem(new int[] { 50, 0, 0, 40, 0 },
                                        new int[] { 9, 0, 0, 8, 0 }), // earth
                                                                      // breaker
                                new WynnItem(new int[] { 75, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 10, 0 }), // Granitic
                                                                       // Mettle
                                new WynnItem(new int[] { 50, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, -3, 0 }), // Shatterglass
                        },
                        new int[] { 59, 0, 0, 43, 0 },
                        new boolean[] { true, true, true })),

                // Same as case1_subopt_assign but with def=40, expecting failure.
                // To maximize sp, only earth breaker equips.
                Named.of("case1_tff", new TestCase(
                        "case1_tff",
                        new WynnItem[] {
                                new WynnItem(new int[] { 50, 0, 0, 40, 0 },
                                        new int[] { 9, 0, 0, 8, 0 }), // earth
                                                                      // breaker
                                new WynnItem(new int[] { 75, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 10, 0 }), // Granitic
                                                                       // Mettle
                                new WynnItem(new int[] { 50, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, -3, 0 }), // Shatterglass
                        },
                        new int[] { 59, 0, 0, 40, 0 },
                        new boolean[] { true, false, false })),

                // 2) Strict dependency chain: only order a -> b -> c works
                Named.of("case2_strictChain_abc", new TestCase(
                        "case2_strictChain_abc",
                        new WynnItem[] {
                                new WynnItem(new int[] { 1, 0, 0, 0, 0 },
                                        new int[] { 0, 2, -1, 0, 0 }), // a
                                new WynnItem(new int[] { 0, 2, 0, 0, 0 },
                                        new int[] { 0, 0, 1, 0, 0 }), // b
                                new WynnItem(new int[] { 0, 0, 1, 0, 0 },
                                        new int[] { 0, 0, 0, 1, 0 }), // c
                        },
                        new int[] { 1, 0, 1, 0, 0 },
                        new boolean[] { true, true, true })),

                // 3) No requirements at all — everything equips freely
                Named.of("case3_noRequirements", new TestCase(
                        "case3_noRequirements",
                        new WynnItem[] {
                                new WynnItem(new int[] { 0, 0, 0, 0, 0 },
                                        new int[] { 5, 5, 5, 5, 5 }),
                                new WynnItem(new int[] { 0, 0, 0, 0, 0 },
                                        new int[] { -2, 0, 0, 0, 0 }),
                        },
                        new int[] { 0, 0, 0, 0, 0 },
                        new boolean[] { true, true })),

                // 4) Impossible requirements — not enough SP
                Named.of("case4_impossibleRequirements", new TestCase(
                        "case4_impossibleRequirements",
                        new WynnItem[] {
                                new WynnItem(new int[] { 100, 0, 0, 0, 0 },
                                        new int[] { 5, 0, 0, 0, 0 }),
                        },
                        new int[] { 10, 0, 0, 0, 0 },
                        new boolean[] { false })),

                // 5) Negative bonus that would invalidate a prior item
                // Item A needs str 10, gives +5. Item B gives -20 str.
                // After both: 10+5-20 = -5 < A's req of 10. B should not equip.
                Named.of("case5_negativeInvalidatesPrior", new TestCase(
                        "case5_negativeInvalidatesPrior",
                        new WynnItem[] {
                                new WynnItem(new int[] { 10, 0, 0, 0, 0 },
                                        new int[] { 5, 0, 0, 0, 0 }),
                                new WynnItem(new int[] { 0, 0, 0, 0, 0 },
                                        new int[] { -20, 0, 0, 0, 0 }),
                        },
                        new int[] { 10, 0, 0, 0, 0 },
                        new boolean[] { true, false })),

                // 6) Single item, exactly meets requirements
                Named.of("case6_exactRequirement", new TestCase(
                        "case6_exactRequirement",
                        new WynnItem[] {
                                new WynnItem(new int[] { 50, 30, 0, 0, 0 },
                                        new int[] { 0, 0, 10, 0, 0 }),
                        },
                        new int[] { 50, 30, 0, 0, 0 },
                        new boolean[] { true })),

                // 7) Mutual dependency — circular, neither can equip first
                Named.of("case7_mutualDependency", new TestCase(
                        "case7_mutualDependency",
                        new WynnItem[] {
                                new WynnItem(new int[] { 0, 10, 0, 0, 0 },
                                        new int[] { 10, 0, 0, 0, 0 }), // needs
                                                                       // dex,
                                                                       // gives
                                                                       // str
                                new WynnItem(new int[] { 10, 0, 0, 0, 0 },
                                        new int[] { 0, 10, 0, 0, 0 }), // needs
                                                                       // str,
                                                                       // gives
                                                                       // dex
                        },
                        new int[] { 0, 0, 0, 0, 0 },
                        new boolean[] { false, false })),

                // 8) Large build — Valhalla + Dark Shroud + Far Cosmos + Brainwash +
                // Giant's Ring + Prism + Prowess + Shatterglass
                Named.of("case8_fullBuild_8items", new TestCase(
                        "case8_fullBuild_8items",
                        new WynnItem[] {
                                new WynnItem(new int[] { 40, 0, 0, 40, 40 },
                                        new int[] { 9, 0, 0, 9, 9 }), // Valhalla
                                new WynnItem(new int[] { 0, 15, 0, 0, 50 },
                                        new int[] { 0, 15, 0, 0, 25 }), // Dark
                                                                        // Shroud
                                new WynnItem(new int[] { 30, 30, 30, 30, 30 },
                                        new int[] { 8, 8, 8, 8, 8 }), // Far
                                                                      // Cosmos
                                new WynnItem(new int[] { 40, 70, 0, 0, 0 },
                                        new int[] { 13, 0, -50, 0, 0 }), // Brainwash
                                new WynnItem(new int[] { 25, 0, 0, 0, 0 },
                                        new int[] { 5, 0, 0, 0, -3 }), // Giant's
                                                                       // Ring
                                new WynnItem(new int[] { 25, 25, 25, 25, 25 },
                                        new int[] { 3, 3, 3, 3, 3 }), // Prism
                                new WynnItem(new int[] { 0, 0, 0, 0, 0 },
                                        new int[] { 4, 4, 4, 4, 4 }), // Prowess
                                new WynnItem(new int[] { 50, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, -3, 0 }), // Shatterglass
                        },
                        new int[] { 21, 40, 73, 28, 29 },
                        // All items should be equippable in some valid order
                        new boolean[] { true, true, true, true, true, true, true, true })),

                // ── Ported from WynnSolver test_skillpoints.json ────────

                // 9) Dex/Int/Agi build with negative str/def bonuses
                // Boreal Crown + Etiolation + Thunderhead + Liminal Strides + Vilavud Os
                // + Creed Catalyst + Thought Energy + Stormleader + Epoch
                Named.of("case9_dexIntAgiBuild", new TestCase(
                        "case9_dexIntAgiBuild",
                        new WynnItem[] {
                                new WynnItem(new int[] { 0, 40, 40, 0, 40 },
                                        new int[] { -30, 0, 0, -30, 0 }), // Boreal-Patterned
                                                                          // Crown
                                new WynnItem(new int[] { 0, 50, 0, 0, 65 },
                                        new int[] { 0, 8, 0, 0, 0 }), // Etiolation
                                new WynnItem(new int[] { 0, 50, 55, 0, 50 },
                                        new int[] { 0, 6, 4, 0, 6 }), // Thunderhead
                                new WynnItem(new int[] { 0, 55, 0, 0, 55 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Liminal
                                                                      // Strides
                                new WynnItem(new int[] { 0, 50, 0, 0, 45 },
                                        new int[] { 0, 4, 0, 0, 0 }), // Vilavud
                                                                      // Os
                                new WynnItem(new int[] { 0, 60, 0, 0, 0 },
                                        new int[] { 0, 0, 6, 0, 0 }), // Creed
                                                                      // Catalyst
                                new WynnItem(new int[] { 0, 45, 45, 0, 45 },
                                        new int[] { -15, 0, 0, 0, 0 }), // Thought
                                                                        // Energy
                                new WynnItem(new int[] { 0, 45, 0, 0, 0 },
                                        new int[] { 0, 8, 0, 0, 0 }), // Stormleader
                        // new WynnItem(new int[] { 0, 70, 0, 0, 70 }, new int[] { 0, 0, 0, 0, 0
                        // }), //
                        // Epoch
                        // // (weapon)
                        },
                        new int[] { 0, 45, 49, 0, 65 },
                        new boolean[] { true, true, true, true, true, true, true, true })),

                // 10) Int/Agi-heavy mage build with large negative dex/def
                // Aphotic + Phantasmal Hostage + Tao + Moontower + Yang
                // + Cistern Circlet + Diamond Hydro Bracelet + Bookworm + Spring
                Named.of("case10_intAgiHeavyMageBuild", new TestCase(
                        "case10_intAgiHeavyMageBuild",
                        new WynnItem[] {
                                new WynnItem(new int[] { 0, 0, 100, 0, 0 },
                                        new int[] { 0, -80, 5, 0, 0 }), // Aphotic
                                new WynnItem(new int[] { 50, 0, 65, 0, 50 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Phantasmal
                                                                      // Hostage
                                new WynnItem(new int[] { 50, 0, 55, 0, 0 },
                                        new int[] { 6, 0, 6, 0, 0 }), // Tao
                                new WynnItem(new int[] { 0, 0, 70, 0, 80 },
                                        new int[] { -10, -10, 35, -40, 60 }), // Moontower
                                new WynnItem(new int[] { 0, 0, 55, 0, 0 },
                                        new int[] { 0, 0, 4, 0, 0 }), // Yang
                                new WynnItem(new int[] { 0, 0, 65, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Cistern
                                                                      // Circlet
                                new WynnItem(new int[] { 0, 0, 100, 0, 0 },
                                        new int[] { 0, 0, 6, 0, 0 }), // Diamond
                                                                      // Hydro
                                                                      // Bracelet
                                new WynnItem(new int[] { 0, 0, 55, 0, 0 },
                                        new int[] { 0, 0, 4, 0, 0 }), // Bookworm
                        // new WynnItem(new int[] { 0, 0, 120, 0, 0 }, new int[] { 15, -40, 15,
                        // 0, 0 }),
                        // // Spring
                        // // (weapon)
                        },
                        new int[] { 60, 0, 60, 0, 80 },
                        new boolean[] { true, true, true, true, true, true, true, true })),

                // 11) Str/Dex/Def warrior build with large negative agi
                // Gaze from the Snowbank + Darkiron Aegis + Writhing Growth + Crusade Sabatons
                // + Spark of Instinct x2 + Ironleaf Bangle + Espionage + Trance
                Named.of("case11_strDexDefWarriorBuild", new TestCase(
                        "case11_strDexDefWarriorBuild",
                        new WynnItem[] {
                                new WynnItem(new int[] { 75, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Gaze
                                                                      // from
                                                                      // the
                                                                      // Snowbank
                                new WynnItem(new int[] { 0, 0, 0, 65, 0 },
                                        new int[] { 0, 10, 0, 5, 0 }), // Darkiron
                                                                       // Aegis
                                new WynnItem(new int[] { 49, 31, 0, 37, 0 },
                                        new int[] { 0, 0, 0, 0, -43 }), // Writhing
                                                                        // Growth
                                new WynnItem(new int[] { 60, 0, 0, 70, 0 },
                                        new int[] { 20, 0, 0, 25, 0 }), // Crusade
                                                                        // Sabatons
                                new WynnItem(new int[] { 0, 45, 0, 0, 0 },
                                        new int[] { 0, 6, 0, 0, 0 }), // Spark
                                                                      // of
                                                                      // Instinct
                                new WynnItem(new int[] { 0, 45, 0, 0, 0 },
                                        new int[] { 0, 6, 0, 0, 0 }), // Spark
                                                                      // of
                                                                      // Instinct
                                new WynnItem(new int[] { 45, 0, 0, 50, 0 },
                                        new int[] { 0, 0, 0, 7, 0 }), // Ironleaf
                                                                      // Bangle
                                new WynnItem(new int[] { 0, 50, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Espionage
                        // new WynnItem(new int[] { 0, 80, 0, 70, 0 }, new int[] { 0, 0, 0, 0, 0
                        // }), //
                        // Trance
                        // // (weapon)
                        },
                        new int[] { 60, 58, 0, 58, 0 },
                        new boolean[] { true, true, true, true, true, true, true, true })),

                // 12) Str-stacking build with negative agi
                // Gaze from the Snowbank + Taurus + Writhing Growth + Dawnbreak
                // + Hypoxia x2 + Diamond Fiber Bracelet + Heart of Shadow + Vengeance
                Named.of("case12_strStackingBuildWithNegativeAgi", new TestCase(
                        "case12_strStackingBuildWithNegativeAgi",
                        new WynnItem[] {
                                new WynnItem(new int[] { 75, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Gaze
                                                                      // from
                                                                      // the
                                                                      // Snowbank
                                new WynnItem(new int[] { 90, 0, 0, 0, 0 },
                                        new int[] { 15, 0, 0, 0, 0 }), // Taurus
                                new WynnItem(new int[] { 49, 31, 0, 37, 0 },
                                        new int[] { 0, 0, 0, 0, -43 }), // Writhing
                                                                        // Growth
                                new WynnItem(new int[] { 0, 65, 0, 65, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Dawnbreak
                                new WynnItem(new int[] { 0, 0, 0, 0, 0 },
                                        new int[] { 3, 3, 0, 0, 0 }), // Hypoxia
                                new WynnItem(new int[] { 0, 0, 0, 0, 0 },
                                        new int[] { 3, 3, 0, 0, 0 }), // Hypoxia
                                new WynnItem(new int[] { 100, 0, 0, 0, 0 },
                                        new int[] { 6, 0, 0, 0, 0 }), // Diamond
                                                                      // Fiber
                                                                      // Bracelet
                                new WynnItem(new int[] { 0, 40, 0, 40, 0 },
                                        new int[] { 0, 7, 0, 0, 0 }), // Heart
                                                                      // of
                                                                      // Shadow
                        // new WynnItem(new int[] { 60, 60, 0, 50, 0 }, new int[] { 0, 0, 0, 0,
                        // 0 }), //
                        // Vengeance
                        // // (weapon)
                        },
                        new int[] { 84, 52, 0, 65, 0 },
                        new boolean[] { true, true, true, true, true, true, true, true })),

                // 13) Int/Agi mage build with Moontower's large negatives
                // Aquamarine + Time Rift + Air Sanctuary + Moontower
                // + Melting Permafrost x2 + Freight Trainer + Contrast + Warp
                Named.of("case13_intAgiMageBuildWithMoontowersLargeNegatives", new TestCase(
                        "case13_intAgiMageBuildWithMoontowersLargeNegatives",
                        new WynnItem[] {
                                new WynnItem(new int[] { 40, 0, 40, 0, 0 },
                                        new int[] { 7, 0, 7, 0, 0 }), // Aquamarine
                                new WynnItem(new int[] { 0, 0, 120, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Time
                                                                      // Rift
                                new WynnItem(new int[] { 0, 0, 0, 0, 70 },
                                        new int[] { 0, 0, 0, 0, 5 }), // Air
                                                                      // Sanctuary
                                new WynnItem(new int[] { 0, 0, 70, 0, 80 },
                                        new int[] { -10, -10, 35, -40, 60 }), // Moontower
                                new WynnItem(new int[] { 0, 0, 45, 0, 45 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Melting
                                                                      // Permafrost
                                new WynnItem(new int[] { 0, 0, 45, 0, 45 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Melting
                                                                      // Permafrost
                                new WynnItem(new int[] { 0, 0, 0, 0, 50 },
                                        new int[] { 0, 0, 0, 0, 6 }), // Freight
                                                                      // Trainer
                                new WynnItem(new int[] { 0, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Contrast
                        // new WynnItem(new int[] { 0, 0, 0, 0, 125 }, new int[] { 0, 0, 0, 0,
                        // 25 }), //
                        // Warp
                        // // (weapon)
                        },
                        new int[] { 50, 0, 78, 0, 69 }, // { 57, 0, 85, 0, 80 }
                        new boolean[] { true, true, true, true, true, true, true, true })),

                // 14) Str/Int melee build with weapon needing 110 str
                // Underground + Drenched Igneous + Chain Rule + Galleon
                // + Hellion + Bygg + Enmity + Contrast + Grandmother
                Named.of("case14_strIntMeleeBuild", new TestCase(
                        "case14_strIntMeleeBuild",
                        new WynnItem[] {
                                new WynnItem(new int[] { 65, 0, 0, 0, 0 },
                                        new int[] { 10, 0, 0, 0, -5 }), // Underground
                                new WynnItem(new int[] { 50, 0, 50, 50, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Drenched
                                                                      // Igneous
                                new WynnItem(new int[] { 105, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Chain
                                                                      // Rule
                                new WynnItem(new int[] { 60, 0, 60, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Galleon
                                new WynnItem(new int[] { 0, 0, 0, 45, 0 },
                                        new int[] { 0, 0, 0, 4, 0 }), // Hellion
                                new WynnItem(new int[] { 50, 0, 0, 0, 0 },
                                        new int[] { 4, 0, 0, 2, 0 }), // Bygg
                                new WynnItem(new int[] { 60, 0, 0, 0, 0 },
                                        new int[] { 0, 4, 0, 0, 0 }), // Enmity
                                new WynnItem(new int[] { 0, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Contrast
                        // new WynnItem(new int[] { 110, 0, 0, 0, 0 }, new int[] { 15, 0, 0, 0,
                        // 55 }),
                        // // Grandmother
                        // // (weapon)
                        },
                        new int[] { 96, 0, 60, 44, 0 },
                        new boolean[] { true, true, true, true, true, true, true, true })),

                // 15) Double Diamond Hydro Rings with enough SP
                Named.of("case15_doubleDiamondHydroRings", new TestCase(
                        "case15_doubleDiamondHydroRings",
                        new WynnItem[] {
                                new WynnItem(new int[] { 0, 0, 100, 0, 0 },
                                        new int[] { 0, 0, 5, 0, 0 }), // Diamond
                                                                      // Hydro
                                                                      // Ring
                                new WynnItem(new int[] { 0, 0, 100, 0, 0 },
                                        new int[] { 0, 0, 5, 0, 0 }), // Diamond
                                                                      // Hydro
                                                                      // Ring
                        },
                        new int[] { 0, 0, 100, 0, 0 },
                        new boolean[] { true, true })),

                // 15) Double Diamond Hydro Rings without enough SP
                Named.of("case15_doubleDiamondHydroRings_fail", new TestCase(
                        "case15_doubleDiamondHydroRings_fail",
                        new WynnItem[] {
                                new WynnItem(new int[] { 0, 0, 100, 0, 0 },
                                        new int[] { 0, 0, 5, 0, 0 }), // Diamond
                                                                      // Hydro
                                                                      // Ring
                                new WynnItem(new int[] { 0, 0, 100, 0, 0 },
                                        new int[] { 0, 0, 5, 0, 0 }), // Diamond
                                                                      // Hydro
                                                                      // Ring
                        },
                        new int[] { 0, 0, 95, 0, 0 },
                        new boolean[] { false, false })),

                // 16) Str-stacking with cascading bonuses
                // Gaze from the Snowbank + Taurus + Babel + Blind Thrust
                // + Diamond Fiber Ring x2 + Momentum + Albatross + Vengeance
                Named.of("case16_strStackingWithCascadingBonuses", new TestCase(
                        "case16_strStackingWithCascadingBonuses",
                        new WynnItem[] {
                                new WynnItem(new int[] { 75, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Gaze
                                                                      // from
                                                                      // the
                                                                      // Snowbank
                                new WynnItem(new int[] { 90, 0, 0, 0, 0 },
                                        new int[] { 15, 0, 0, 0, 0 }), // Taurus
                                new WynnItem(new int[] { 75, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Babel
                                new WynnItem(new int[] { 95, 0, 0, 0, 0 },
                                        new int[] { 10, 0, 0, 0, 0 }), // Blind
                                                                       // Thrust
                                new WynnItem(new int[] { 100, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, 0, 0 }), // Diamond
                                                                      // Fiber
                                                                      // Ring
                                new WynnItem(new int[] { 100, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, 0, 0 }), // Diamond
                                                                      // Fiber
                                                                      // Ring
                                new WynnItem(new int[] { 50, 0, 0, 0, 0 },
                                        new int[] { 5, 3, 0, 0, 0 }), // Momentum
                                new WynnItem(new int[] { 0, 50, 0, 50, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Albatross
                        // new WynnItem(new int[] { 60, 60, 0, 50, 0 }, new int[] { 0, 0, 0, 0,
                        // 0 }), //
                        // Vengeance
                        // // (weapon)
                        },
                        new int[] { 85, 57, 0, 50, 0 },
                        new boolean[] { true, true, true, true, true, true, true, true })),

                // 17) Zeer-provided 1
                Named.of("case17_negDefBlocksChain", new TestCase(
                        "case17_negDefBlocksChain",
                        new WynnItem[] {
                                new WynnItem(new int[] { 75, 0, 0, 0, 0 },
                                        new int[] { 0, 0, 0, 10, 0 }), // Chestplate
                                new WynnItem(new int[] { 50, 0, 0, 50, 0 },
                                        new int[] { 9, 0, 0, 8, 0 }), // Leggings
                                new WynnItem(new int[] { 50, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, -3, 0 }), // Necklace
                        },
                        new int[] { 59, 0, 0, 40, 0 },
                        new boolean[] { false, false, true })),

                // 18) Zeer-provided 2
                // Helmet(45dex,45def→+10str+10dex+10def),
                // Chestplate(50dex,55def→+5dex-35int+5def),
                // Leggings(50str,40def→+9str+8def), Boots(65dex,65def),
                // Ring1(45str,55agi→+5agi),
                // Ring2(45str,55agi→+5agi), Bracelet(25dex→+6dex), Necklace(45agi)
                Named.of("case18_multiStatAllEquip", new TestCase(
                        "case18_multiStatAllEquip",
                        new WynnItem[] {
                                new WynnItem(new int[] { 0, 45, 0, 45, 0 },
                                        new int[] { 10, 10, 0, 10, 0 }), // Helmet
                                new WynnItem(new int[] { 0, 50, 0, 55, 0 },
                                        new int[] { 0, 5, -35, 5, 0 }), // Chestplate
                                new WynnItem(new int[] { 50, 0, 0, 40, 0 },
                                        new int[] { 9, 0, 0, 8, 0 }), // Leggings
                                new WynnItem(new int[] { 0, 65, 0, 65, 0 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Boots
                                new WynnItem(new int[] { 45, 0, 0, 0, 55 },
                                        new int[] { 0, 0, 0, 0, 5 }), // Ring 1
                                new WynnItem(new int[] { 45, 0, 0, 0, 55 },
                                        new int[] { 0, 0, 0, 0, 5 }), // Ring 2
                                new WynnItem(new int[] { 0, 25, 0, 0, 0 },
                                        new int[] { 0, 6, 0, 0, 0 }), // Bracelet
                                new WynnItem(new int[] { 0, 0, 0, 0, 45 },
                                        new int[] { 0, 0, 0, 0, 0 }), // Necklace
                        },
                        new int[] { 48, 47, 0, 45, 60 },
                        new boolean[] { true, true, true, true, true, true, true, true })),

                // 19) Zeer-provided 3
                Named.of("case19_dualRingsWithNegNecklace", new TestCase(
                        "case19_dualRingsWithNegNecklace",
                        new WynnItem[] {
                                new WynnItem(new int[] { 100, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, 0, 0 }), // Ring 1
                                new WynnItem(new int[] { 100, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, 0, 0 }), // Ring 2
                                new WynnItem(new int[] { 0, 0, 0, 0, 0 },
                                        new int[] { -3, 0, 0, 0, 0 }), // Necklace
                        },
                        new int[] { 100, 0, 0, 0, 0 },
                        new boolean[] { true, true, true })),

                // 20) Zeer-provided 4
                Named.of("case20_bothDisabledInsufficientStr", new TestCase(
                        "case20_bothDisabledInsufficientStr",
                        new WynnItem[] {
                                new WynnItem(new int[] { 40, 70, 0, 0, 0 },
                                        new int[] { 13, 0, -50, 0, 0 }), // Helmet
                                new WynnItem(new int[] { 50, 0, 0, 0, 0 },
                                        new int[] { 7, 0, 0, 0, 0 }), // Necklace
                        },
                        new int[] { 37, 70, 0, 0, 0 },
                        new boolean[] { false, false })));
    }

    // -- The parameterized test --------------------------------------------

    static Stream<org.junit.jupiter.params.provider.Arguments> algorithmAndCase() {
        return algorithms()
                .flatMap(algo -> testCases()
                        .map(tc -> org.junit.jupiter.params.provider.Arguments.of(algo, tc)));
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("algorithmAndCase")
    void testEquipResult(SkillpointChecker checker, TestCase tc) {
        String algoName = checker.getClass().getSimpleName();

        long startNs = System.nanoTime();
        boolean[] result = checker.check(tc.items, tc.assignedSkillpoints.clone());
        long elapsedNs = System.nanoTime() - startNs;

        boolean passed = Arrays.equals(tc.expectedEquippable, result);
        if (passed) {
            System.out.printf("[%s / %s] %.3f ms  PASS%n",
                    algoName, tc.name, elapsedNs / 1e6);
        } else {
            System.out.printf("[%s / %s] %.3f ms  FAIL%n    expected: %s%n    actual:   %s%n",
                    algoName, tc.name, elapsedNs / 1e6,
                    Arrays.toString(tc.expectedEquippable), Arrays.toString(result));
        }

        totalTimeNs.computeIfAbsent(algoName, k -> new AtomicLong()).addAndGet(elapsedNs);
        if (passed) {
            passes.computeIfAbsent(algoName, k -> new AtomicInteger()).incrementAndGet();
        } else {
            failures.computeIfAbsent(algoName, k -> new AtomicInteger()).incrementAndGet();
        }

        assertArrayEquals(tc.expectedEquippable, result,
                () -> "Case '" + tc.name + "': expected " + Arrays.toString(tc.expectedEquippable)
                        + " but got " + Arrays.toString(result));
    }
}
