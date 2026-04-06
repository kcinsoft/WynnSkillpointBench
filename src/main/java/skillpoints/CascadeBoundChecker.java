package skillpoints;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

public final class CascadeBoundChecker extends SkillpointChecker {
    private final Map<WynnItem[], Prepared> preparedCache = new IdentityHashMap<>();

    @Override
    public boolean[] check(WynnItem[] items, int[] assignedSkillpoints) {
        Prepared prepared = preparedCache.get(items);
        if (prepared == null) {
            prepared = Prepared.create(items);
            preparedCache.put(items, prepared);
        }

        int[] statsStack = new int[(prepared.itemCount + 1) * WynnItem.NUM_SKILLPOINTS];
        int[] needStack = new int[(prepared.itemCount + 1) * WynnItem.NUM_SKILLPOINTS];
        Arrays.fill(needStack, Integer.MIN_VALUE);
        for (int skill = 0; skill < WynnItem.NUM_SKILLPOINTS; skill++) {
            statsStack[skill] = assignedSkillpoints[skill] + prepared.freePositiveBonuses[skill];
        }

        SearchContext context = new SearchContext(
            prepared.itemCount,
            prepared.fullMask,
            prepared.requirements,
            prepared.bonuses,
            prepared.reqPlusBonus,
            prepared.itemScores,
            prepared.order,
            statsStack,
            needStack,
            prepared.freePositiveMask,
            prepared.freePositiveCount,
            prepared.freePositiveScore
        );
        context.run();

        boolean[] equipped = new boolean[prepared.itemCount];
        for (int itemIndex = 0; itemIndex < prepared.itemCount; itemIndex++) {
            equipped[itemIndex] = (context.bestMask & (1 << itemIndex)) != 0;
        }
        return equipped;
    }

    private static void sortOrder(int[] order, int[] sortKeys) {
        for (int i = 1; i < order.length; i++) {
            int current = order[i];
            int currentKey = sortKeys[current];
            int j = i - 1;
            while (j >= 0) {
                int candidate = order[j];
                int candidateKey = sortKeys[candidate];
                if (candidateKey > currentKey || (candidateKey == currentKey && candidate < current)) {
                    break;
                }
                order[j + 1] = candidate;
                j--;
            }
            order[j + 1] = current;
        }
    }

    private static final class Prepared {
        private final int itemCount;
        private final int fullMask;
        private final int[] requirements;
        private final int[] bonuses;
        private final int[] reqPlusBonus;
        private final int[] itemScores;
        private final int[] order;
        private final int freePositiveMask;
        private final int freePositiveCount;
        private final int freePositiveScore;
        private final int[] freePositiveBonuses;

        private Prepared(
            int itemCount,
            int fullMask,
            int[] requirements,
            int[] bonuses,
            int[] reqPlusBonus,
            int[] itemScores,
            int[] order,
            int freePositiveMask,
            int freePositiveCount,
            int freePositiveScore,
            int[] freePositiveBonuses
        ) {
            this.itemCount = itemCount;
            this.fullMask = fullMask;
            this.requirements = requirements;
            this.bonuses = bonuses;
            this.reqPlusBonus = reqPlusBonus;
            this.itemScores = itemScores;
            this.order = order;
            this.freePositiveMask = freePositiveMask;
            this.freePositiveCount = freePositiveCount;
            this.freePositiveScore = freePositiveScore;
            this.freePositiveBonuses = freePositiveBonuses;
        }

        private static Prepared create(WynnItem[] items) {
            int itemCount = items.length;
            int[] requirements = new int[itemCount * WynnItem.NUM_SKILLPOINTS];
            int[] bonuses = new int[itemCount * WynnItem.NUM_SKILLPOINTS];
            int[] reqPlusBonus = new int[itemCount * WynnItem.NUM_SKILLPOINTS];
            int[] itemScores = new int[itemCount];
            int[] order = new int[itemCount];
            int[] sortKeys = new int[itemCount];
            int freePositiveMask = 0;
            int freePositiveCount = 0;
            int freePositiveScore = 0;
            int[] freePositiveBonuses = new int[WynnItem.NUM_SKILLPOINTS];

            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                WynnItem item = items[itemIndex];
                int offset = itemIndex * WynnItem.NUM_SKILLPOINTS;
                int score = 0;
                boolean hasRequirement = false;
                boolean hasNegativeBonus = false;
                int requirementSum = 0;
                int positiveBonusSum = 0;
                int negativeBonusMagnitude = 0;
                int requiredStats = 0;

                for (int skill = 0; skill < WynnItem.NUM_SKILLPOINTS; skill++) {
                    int requirement = item.requirements[skill];
                    int bonus = item.bonuses[skill];
                    requirements[offset + skill] = requirement;
                    bonuses[offset + skill] = bonus;
                    reqPlusBonus[offset + skill] = requirement == 0 ? Integer.MIN_VALUE : requirement + bonus;
                    score += bonus;
                    if (requirement != 0) {
                        hasRequirement = true;
                        requirementSum += requirement;
                        requiredStats++;
                    }
                    if (bonus < 0) {
                        hasNegativeBonus = true;
                        negativeBonusMagnitude -= bonus;
                    } else {
                        positiveBonusSum += bonus;
                    }
                }

                itemScores[itemIndex] = score;
                order[itemIndex] = itemIndex;
                sortKeys[itemIndex] = (hasNegativeBonus ? 1 << 30 : 0)
                    + (requiredStats << 24)
                    + (requirementSum << 8)
                    + Math.min(255, positiveBonusSum + negativeBonusMagnitude);

                if (!hasRequirement && !hasNegativeBonus) {
                    int itemBit = 1 << itemIndex;
                    freePositiveMask |= itemBit;
                    freePositiveCount++;
                    freePositiveScore += score;
                    for (int skill = 0; skill < WynnItem.NUM_SKILLPOINTS; skill++) {
                        freePositiveBonuses[skill] += bonuses[offset + skill];
                    }
                }
            }

            sortOrder(order, sortKeys);
            return new Prepared(
                itemCount,
                (1 << itemCount) - 1,
                requirements,
                bonuses,
                reqPlusBonus,
                itemScores,
                order,
                freePositiveMask,
                freePositiveCount,
                freePositiveScore,
                freePositiveBonuses
            );
        }
    }

    private static final class SearchContext {
        private final int itemCount;
        private final int fullMask;
        private final int[] requirements;
        private final int[] bonuses;
        private final int[] reqPlusBonus;
        private final int[] itemScores;
        private final int[] order;
        private final int[] statsStack;
        private final int[] needStack;

        private int bestMask;
        private int bestCount;
        private int bestScore;

        private SearchContext(
            int itemCount,
            int fullMask,
            int[] requirements,
            int[] bonuses,
            int[] reqPlusBonus,
            int[] itemScores,
            int[] order,
            int[] statsStack,
            int[] needStack,
            int startMask,
            int startCount,
            int startScore
        ) {
            this.itemCount = itemCount;
            this.fullMask = fullMask;
            this.requirements = requirements;
            this.bonuses = bonuses;
            this.reqPlusBonus = reqPlusBonus;
            this.itemScores = itemScores;
            this.order = order;
            this.statsStack = statsStack;
            this.needStack = needStack;
            this.bestMask = startMask;
            this.bestCount = startCount;
            this.bestScore = startScore;
        }

        private void run() {
            if (bestMask != fullMask) {
                dfs(bestMask, 0, bestCount, bestScore, itemCount - bestCount);
            }
        }

        private boolean dfs(int mask, int depth, int count, int score, int remainingCount) {
            if (count > bestCount || (count == bestCount && (score > bestScore || (score == bestScore && mask < bestMask)))) {
                bestMask = mask;
                bestCount = count;
                bestScore = score;
            }
            if (mask == fullMask) {
                return true;
            }
            if (count + remainingCount < bestCount) {
                return false;
            }

            int statsOffset = depth * WynnItem.NUM_SKILLPOINTS;
            int nextStatsOffset = (depth + 1) * WynnItem.NUM_SKILLPOINTS;
            int needOffset = depth * WynnItem.NUM_SKILLPOINTS;
            int nextNeedOffset = (depth + 1) * WynnItem.NUM_SKILLPOINTS;

            for (int position = 0; position < itemCount; position++) {
                int itemIndex = order[position];
                int itemBit = 1 << itemIndex;
                if ((mask & itemBit) != 0) {
                    continue;
                }

                int itemOffset = itemIndex * WynnItem.NUM_SKILLPOINTS;
                int current0 = statsStack[statsOffset];
                int current1 = statsStack[statsOffset + 1];
                int current2 = statsStack[statsOffset + 2];
                int current3 = statsStack[statsOffset + 3];
                int current4 = statsStack[statsOffset + 4];
                if ((requirements[itemOffset] != 0 && requirements[itemOffset] > current0)
                    || (requirements[itemOffset + 1] != 0 && requirements[itemOffset + 1] > current1)
                    || (requirements[itemOffset + 2] != 0 && requirements[itemOffset + 2] > current2)
                    || (requirements[itemOffset + 3] != 0 && requirements[itemOffset + 3] > current3)
                    || (requirements[itemOffset + 4] != 0 && requirements[itemOffset + 4] > current4)) {
                    continue;
                }

                int next0 = current0 + bonuses[itemOffset];
                int next1 = current1 + bonuses[itemOffset + 1];
                int next2 = current2 + bonuses[itemOffset + 2];
                int next3 = current3 + bonuses[itemOffset + 3];
                int next4 = current4 + bonuses[itemOffset + 4];
                int need0 = Math.max(needStack[needOffset], reqPlusBonus[itemOffset]);
                int need1 = Math.max(needStack[needOffset + 1], reqPlusBonus[itemOffset + 1]);
                int need2 = Math.max(needStack[needOffset + 2], reqPlusBonus[itemOffset + 2]);
                int need3 = Math.max(needStack[needOffset + 3], reqPlusBonus[itemOffset + 3]);
                int need4 = Math.max(needStack[needOffset + 4], reqPlusBonus[itemOffset + 4]);
                if (next0 < need0 || next1 < need1 || next2 < need2 || next3 < need3 || next4 < need4) {
                    continue;
                }

                statsStack[nextStatsOffset] = next0;
                statsStack[nextStatsOffset + 1] = next1;
                statsStack[nextStatsOffset + 2] = next2;
                statsStack[nextStatsOffset + 3] = next3;
                statsStack[nextStatsOffset + 4] = next4;
                needStack[nextNeedOffset] = need0;
                needStack[nextNeedOffset + 1] = need1;
                needStack[nextNeedOffset + 2] = need2;
                needStack[nextNeedOffset + 3] = need3;
                needStack[nextNeedOffset + 4] = need4;

                int nextMask = mask | itemBit;
                int nextCount = count + 1;
                int nextScore = score + itemScores[itemIndex];
                if (dfs(nextMask, depth + 1, nextCount, nextScore, remainingCount - 1)) {
                    return true;
                }
            }
            return false;
        }
    }
}
