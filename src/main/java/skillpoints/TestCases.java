package skillpoints;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared test-case data used by both JUnit tests and JMH benchmarks.
 */
public final class TestCases {

    public record TestCase(
            String name,
            WynnItem[] items,
            int[] assignedSkillpoints,
            boolean[] expectedEquippable) {
    }

    /** All test cases, keyed by name. Insertion-ordered. */
    public static final Map<String, TestCase> ALL = new LinkedHashMap<>();

    static {
        add("case1_optimal",
                new WynnItem[] {
                        new WynnItem(new int[] { 50, 0, 0, 40, 0 }, new int[] { 9, 0, 0, 8, 0 }),
                        new WynnItem(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 10, 0 }),
                        new WynnItem(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 }),
                },
                new int[] { 68, 0, 0, 33, 0 },
                new boolean[] { true, true, true });

        add("case1_subopt_assign",
                new WynnItem[] {
                        new WynnItem(new int[] { 50, 0, 0, 40, 0 }, new int[] { 9, 0, 0, 8, 0 }),
                        new WynnItem(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 10, 0 }),
                        new WynnItem(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 }),
                },
                new int[] { 59, 0, 0, 43, 0 },
                new boolean[] { true, true, true });

        add("case1_tff",
                new WynnItem[] {
                        new WynnItem(new int[] { 50, 0, 0, 40, 0 }, new int[] { 9, 0, 0, 8, 0 }),
                        new WynnItem(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 10, 0 }),
                        new WynnItem(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 }),
                },
                new int[] { 59, 0, 0, 40, 0 },
                new boolean[] { true, false, false });

        add("case2_strictChain_abc",
                new WynnItem[] {
                        new WynnItem(new int[] { 1, 0, 0, 0, 0 }, new int[] { 0, 2, -1, 0, 0 }),
                        new WynnItem(new int[] { 0, 2, 0, 0, 0 }, new int[] { 0, 0, 1, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 1, 0, 0 }, new int[] { 0, 0, 0, 1, 0 }),
                },
                new int[] { 1, 0, 1, 0, 0 },
                new boolean[] { true, true, true });

        add("case2_strictChain_cab",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 0, 1, 0, 0 }, new int[] { 0, 0, 0, 1, 0 }),
                        new WynnItem(new int[] { 1, 0, 0, 0, 0 }, new int[] { 0, 2, -1, 0, 0 }),
                        new WynnItem(new int[] { 0, 2, 0, 0, 0 }, new int[] { 0, 0, 1, 0, 0 }),
                },
                new int[] { 1, 0, 1, 0, 0 },
                new boolean[] { true, true, true });

        add("case3_noRequirements",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 0, 0, 0, 0 }, new int[] { 5, 5, 5, 5, 5 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 0 }, new int[] { -2, 0, 0, 0, 0 }),
                },
                new int[] { 0, 0, 0, 0, 0 },
                new boolean[] { true, true });

        add("case4_impossibleRequirements",
                new WynnItem[] {
                        new WynnItem(new int[] { 100, 0, 0, 0, 0 }, new int[] { 5, 0, 0, 0, 0 }),
                },
                new int[] { 10, 0, 0, 0, 0 },
                new boolean[] { false });

        add("case5_negativeInvalidatesPrior",
                new WynnItem[] {
                        new WynnItem(new int[] { 10, 0, 0, 0, 0 }, new int[] { 5, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 0 }, new int[] { -20, 0, 0, 0, 0 }),
                },
                new int[] { 10, 0, 0, 0, 0 },
                new boolean[] { true, false });

        add("case6_exactRequirement",
                new WynnItem[] {
                        new WynnItem(new int[] { 50, 30, 0, 0, 0 }, new int[] { 0, 0, 10, 0, 0 }),
                },
                new int[] { 50, 30, 0, 0, 0 },
                new boolean[] { true });

        add("case7_mutualDependency",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 10, 0, 0, 0 }, new int[] { 10, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 10, 0, 0, 0, 0 }, new int[] { 0, 10, 0, 0, 0 }),
                },
                new int[] { 0, 0, 0, 0, 0 },
                new boolean[] { false, false });

        add("case8_fullBuild_8items",
                new WynnItem[] {
                        new WynnItem(new int[] { 40, 0, 0, 40, 40 }, new int[] { 9, 0, 0, 9, 9 }),
                        new WynnItem(new int[] { 0, 15, 0, 0, 50 }, new int[] { 0, 15, 0, 0, 25 }),
                        new WynnItem(new int[] { 30, 30, 30, 30, 30 }, new int[] { 8, 8, 8, 8, 8 }),
                        new WynnItem(new int[] { 40, 70, 0, 0, 0 }, new int[] { 13, 0, -50, 0, 0 }),
                        new WynnItem(new int[] { 25, 0, 0, 0, 0 }, new int[] { 5, 0, 0, 0, -3 }),
                        new WynnItem(new int[] { 25, 25, 25, 25, 25 }, new int[] { 3, 3, 3, 3, 3 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 0 }, new int[] { 4, 4, 4, 4, 4 }),
                        new WynnItem(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 }),
                },
                new int[] { 21, 40, 73, 28, 29 },
                new boolean[] { true, true, true, true, true, true, true, true });

        add("case9_dexIntAgiBuild",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 40, 40, 0, 40 }, new int[] { -30, 0, 0, -30, 0 }),
                        new WynnItem(new int[] { 0, 50, 0, 0, 65 }, new int[] { 0, 8, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 50, 55, 0, 50 }, new int[] { 0, 6, 4, 0, 6 }),
                        new WynnItem(new int[] { 0, 55, 0, 0, 55 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 50, 0, 0, 45 }, new int[] { 0, 4, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 60, 0, 0, 0 }, new int[] { 0, 0, 6, 0, 0 }),
                        new WynnItem(new int[] { 0, 45, 45, 0, 45 }, new int[] { -15, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 45, 0, 0, 0 }, new int[] { 0, 8, 0, 0, 0 }),
                },
                new int[] { 0, 45, 49, 0, 65 },
                new boolean[] { true, true, true, true, true, true, true, true });

        add("case10_intAgiHeavyMageBuild",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, -80, 5, 0, 0 }),
                        new WynnItem(new int[] { 50, 0, 65, 0, 50 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 50, 0, 55, 0, 0 }, new int[] { 6, 0, 6, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 70, 0, 80 }, new int[] { -10, -10, 35, -40, 60 }),
                        new WynnItem(new int[] { 0, 0, 55, 0, 0 }, new int[] { 0, 0, 4, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 65, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 6, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 55, 0, 0 }, new int[] { 0, 0, 4, 0, 0 }),
                },
                new int[] { 60, 0, 60, 0, 80 },
                new boolean[] { true, true, true, true, true, true, true, true });

        add("case11_strDexDefWarriorBuild",
                new WynnItem[] {
                        new WynnItem(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 65, 0 }, new int[] { 0, 10, 0, 5, 0 }),
                        new WynnItem(new int[] { 49, 31, 0, 37, 0 }, new int[] { 0, 0, 0, 0, -43 }),
                        new WynnItem(new int[] { 60, 0, 0, 70, 0 }, new int[] { 20, 0, 0, 25, 0 }),
                        new WynnItem(new int[] { 0, 45, 0, 0, 0 }, new int[] { 0, 6, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 45, 0, 0, 0 }, new int[] { 0, 6, 0, 0, 0 }),
                        new WynnItem(new int[] { 45, 0, 0, 50, 0 }, new int[] { 0, 0, 0, 7, 0 }),
                        new WynnItem(new int[] { 0, 50, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                },
                new int[] { 60, 58, 0, 58, 0 },
                new boolean[] { true, true, true, true, true, true, true, true });

        add("case12_strStackingBuildWithNegativeAgi",
                new WynnItem[] {
                        new WynnItem(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 90, 0, 0, 0, 0 }, new int[] { 15, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 49, 31, 0, 37, 0 }, new int[] { 0, 0, 0, 0, -43 }),
                        new WynnItem(new int[] { 0, 65, 0, 65, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 0 }, new int[] { 3, 3, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 0 }, new int[] { 3, 3, 0, 0, 0 }),
                        new WynnItem(new int[] { 100, 0, 0, 0, 0 }, new int[] { 6, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 40, 0, 40, 0 }, new int[] { 0, 7, 0, 0, 0 }),
                },
                new int[] { 84, 52, 0, 65, 0 },
                new boolean[] { true, true, true, true, true, true, true, true });

        add("case13_intAgiMageBuildWithMoontowersLargeNegatives",
                new WynnItem[] {
                        new WynnItem(new int[] { 40, 0, 40, 0, 0 }, new int[] { 7, 0, 7, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 120, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 70 }, new int[] { 0, 0, 0, 0, 5 }),
                        new WynnItem(new int[] { 0, 0, 70, 0, 80 }, new int[] { -10, -10, 35, -40, 60 }),
                        new WynnItem(new int[] { 0, 0, 45, 0, 45 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 45, 0, 45 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 50 }, new int[] { 0, 0, 0, 0, 6 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                },
                new int[] { 50, 0, 78, 0, 69 },
                new boolean[] { true, true, true, true, true, true, true, true });

        add("case14_strIntMeleeBuild",
                new WynnItem[] {
                        new WynnItem(new int[] { 65, 0, 0, 0, 0 }, new int[] { 10, 0, 0, 0, -5 }),
                        new WynnItem(new int[] { 50, 0, 50, 50, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 105, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 60, 0, 60, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 45, 0 }, new int[] { 0, 0, 0, 4, 0 }),
                        new WynnItem(new int[] { 50, 0, 0, 0, 0 }, new int[] { 4, 0, 0, 2, 0 }),
                        new WynnItem(new int[] { 60, 0, 0, 0, 0 }, new int[] { 0, 4, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                },
                new int[] { 96, 0, 60, 44, 0 },
                new boolean[] { true, true, true, true, true, true, true, true });

        add("case15_doubleDiamondHydroRings",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 5, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 5, 0, 0 }),
                },
                new int[] { 0, 0, 100, 0, 0 },
                new boolean[] { true, true });

        add("case15_doubleDiamondHydroRings_fail",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 5, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 100, 0, 0 }, new int[] { 0, 0, 5, 0, 0 }),
                },
                new int[] { 0, 0, 95, 0, 0 },
                new boolean[] { false, false });

        add("case16_strStackingWithCascadingBonuses",
                new WynnItem[] {
                        new WynnItem(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 90, 0, 0, 0, 0 }, new int[] { 15, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 95, 0, 0, 0, 0 }, new int[] { 10, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 100, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 100, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 50, 0, 0, 0, 0 }, new int[] { 5, 3, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 50, 0, 50, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                },
                new int[] { 85, 57, 0, 50, 0 },
                new boolean[] { true, true, true, true, true, true, true, true });

        add("case17_negDefBlocksChain",
                new WynnItem[] {
                        new WynnItem(new int[] { 75, 0, 0, 0, 0 }, new int[] { 0, 0, 0, 10, 0 }),
                        new WynnItem(new int[] { 50, 0, 0, 50, 0 }, new int[] { 9, 0, 0, 8, 0 }),
                        new WynnItem(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, -3, 0 }),
                },
                new int[] { 59, 0, 0, 40, 0 },
                new boolean[] { false, false, true });

        add("case18_multiStatAllEquip",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 45, 0, 45, 0 }, new int[] { 10, 10, 0, 10, 0 }),
                        new WynnItem(new int[] { 0, 50, 0, 55, 0 }, new int[] { 0, 5, -35, 5, 0 }),
                        new WynnItem(new int[] { 50, 0, 0, 40, 0 }, new int[] { 9, 0, 0, 8, 0 }),
                        new WynnItem(new int[] { 0, 65, 0, 65, 0 }, new int[] { 0, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 45, 0, 0, 0, 55 }, new int[] { 0, 0, 0, 0, 5 }),
                        new WynnItem(new int[] { 45, 0, 0, 0, 55 }, new int[] { 0, 0, 0, 0, 5 }),
                        new WynnItem(new int[] { 0, 25, 0, 0, 0 }, new int[] { 0, 6, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 45 }, new int[] { 0, 0, 0, 0, 0 }),
                },
                new int[] { 48, 47, 0, 45, 60 },
                new boolean[] { true, true, true, true, true, true, true, true });

        add("case19_dualRingsWithNegNecklace",
                new WynnItem[] {
                        new WynnItem(new int[] { 100, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 100, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 }),
                        new WynnItem(new int[] { 0, 0, 0, 0, 0 }, new int[] { -3, 0, 0, 0, 0 }),
                },
                new int[] { 100, 0, 0, 0, 0 },
                new boolean[] { true, true, true });

        add("case20_bothDisabledInsufficientStr",
                new WynnItem[] {
                        new WynnItem(new int[] { 40, 70, 0, 0, 0 }, new int[] { 13, 0, -50, 0, 0 }),
                        new WynnItem(new int[] { 50, 0, 0, 0, 0 }, new int[] { 7, 0, 0, 0, 0 }),
                },
                new int[] { 37, 70, 0, 0, 0 },
                new boolean[] { false, false });

        add("case21_repurposedVessels",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 45, 0, 45, 0 }, new int[] { 30, -3, -3, -3, -3 }),
                },
                new int[] { 0, 48, 0, 48, 0 },
                new boolean[] { true });

        add("case21_repurposedVessels_fail",
                new WynnItem[] {
                        new WynnItem(new int[] { 0, 45, 0, 45, 0 }, new int[] { 30, -3, -3, -3, -3 }),
                },
                new int[] { 0, 45, 0, 45, 0 },
                new boolean[] { true });
    }

    private static void add(String name, WynnItem[] items, int[] sp, boolean[] expected) {
        ALL.put(name, new TestCase(name, items, sp, expected));
    }

    private TestCases() {
    }
}
