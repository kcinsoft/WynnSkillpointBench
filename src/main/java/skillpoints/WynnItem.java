package skillpoints;

import java.util.Arrays;

public class WynnItem {
	public static final int NUM_SKILLPOINTS = 5;
	
	public int[] requirements;
	public int[] bonuses;

	/**
	 * Construct an item with the given requirements and skillpoint bonuses.
	 * 
	 * @param requirements
	 * @param bonuses
	 */
	public WynnItem(int[] requirements, int[] bonuses) {
		assert(requirements.length == NUM_SKILLPOINTS);
		assert(bonuses.length == NUM_SKILLPOINTS);
		this.requirements = requirements;
		this.bonuses = bonuses;
	}
	
	// For iteration
	public int[] requirements() { return requirements; }
	public int[] bonuses() { return bonuses; }
	
	public boolean canEquip(int[] currentSkillpoints) {
		for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
			// NOTE: cannot be > 0. Thanks to crafted items
			if (requirements[i] != 0) {
				if (requirements[i] > currentSkillpoints[i]) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean isValid(int[] currentSkillpoints) {
		for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
			// NOTE: cannot be > 0. Thanks to crafted items
			if (requirements[i] != 0) {
				if (requirements[i] + bonuses[i] > currentSkillpoints[i]) {
					return false;
				}
			}
		}
		return true;
	}
	
	public void apply(int[] skillpoints) {
		for (int i = 0; i < NUM_SKILLPOINTS; ++i) {
			skillpoints[i] += this.bonuses[i];
		}
	}
	
	public boolean equals(WynnItem other) {
		return Arrays.equals(requirements, other.requirements)
				&& Arrays.equals(bonuses, other.bonuses);
	}
	
	public String toString() {
		return "Reqs: " + Arrays.toString(requirements) + " Bonus: " + Arrays.toString(bonuses);
	}
}

/**
  best = EMPTY
  
  positiveSkills = zeroed skill array
  basePositiveWeight = 0
  baseActivePositive = [false for each positive item]
  
  skillPoints = zeroed skill array

LABEL 1
  repeat:
      changed = false
  
      for each positive item i:
          if baseActivePositive[i]:
              continue
  
          if positive[i].tryEquip() is not VALID:
              continue
  
          baseActivePositive[i] = true
          changed = true
  
          apply positive[i].skills into skillPoints
          apply positive[i].skills into positiveSkills
          basePositiveWeight += positive[i].skills.weight
  
  until changed is false

LABEL 2
  for each negativeMask in MASK_CACHE(negative.size):
      negativeCount = bitCount(negativeMask)
      maxCount = negativeCount + positive.size
  
      if best.items > maxCount:
          break
  
      valid = []
      invalid = []
  
      negativeItems = array sized negativeCount
      negativeSkills = zeroed skill array
  
      fill negativeItems from negativeMask
      add unselected negative items to invalid
      apply each selected negative item's skills into negativeSkills
  
      optimisticWeight = sum(positiveSkills + negativeSkills)
      if maxCount == best.items and optimisticWeight <= best.weight:
          continue
  
      activePositive = copy(baseActivePositive)
      activeNegative = [false for each selected negative item]
      finalWeight = basePositiveWeight
  
      skillPoints = zeroed skill array
      for each positive item i:
          if activePositive[i]:
              apply positive[i].skills into skillPoints
LABEL 3: Apply loop
      repeat:
          changed = false
  
          for each selected negative item i:
              if activeNegative[i]:
                  continue
  
              if negativeItems[i].tryEquip() is not VALID:
                  continue
  
              previousSkillPoints = copy(skillPoints)
              apply negativeItems[i].skills into skillPoints
  
              invalidatesPositive = false
              for each positive item j:
                  if not activePositive[j]:
                      continue
  
                  if testEquip(positive[j]) is VALID:
                      continue
  
                  invalidatesPositive = true
                  break
  
              if invalidatesPositive:
                  skillPoints = previousSkillPoints
                  continue
  
              activeNegative[i] = true
              changed = true
              finalWeight += negativeItems[i].skills.weight
  
          for each positive item i:
              if activePositive[i]:
                  continue
  
              if positive[i].tryEquip() is not VALID:
                  continue
  
              activePositive[i] = true
              changed = true
  
              apply positive[i].skills into skillPoints
              finalWeight += positive[i].skills.weight
  
      until changed is false

// LABEL 4: Final checks
      if any selected negative item is still inactive:
          continue
  
      for each positive item i:
          if not activePositive[i]:
              invalid.add(positive[i])
              continue
  
          if testEquip(positive[i]) is not VALID:
              continue outer loop
  
      for each selected negative item i:
          if testEquip(negativeItems[i]) is not VALID:
              continue outer loop

// LABEL 5: Clean up and present result
      add all active selected negative items to valid
      add all active positive items to valid
  
      if best.items > valid.size:
          continue
  
      if best.items == valid.size and best.weight > finalWeight:
          continue
  
      best = Combination(finalWeight, valid, invalid)
  
 */