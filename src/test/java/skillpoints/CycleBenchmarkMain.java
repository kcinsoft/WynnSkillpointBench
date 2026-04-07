package skillpoints;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Named;

public final class CycleBenchmarkMain {
    private static final int DEFAULT_WARMUP_ITERATIONS = 200;
    private static final int DEFAULT_MEASURE_ITERATIONS = 20_000;

    private CycleBenchmarkMain() {
    }

    public static void main(String[] args) {
        String algorithmFilter = args.length > 0 ? args[0] : System.getProperty("skillpoints.algorithm", "");
        int warmupIterations = Integer.getInteger("skillpoints.warmup", DEFAULT_WARMUP_ITERATIONS);
        int measureIterations = Integer.getInteger("skillpoints.iterations", DEFAULT_MEASURE_ITERATIONS);

        List<Named<SkillpointChecker>> algorithms = SkillpointTest.algorithms().toList();
        List<Named<TestCases.TestCase>> cases = SkillpointTest.testCases().toList();

        List<Named<SkillpointChecker>> selectedAlgorithms = new ArrayList<>();
        for (Named<SkillpointChecker> algorithm : algorithms) {
            if (algorithmFilter.isEmpty() || algorithm.getName().equals(algorithmFilter)) {
                selectedAlgorithms.add(algorithm);
            }
        }
        if (selectedAlgorithms.isEmpty()) {
            throw new IllegalArgumentException("unknown algorithm filter: " + algorithmFilter);
        }

        for (Named<SkillpointChecker> algorithm : selectedAlgorithms) {
            long checksum = runAlgorithm(algorithm.getPayload(), cases, warmupIterations, measureIterations);
            System.out.printf(
                "algorithm=%s warmup=%d iterations=%d cases=%d checksum=%d%n",
                algorithm.getName(),
                warmupIterations,
                measureIterations,
                cases.size(),
                checksum
            );
        }
    }

    private static long runAlgorithm(
        SkillpointChecker checker,
        List<Named<TestCases.TestCase>> cases,
        int warmupIterations,
        int measureIterations
    ) {
        WynnItem[][] itemsByCase = new WynnItem[cases.size()][];
        int[][] assignedByCase = new int[cases.size()][];
        for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
            TestCases.TestCase testCase = cases.get(caseIndex).getPayload();
            itemsByCase[caseIndex] = SkillpointTest.cloneItems(testCase.items());
            assignedByCase[caseIndex] = testCase.assignedSkillpoints().clone();
        }

        long checksum = 0;
        for (int iteration = 0; iteration < warmupIterations; iteration++) {
            checksum += runCases(checker, itemsByCase, assignedByCase);
        }
        for (int iteration = 0; iteration < measureIterations; iteration++) {
            checksum += runCases(checker, itemsByCase, assignedByCase);
        }
        return checksum;
    }

    private static long runCases(SkillpointChecker checker, WynnItem[][] itemsByCase, int[][] assignedByCase) {
        long checksum = 0;
        for (int caseIndex = 0; caseIndex < itemsByCase.length; caseIndex++) {
            boolean[] result = runOnce(checker, itemsByCase, assignedByCase, caseIndex);
            checksum = checksum * 31 + encode(result);
        }
        return checksum;
    }

    private static boolean[] runOnce(
        SkillpointChecker checker,
        WynnItem[][] itemsByCase,
        int[][] assignedByCase,
        int caseIndex
    ) {
        if (checker instanceof GreedyAlgorithm) {
            return checker.check(
                SkillpointTest.cloneItems(itemsByCase[caseIndex]),
                assignedByCase[caseIndex].clone()
            );
        }
        return checker.check(itemsByCase[caseIndex], assignedByCase[caseIndex]);
    }

    private static int encode(boolean[] values) {
        int mask = 0;
        for (int index = 0; index < values.length; index++) {
            if (values[index]) {
                mask |= 1 << index;
            }
        }
        return mask;
    }
}
