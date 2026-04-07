package skillpoints;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmark simulating realistic item-by-item equipping.
 *
 * For each full-build (8-item) test case, generates 8 seeded random
 * permutations of equip order. Each benchmark invocation runs all 8
 * permutations; for each permutation, items are added one at a time
 * (8 incremental check() calls). Cache is preserved within a permutation
 * but cleared between permutations.
 *
 * This models the real scenario: a player equips items one at a time,
 * and the algorithm reruns on each equip. Adding a new build case is
 * just adding an 8-item entry to TestCases.java and listing it here.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class EquipSequenceJMH {

    private static final int NUM_PERMUTATIONS = 8;
    private static final long SEED = 42L;

    // ── Parameters ──────────────────────────────────────────────────────

    @Param({
            "WynnAlgorithm",
            "SCCGraphAlgorithm",
            "WynnSolver",
            "CascadeBound",
            "MyFirstAlgorithm",
            "MySecondAlgorithm",
            "TheThirdAlgorithm",
            "OurSecondAlgorithm",
    })
    String algoName;

    @Param({
            "case8_fullBuild_8items",
            "case9_dexIntAgiBuild",
            "case10_intAgiHeavyMageBuild",
            "case11_strDexDefWarriorBuild",
            "case12_strStackingBuildWithNegativeAgi",
            "case13_intAgiMageBuildWithMoontowersLargeNegatives",
            "case14_strIntMeleeBuild",
            "case16_strStackingWithCascadingBonuses",
            "case18_multiStatAllEquip",
    })
    String caseName;

    // ── Resolved state ──────────────────────────────────────────────────

    private SkillpointChecker checker;
    private int[] assignedSkillpoints;
    private boolean needsClone;

    /**
     * Pre-built incremental item arrays.
     * permutationSteps[perm][step] = the items array for check() at that step.
     * Step k contains items order[0..k] (k+1 items total).
     */
    private WynnItem[][][] permutationSteps;

    @Setup(Level.Trial)
    public void setup() {
        checker = switch (algoName) {
            case "WynnAlgorithm" -> new WynnAlgorithm();
            case "SCCGraphAlgorithm" -> new SCCGraphAlgorithm();
            case "WynnSolver" -> new WynnSolverAlgorithm();
            case "CascadeBound" -> new CascadeBoundChecker();
            case "MyFirstAlgorithm" -> new MyFirstAlgorithm();
            case "MySecondAlgorithm" -> new MySecondAlgorithm();
            case "TheThirdAlgorithm" -> new TheThirdAlgorithm();
            case "OurSecondAlgorithm" -> new OurSecondAlgorithm();
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algoName);
        };
        needsClone = checker instanceof GreedyAlgorithm;

        var tc = TestCases.ALL.get(caseName);
        if (tc == null)
            throw new IllegalArgumentException("Unknown test case: " + caseName);

        WynnItem[] items = tc.items();
        assignedSkillpoints = tc.assignedSkillpoints();
        int n = items.length;

        // Generate seeded random permutations
        Random rng = new Random(SEED);
        permutationSteps = new WynnItem[NUM_PERMUTATIONS][][];

        for (int p = 0; p < NUM_PERMUTATIONS; p++) {
            // Fisher-Yates shuffle
            int[] order = new int[n];
            for (int i = 0; i < n; i++)
                order[i] = i;
            for (int i = n - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int tmp = order[i];
                order[i] = order[j];
                order[j] = tmp;
            }

            // Build incremental arrays: step k has items order[0..k]
            permutationSteps[p] = new WynnItem[n][];
            for (int step = 0; step < n; step++) {
                WynnItem[] stepItems = new WynnItem[step + 1];
                for (int k = 0; k <= step; k++) {
                    WynnItem orig = items[order[k]];
                    stepItems[k] = new WynnItem(orig.requirements.clone(), orig.bonuses.clone());
                }
                permutationSteps[p][step] = stepItems;
            }
        }
    }

    @Benchmark
    public void bench(Blackhole bh) {
        for (int p = 0; p < NUM_PERMUTATIONS; p++) {
            checker.clearCache();
            WynnItem[][] steps = permutationSteps[p];
            for (int step = 0; step < steps.length; step++) {
                WynnItem[] items;
                int[] sp;
                if (needsClone) {
                    items = SkillpointTest.cloneItems(steps[step]);
                    sp = assignedSkillpoints.clone();
                } else {
                    items = steps[step];
                    sp = assignedSkillpoints;
                }
                bh.consume(checker.check(items, sp));
            }
        }
    }
}
