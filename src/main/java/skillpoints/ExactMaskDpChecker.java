package skillpoints;

/**
 * Exact solver for the benchmark's equip-order semantics.
 *
 * A mask is reachable if there exists an equip order where each added item:
 * - canEquip() from the current accumulated SP, and
 * - leaves every equipped item valid() after its bonuses are applied.
 *
 * Among all reachable masks, the winner is the one with the most items, then
 * the highest total bonus sum, then the lowest mask for deterministic output.
 */
public final class ExactMaskDpChecker extends SkillpointChecker {

    @Override
    public boolean[] check(WynnItem[] items, int[] assignedSkillpoints) {
        int itemCount = items.length;
        int maskLimit = 1 << itemCount;

        int[] requirements = new int[itemCount * WynnItem.NUM_SKILLPOINTS];
        int[] bonuses = new int[itemCount * WynnItem.NUM_SKILLPOINTS];
        int[] itemScores = new int[itemCount];

        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            WynnItem item = items[itemIndex];
            int offset = itemIndex * WynnItem.NUM_SKILLPOINTS;
            int score = 0;
            for (int skill = 0; skill < WynnItem.NUM_SKILLPOINTS; skill++) {
                int requirement = item.requirements[skill];
                int bonus = item.bonuses[skill];
                requirements[offset + skill] = requirement;
                bonuses[offset + skill] = bonus;
                score += bonus;
            }
            itemScores[itemIndex] = score;
        }

        int[] maskTotals = new int[maskLimit * WynnItem.NUM_SKILLPOINTS];
        int[] maskScores = new int[maskLimit];
        byte[] bitCounts = new byte[maskLimit];
        boolean[] sustainable = new boolean[maskLimit];
        sustainable[0] = true;

        for (int mask = 1; mask < maskLimit; mask++) {
            int lowBit = mask & -mask;
            int itemIndex = Integer.numberOfTrailingZeros(lowBit);
            int previousMask = mask ^ lowBit;
            int previousOffset = previousMask * WynnItem.NUM_SKILLPOINTS;
            int currentOffset = mask * WynnItem.NUM_SKILLPOINTS;
            int itemOffset = itemIndex * WynnItem.NUM_SKILLPOINTS;

            for (int skill = 0; skill < WynnItem.NUM_SKILLPOINTS; skill++) {
                maskTotals[currentOffset + skill] = maskTotals[previousOffset + skill] + bonuses[itemOffset + skill];
            }
            maskScores[mask] = maskScores[previousMask] + itemScores[itemIndex];
            bitCounts[mask] = (byte) (bitCounts[previousMask] + 1);

            sustainable[mask] = isSustainable(mask, assignedSkillpoints, requirements, bonuses, maskTotals);
        }

        boolean[] reachable = new boolean[maskLimit];
        reachable[0] = true;

        int bestMask = 0;
        int bestCount = 0;
        int bestScore = 0;

        for (int mask = 0; mask < maskLimit; mask++) {
            if (!reachable[mask]) {
                continue;
            }

            int count = bitCounts[mask] & 0xFF;
            int score = maskScores[mask];
            if (count > bestCount || (count == bestCount && (score > bestScore || (score == bestScore && mask < bestMask)))) {
                bestMask = mask;
                bestCount = count;
                bestScore = score;
            }

            int totalsOffset = mask * WynnItem.NUM_SKILLPOINTS;
            int current0 = assignedSkillpoints[0] + maskTotals[totalsOffset];
            int current1 = assignedSkillpoints[1] + maskTotals[totalsOffset + 1];
            int current2 = assignedSkillpoints[2] + maskTotals[totalsOffset + 2];
            int current3 = assignedSkillpoints[3] + maskTotals[totalsOffset + 3];
            int current4 = assignedSkillpoints[4] + maskTotals[totalsOffset + 4];

            for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
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

                int nextMask = mask | itemBit;
                if (sustainable[nextMask]) {
                    reachable[nextMask] = true;
                }
            }
        }

        boolean[] result = new boolean[itemCount];
        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            result[itemIndex] = (bestMask & (1 << itemIndex)) != 0;
        }
        return result;
    }

    private static boolean isSustainable(
        int mask,
        int[] assignedSkillpoints,
        int[] requirements,
        int[] bonuses,
        int[] maskTotals
    ) {
        int totalsOffset = mask * WynnItem.NUM_SKILLPOINTS;
        int current0 = assignedSkillpoints[0] + maskTotals[totalsOffset];
        int current1 = assignedSkillpoints[1] + maskTotals[totalsOffset + 1];
        int current2 = assignedSkillpoints[2] + maskTotals[totalsOffset + 2];
        int current3 = assignedSkillpoints[3] + maskTotals[totalsOffset + 3];
        int current4 = assignedSkillpoints[4] + maskTotals[totalsOffset + 4];

        for (int itemIndex = 0; (1 << itemIndex) <= mask; itemIndex++) {
            int itemBit = 1 << itemIndex;
            if ((mask & itemBit) == 0) {
                continue;
            }
            int itemOffset = itemIndex * WynnItem.NUM_SKILLPOINTS;
            if ((requirements[itemOffset] != 0 && requirements[itemOffset] + bonuses[itemOffset] > current0)
                || (requirements[itemOffset + 1] != 0 && requirements[itemOffset + 1] + bonuses[itemOffset + 1] > current1)
                || (requirements[itemOffset + 2] != 0 && requirements[itemOffset + 2] + bonuses[itemOffset + 2] > current2)
                || (requirements[itemOffset + 3] != 0 && requirements[itemOffset + 3] + bonuses[itemOffset + 3] > current3)
                || (requirements[itemOffset + 4] != 0 && requirements[itemOffset + 4] + bonuses[itemOffset + 4] > current4)) {
                return false;
            }
        }
        return true;
    }
}
