package skillpoints;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class WynnAlgorithm extends SkillpointChecker {

	private WynnItem[] cachedItems = new WynnItem[0];
	private int[] cachedMasks = new int[0];
	
	private void generateMasks(WynnItem[] items) {
		boolean refresh = true;
		if (items.length == cachedItems.length) {
			refresh = false;
			for (int i = 0; i < items.length; ++i) {
				if (items[i].equals(cachedItems[i])) continue;
				refresh = true;
				break;
			}
		}
		if (refresh) {
			int num_masks = 1 << items.length;
			cachedItems = items;
			cachedMasks = IntStream.range(0, num_masks).toArray();
		}
	}

	@Override
	public boolean[] check(WynnItem[] items, int[] assignedSkillpoints) {
		assert(assignedSkillpoints.length == WynnItem.NUM_SKILLPOINTS);

		List<WynnItem> positives = new ArrayList<WynnItem>();
		List<WynnItem> negatives = new ArrayList<WynnItem>();
		// Track the positives and negatives back to item indices
		List<Integer> positiveIndex = new ArrayList<Integer>();
		List<Integer> negativeIndex = new ArrayList<Integer>();
		for (int itemIndex = 0; itemIndex < items.length; ++itemIndex) {
			WynnItem item = items[itemIndex];
			boolean added = false;
			for (int i = 0; i < WynnItem.NUM_SKILLPOINTS; ++i) {
				if (item.bonuses[i] < 0) {
					negatives.add(item);
					negativeIndex.add(itemIndex);
					added = true;
					break;
				}
			}
			if (!added) {
				positives.add(item);
				positiveIndex.add(itemIndex);
			}
		}
		
		generateMasks(negatives.toArray(new WynnItem[0]));
		boolean changed;


		int bestCount = 0;
		int bestWeight = 0;
		boolean[] bestResult = null;
		boolean[] baseResult = new boolean[items.length];

		
		// Total skillpoints. Interpreted from implicit assigned skillpoints
		// in zeer's writeup
		int[] baseSkillpoints = assignedSkillpoints.clone();
		
		// bonus skillpoints only.
		int[] positiveBonuses = new int[WynnItem.NUM_SKILLPOINTS];
		int basePositiveWeight = 0;
		
		boolean[] baseActivePositive = new boolean[positives.size()];

		// LABEL 1 apply positive greedy
		do {
			changed = false;
			for (int i = 0; i < positives.size(); ++i) {
				if (baseActivePositive[i]) continue;
				WynnItem item = positives.get(i);
				if (!item.canEquip(baseSkillpoints)) continue;
				
				baseActivePositive[i] = true;
				changed = true;
				// Save the greedy positives as valid
				baseResult[positiveIndex.get(i)] = true;

				item.apply(baseSkillpoints);
				item.apply(positiveBonuses);
				basePositiveWeight += IntStream.of(item.bonuses).sum();

			}
		} while (changed);
		bestResult = baseResult;

		// LABEL 2 apply negative sets
		maskloop:
		for (int mask : cachedMasks) {
			int numNegative = Integer.bitCount(mask);
			int maxCount = numNegative + positives.size();
			if (maxCount < bestCount) continue;
			
			// valid/invalid not kept, just store the result as an array
			boolean[] result = baseResult.clone();
			WynnItem[] negativeSet = new WynnItem[numNegative];
			int[] negativeBonuses = new int[WynnItem.NUM_SKILLPOINTS];
			{
				int i = 0;
				int j = 0;
				for (int iterMask = mask; iterMask != 0; iterMask >>>= 1) {
					if ((iterMask & 1) == 1) {
						negativeSet[j] = negatives.get(i);
						negatives.get(i).apply(negativeBonuses);
						// Preemptively adding negative results here, to be cleaner
						result[negativeIndex.get(i)] = true;
						j += 1;
					}
					i += 1;
				}
			}
			
			int optimisticWeight = IntStream.of(negativeBonuses).sum()
					+ basePositiveWeight;
			if (maxCount == bestCount
					&& optimisticWeight <= bestWeight) continue;
			
			boolean[] activePositive = baseActivePositive.clone();
			boolean[] activeNegative = new boolean[negativeSet.length];
			int finalWeight = basePositiveWeight;
			
			// Difference: Skip the positive application loop, just clone
			int[] skillpoints = baseSkillpoints.clone();

			// LABEL 3 Apply loop
			do {
				changed = false;
				
				// Try applying negative items.
				negativeApplyLoop:
				for (int i = 0; i < negativeSet.length; ++i) {
					if (activeNegative[i]) continue;
					WynnItem item = negativeSet[i];
					if (!item.canEquip(skillpoints)) continue;
					
					// Make sure after negative item application that all
					//   positive items are still valid.
					int[] saveSkillpoints = skillpoints.clone();
					item.apply(skillpoints);
					for (int j = 0; j < activePositive.length; ++j) {
						if (!activePositive[j]) continue;
						if (positives.get(j).isValid(skillpoints)) continue;
						
						// Compressed: inavlidatesPositive to just
						// go to next iteration of the outer loop
						skillpoints = saveSkillpoints;
						continue negativeApplyLoop;
					}
					
					// applied!
					activeNegative[i] = true;
					changed = true;
					finalWeight += IntStream.of(item.bonuses).sum();
				}
				
				// Try applying positive items.
				for (int i = 0; i < positives.size(); ++i) {
					if (activePositive[i]) continue;
					WynnItem item = positives.get(i);
					if (!item.canEquip(skillpoints)) continue;
					
					// applied!
					item.apply(skillpoints);
					activePositive[i] = true;
					changed = true;
					finalWeight += IntStream.of(item.bonuses).sum();
				}
			} while (changed);
			
			// LABEL 4: Final checks
			for (boolean b : activeNegative) {
				if (!b) continue maskloop;
			}

			for (int i = 0; i < positives.size(); ++i) {
				if (!activePositive[i]) continue;
				WynnItem item = positives.get(i);
				if (!item.isValid(skillpoints)) {
					continue maskloop;
				}
			}

			for (int i = 0; i < negativeSet.length; ++i) {
				if (!negativeSet[i].isValid(skillpoints)) continue maskloop;
			}
			
			// LABEL 5: Clean up and present result
			int activeCount = numNegative;
			for (int i = 0; i < positives.size(); ++i) {
				if (!activePositive[i]) continue;
				result[positiveIndex.get(i)] = true;
				activeCount += 1;
			}
			
			if (bestCount > activeCount) continue;
			if (bestCount == activeCount && bestWeight > finalWeight) continue;
			
			bestCount = activeCount;
			bestWeight = finalWeight;
			bestResult = result;
		}
		
		return bestResult;
	}
}