package skillpoints;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

public final class CascadeBoundChecker extends SkillpointChecker {
    private final Map<WynnItem[], Prepared> preparedCache = new IdentityHashMap<>();
    private int[] statsStack = new int[0];
    private int[] needStack = new int[0];
    private boolean[] visitedMask = new boolean[0];

    @Override
    public boolean[] check(WynnItem[] items, int[] assignedSkillpoints) {
        Prepared prepared = preparedCache.get(items);
        if (prepared == null) {
            prepared = Prepared.create(items);
            preparedCache.put(items, prepared);
        }

        int stackLength = (prepared.itemCount + 1) * WynnItem.NUM_SKILLPOINTS;
        if (statsStack.length < stackLength) {
            statsStack = new int[stackLength];
            needStack = new int[stackLength];
        }
        if (visitedMask.length < prepared.maskLimit) {
            visitedMask = new boolean[prepared.maskLimit];
        }
        Arrays.fill(needStack, 0, stackLength, Integer.MIN_VALUE);
        Arrays.fill(visitedMask, 0, prepared.maskLimit, false);
        for (int skill = 0; skill < WynnItem.NUM_SKILLPOINTS; skill++) {
            statsStack[skill] = assignedSkillpoints[skill];
        }

        SearchContext context = new SearchContext(
            prepared.itemCount,
            prepared.fullMask,
            prepared.requirements,
            prepared.bonuses,
            prepared.reqPlusBonus,
            prepared.itemScores,
            prepared.forcedOrder,
            prepared.forcedItemCount,
            prepared.branchOrder,
            prepared.branchItemCount,
            statsStack,
            needStack,
            visitedMask
        );
        context.run();

        boolean[] equipped = new boolean[prepared.itemCount];
        for (int itemIndex = 0; itemIndex < prepared.itemCount; itemIndex++) {
            equipped[itemIndex] = (context.bestMask & (1 << itemIndex)) != 0;
        }
        return equipped;
    }

    private static void sortOrder(int[] order, int[] sortKeys, int length) {
        for (int i = 1; i < length; i++) {
            int current = order[i];
            int currentKey = sortKeys[i];
            int j = i - 1;
            while (j >= 0) {
                int candidateKey = sortKeys[j];
                if (candidateKey > currentKey || (candidateKey == currentKey && order[j] < current)) {
                    break;
                }
                order[j + 1] = order[j];
                sortKeys[j + 1] = candidateKey;
                j--;
            }
            order[j + 1] = current;
            sortKeys[j + 1] = currentKey;
        }
    }

    private static final class Prepared {
        private final int itemCount;
        private final int fullMask;
        private final int[] requirements;
        private final int[] bonuses;
        private final int[] reqPlusBonus;
        private final int[] itemScores;
        private final int[] forcedOrder;
        private final int forcedItemCount;
        private final int[] branchOrder;
        private final int branchItemCount;
        private final int maskLimit;

        private Prepared(
            int itemCount,
            int fullMask,
            int[] requirements,
            int[] bonuses,
            int[] reqPlusBonus,
            int[] itemScores,
            int[] forcedOrder,
            int forcedItemCount,
            int[] branchOrder,
            int branchItemCount,
            int maskLimit
        ) {
            this.itemCount = itemCount;
            this.fullMask = fullMask;
            this.requirements = requirements;
            this.bonuses = bonuses;
            this.reqPlusBonus = reqPlusBonus;
            this.itemScores = itemScores;
            this.forcedOrder = forcedOrder;
            this.forcedItemCount = forcedItemCount;
            this.branchOrder = branchOrder;
            this.branchItemCount = branchItemCount;
            this.maskLimit = maskLimit;
        }

        private static Prepared create(WynnItem[] items) {
            int itemCount = items.length;
            int[] requirements = new int[itemCount * WynnItem.NUM_SKILLPOINTS];
            int[] bonuses = new int[itemCount * WynnItem.NUM_SKILLPOINTS];
            int[] reqPlusBonus = new int[itemCount * WynnItem.NUM_SKILLPOINTS];
            int[] itemScores = new int[itemCount];
            int[] forcedOrder = new int[itemCount];
            int[] forcedSortKeys = new int[itemCount];
            int forcedItemCount = 0;
            int[] branchOrder = new int[itemCount];
            int[] branchSortKeys = new int[itemCount];
            int branchItemCount = 0;
            boolean[] riskySkill = new boolean[WynnItem.NUM_SKILLPOINTS];

            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                WynnItem item = items[itemIndex];
                for (int skill = 0; skill < WynnItem.NUM_SKILLPOINTS; skill++) {
                    if (item.bonuses[skill] < 0) {
                        riskySkill[skill] = true;
                    }
                }
            }

            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                WynnItem item = items[itemIndex];
                int offset = itemIndex * WynnItem.NUM_SKILLPOINTS;
                int score = 0;
                boolean hasNegativeBonus = false;
                boolean requiresRiskySkill = false;
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
                        requirementSum += requirement;
                        requiredStats++;
                        if (riskySkill[skill]) {
                            requiresRiskySkill = true;
                        }
                    }
                    if (bonus < 0) {
                        hasNegativeBonus = true;
                        negativeBonusMagnitude -= bonus;
                    } else {
                        positiveBonusSum += bonus;
                    }
                }

                itemScores[itemIndex] = score;
                int sortKey = (requiredStats << 24)
                    + (requirementSum << 8)
                    + Math.min(255, positiveBonusSum + negativeBonusMagnitude);
                if (hasNegativeBonus || requiresRiskySkill) {
                    branchOrder[branchItemCount] = itemIndex;
                    branchSortKeys[branchItemCount] = sortKey + (1 << 30);
                    branchItemCount++;
                } else {
                    forcedOrder[forcedItemCount] = itemIndex;
                    forcedSortKeys[forcedItemCount] = sortKey;
                    forcedItemCount++;
                }
            }

            sortOrder(forcedOrder, forcedSortKeys, forcedItemCount);
            sortOrder(branchOrder, branchSortKeys, branchItemCount);
            return new Prepared(
                itemCount,
                (1 << itemCount) - 1,
                requirements,
                bonuses,
                reqPlusBonus,
                itemScores,
                forcedOrder,
                forcedItemCount,
                branchOrder,
                branchItemCount,
                1 << itemCount
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
        private final int[] forcedOrder;
        private final int forcedItemCount;
        private final int[] branchOrder;
        private final int branchItemCount;
        private final int[] statsStack;
        private final int[] needStack;
        private final boolean[] visitedMask;

        private int bestMask;
        private int bestCount;
        private int bestScore;
        private int closureMask;
        private int closureCount;
        private int closureScore;

        private SearchContext(
            int itemCount,
            int fullMask,
            int[] requirements,
            int[] bonuses,
            int[] reqPlusBonus,
            int[] itemScores,
            int[] forcedOrder,
            int forcedItemCount,
            int[] branchOrder,
            int branchItemCount,
            int[] statsStack,
            int[] needStack,
            boolean[] visitedMask
        ) {
            this.itemCount = itemCount;
            this.fullMask = fullMask;
            this.requirements = requirements;
            this.bonuses = bonuses;
            this.reqPlusBonus = reqPlusBonus;
            this.itemScores = itemScores;
            this.forcedOrder = forcedOrder;
            this.forcedItemCount = forcedItemCount;
            this.branchOrder = branchOrder;
            this.branchItemCount = branchItemCount;
            this.statsStack = statsStack;
            this.needStack = needStack;
            this.visitedMask = visitedMask;
        }

        private void run() {
            applyForcedClosure(0, 0, 0, 0);
            bestMask = closureMask;
            bestCount = closureCount;
            bestScore = closureScore;
            if (closureMask != fullMask) {
                dfs(closureMask, 0, closureCount, closureScore);
            }
        }

        private boolean dfs(int mask, int depth, int count, int score) {
            if (count > bestCount || (count == bestCount && (score > bestScore || (score == bestScore && mask < bestMask)))) {
                bestMask = mask;
                bestCount = count;
                bestScore = score;
            }
            if (mask == fullMask) {
                return true;
            }
            if (visitedMask[mask]) {
                return false;
            }
            visitedMask[mask] = true;

            int statsOffset = depth * WynnItem.NUM_SKILLPOINTS;
            int nextStatsOffset = (depth + 1) * WynnItem.NUM_SKILLPOINTS;
            int needOffset = depth * WynnItem.NUM_SKILLPOINTS;
            int nextNeedOffset = (depth + 1) * WynnItem.NUM_SKILLPOINTS;

            for (int position = 0; position < branchItemCount; position++) {
                int itemIndex = branchOrder[position];
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

                applyForcedClosure(mask | itemBit, depth + 1, count + 1, score + itemScores[itemIndex]);
                if (dfs(closureMask, depth + 1, closureCount, closureScore)) {
                    return true;
                }
            }
            return false;
        }

        private void applyForcedClosure(int mask, int depth, int count, int score) {
            int currentOffset = depth * WynnItem.NUM_SKILLPOINTS;
            boolean changed;
            do {
                changed = false;
                int current0 = statsStack[currentOffset];
                int current1 = statsStack[currentOffset + 1];
                int current2 = statsStack[currentOffset + 2];
                int current3 = statsStack[currentOffset + 3];
                int current4 = statsStack[currentOffset + 4];
                int need0 = needStack[currentOffset];
                int need1 = needStack[currentOffset + 1];
                int need2 = needStack[currentOffset + 2];
                int need3 = needStack[currentOffset + 3];
                int need4 = needStack[currentOffset + 4];

                for (int position = 0; position < forcedItemCount; position++) {
                    int itemIndex = forcedOrder[position];
                    int itemBit = 1 << itemIndex;
                    if ((mask & itemBit) != 0) {
                        continue;
                    }

                    int itemOffset = itemIndex * WynnItem.NUM_SKILLPOINTS;
                    if ((requirements[itemOffset] != 0 && requirements[itemOffset] > current0)
                        || (requirements[itemOffset + 1] != 0 && requirements[itemOffset + 1] > current1)
                        || (requirements[itemOffset + 2] != 0 && requirements[itemOffset + 2] > current2)
                        || (requirements[itemOffset + 3] != 0 && requirements[itemOffset + 3] > current3)
                        || (requirements[itemOffset + 4] != 0 && requirements[itemOffset + 4] > current4)) {
                        continue;
                    }

                    current0 += bonuses[itemOffset];
                    current1 += bonuses[itemOffset + 1];
                    current2 += bonuses[itemOffset + 2];
                    current3 += bonuses[itemOffset + 3];
                    current4 += bonuses[itemOffset + 4];
                    need0 = Math.max(need0, reqPlusBonus[itemOffset]);
                    need1 = Math.max(need1, reqPlusBonus[itemOffset + 1]);
                    need2 = Math.max(need2, reqPlusBonus[itemOffset + 2]);
                    need3 = Math.max(need3, reqPlusBonus[itemOffset + 3]);
                    need4 = Math.max(need4, reqPlusBonus[itemOffset + 4]);
                    mask |= itemBit;
                    count++;
                    score += itemScores[itemIndex];
                    changed = true;
                }

                statsStack[currentOffset] = current0;
                statsStack[currentOffset + 1] = current1;
                statsStack[currentOffset + 2] = current2;
                statsStack[currentOffset + 3] = current3;
                statsStack[currentOffset + 4] = current4;
                needStack[currentOffset] = need0;
                needStack[currentOffset + 1] = need1;
                needStack[currentOffset + 2] = need2;
                needStack[currentOffset + 3] = need3;
                needStack[currentOffset + 4] = need4;
            } while (changed);
            closureMask = mask;
            closureCount = count;
            closureScore = score;
        }
    }
}
