package skillpoints;

public class GreedyAlgorithm extends SkillpointChecker {
    private boolean meetsReqs(int[] sp, int[] reqs) {
        return (sp[0] >= reqs[0] || reqs[0] == 0) &&
                (sp[1] >= reqs[1] || reqs[1] == 0) &&
                (sp[2] >= reqs[2] || reqs[2] == 0) &&
                (sp[3] >= reqs[3] || reqs[3] == 0) &&
                (sp[4] >= reqs[4] || reqs[4] == 0);
    }

    private int[] applyBonuses(int[] sp, int[] bonuses) {
        return new int[]{
                sp[0] + bonuses[0],
                sp[1] + bonuses[1],
                sp[2] + bonuses[2],
                sp[3] + bonuses[3],
                sp[4] + bonuses[4]
        };
    }

    private int[] updateMinimums(int[] minimums, WynnItem item) {
        int[] reqs = {item.requirements[0], item.requirements[1], item.requirements[2], item.requirements[3], item.requirements[4]};
        for (int s = 0; s < 5; s++) {
            if (item.requirements[s] > 0 && item.bonuses[s] > 0) {
                reqs[s] += item.bonuses[s];
            }
        }
        return new int[]{
                Math.max(minimums[0], reqs[0]),
                Math.max(minimums[1], reqs[1]),
                Math.max(minimums[2], reqs[2]),
                Math.max(minimums[3], reqs[3]),
                Math.max(minimums[4], reqs[4])
        };
    }

    @Override
    public boolean[] check(WynnItem[] items, int[] assignedSkillpoints) {
        boolean[] output = new boolean[items.length];
        boolean[] hasNegative = new boolean[items.length];
        int[] sp = {assignedSkillpoints[0], assignedSkillpoints[1], assignedSkillpoints[2], assignedSkillpoints[3], assignedSkillpoints[4]};
        int[] minimums = {0, 0, 0, 0, 0};
        // Input pre-processing
        for (int i = 0; i < items.length; i++) {
            WynnItem item = items[i];
            boolean hasRequirements = false;
            for (int s = 0; s < 5; s++) {
                if (item.requirements[s] > 0) {
                    hasRequirements = true;
                    break;
                }
            }
            hasNegative[i] = false;
            for (int s = 0; s < 5; s++) {
                if (item.bonuses[s] < 0) {
                    hasNegative[i] = true;
                    break;
                }
            }
            if (!hasNegative[i] && !hasRequirements) {
                output[i] = true;
                sp = applyBonuses(sp, item.bonuses);
            } else {
                output[i] = false;
            }
        }

        while (true) {
            int best = -1;
            int bestScore = -1;
            for (int i = 0; i < items.length; i++) {
                if (output[i]) continue;
                WynnItem item = items[i];
                if (meetsReqs(sp, item.requirements)) {
                    int[] post = applyBonuses(sp, item.bonuses);
                    if (!meetsReqs(post, minimums)) continue;
                    int score = 0;
                    for (int j = 0; j < items.length; j++) {
                        if (i == j || output[j]) continue;
                        if (meetsReqs(post, items[j].requirements)) score++;
                    }
                    if (score > bestScore) {
                        bestScore = score;
                        best = i;
                    }
                }
            }
            if (best == -1) break;
            output[best] = true;
            sp = applyBonuses(sp, items[best].bonuses);
            minimums = updateMinimums(minimums, items[best]);
        }

        return output;
    }
}
