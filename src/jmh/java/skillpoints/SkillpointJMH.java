package skillpoints;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmark for skillpoint algorithms.
 *
 * Run all: ./gradlew jmh
 * Filter: ./gradlew jmh -Pbm=".*WynnAlgorithm.*"
 * Results in: build/results/jmh/results.txt
 */
// Note: these defaults apply when running the jar directly.
// ./gradlew jmh overrides them via build.gradle's jmh {} block.
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Thread)
public class SkillpointJMH {

    // ── Parameters (JMH enumerates all combinations) ─────────────────────

    @Param({
            "WynnAlgorithm",
            "SCCGraphAlgorithm",
            "OptimizedDFS",
            "WynnSolver",
            "CascadeBound",
            "GreedyAlgorithm",
            "MyFirstAlgorithm",
            "MySecondAlgorithm",
    })
    String algoName;

    @Param({
            "case1_optimal",
            "case1_subopt_assign",
            "case1_tff",
            "case2_strictChain_abc",
            "case3_noRequirements",
            "case4_impossibleRequirements",
            "case5_negativeInvalidatesPrior",
            "case6_exactRequirement",
            "case7_mutualDependency",
            "case8_fullBuild_8items",
            "case9_dexIntAgiBuild",
            "case10_intAgiHeavyMageBuild",
            "case11_strDexDefWarriorBuild",
            "case12_strStackingBuildWithNegativeAgi",
            "case13_intAgiMageBuildWithMoontowersLargeNegatives",
            "case14_strIntMeleeBuild",
            "case15_doubleDiamondHydroRings",
            "case15_doubleDiamondHydroRings_fail",
            "case16_strStackingWithCascadingBonuses",
            "case17_negDefBlocksChain",
            "case18_multiStatAllEquip",
            "case19_dualRingsWithNegNecklace",
            "case20_bothDisabledInsufficientStr"
    })
    String caseName;

    // ── Resolved state ───────────────────────────────────────────────────

    private SkillpointChecker checker;
    private WynnItem[] baseItems;
    private int[] baseAssignedSkillpoints;
    private WynnItem[] benchmarkItems;
    private int[] benchmarkAssignedSkillpoints;

    @Setup(Level.Trial)
    public void setup() {
        checker = switch (algoName) {
            case "WynnAlgorithm" -> new WynnAlgorithm();
            case "SCCGraphAlgorithm" -> new SCCGraphAlgorithm();
            case "OptimizedDFS" -> new OptimizedDFSChecker();
            case "WynnSolver" -> new WynnSolverAlgorithm();
            case "CascadeBound" -> new CascadeBoundChecker();
            case "GreedyAlgorithm" -> new GreedyAlgorithm();
            case "MyFirstAlgorithm" -> new MyFirstAlgorithm();
            case "MySecondAlgorithm" -> new MySecondAlgorithm();
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algoName);
        };

        var tc = TestCases.ALL.get(caseName);
        if (tc == null)
            throw new IllegalArgumentException("Unknown test case: " + caseName);
        baseItems = tc.items();
        baseAssignedSkillpoints = tc.assignedSkillpoints();
        if (!(checker instanceof GreedyAlgorithm)) {
            benchmarkItems = SkillpointTest.cloneItems(baseItems);
            benchmarkAssignedSkillpoints = baseAssignedSkillpoints.clone();
        }
    }

    @Benchmark
    public boolean[] bench(Blackhole bh) {
        boolean[] result;
        if (checker instanceof GreedyAlgorithm) {
            result = checker.check(
                SkillpointTest.cloneItems(baseItems),
                baseAssignedSkillpoints.clone()
            );
        } else {
            checker.clearCache();
            result = checker.check(benchmarkItems, benchmarkAssignedSkillpoints);
        }
        bh.consume(result);
        return result;
    }
}
