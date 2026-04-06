package skillpoints;

import java.util.Arrays;
import java.util.List;

public class Main {
	public static void main(String[] args) {
		WynnItem[] items0 = new WynnItem[]{
			new WynnItem(new int[]{ 50,  0,  0, 40,  0},	// earth breaker
					     new int[]{  9,  0,  0,  8,  0}),
			new WynnItem(new int[]{ 75,  0,  0,  0,  0},	// Granitic Mettle
				         new int[]{  0,  0,  0, 15,  0}),
			new WynnItem(new int[]{ 50,  0,  0,  0,  0},	// Shatterglass
			             new int[]{  7,  0,  0, -3,  0}),
		};
		int[] skillpoints0 = new int[] {59, 0, 0, 43, 0};
//		WynnItem[] items = new WynnItem[]{
//		new WynnItem(new int[]{ 40,  0,  0, 40, 40},	// Valhalla
//				     new int[]{  9,  0,  0,  9,  9}),
//		new WynnItem(new int[]{  0, 15,  0,  0, 50},	// Dark Shroud
//				     new int[]{  0, 15,  0,  0, 25}),
//		new WynnItem(new int[]{ 30, 30, 30, 30, 30},	// Far Cosmos
//			     	 new int[]{  8,  8,  8,  8,  8}),
//		new WynnItem(new int[]{ 40, 70,  0,  0,  0},	// Brainwash
//			         new int[]{ 13,  0,-50,  0,  0}),
//		new WynnItem(new int[]{ 25,  0,  0,  0,  0},	// Giant's Ring
//	                 new int[]{  5,  0,  0,  0, -3}),
//		new WynnItem(new int[]{ 25, 25, 25, 25, 25},	// Prism
//                     new int[]{  3,  3,  3,  3,  3}),
//		new WynnItem(new int[]{  0,  0,  0,  0,  0},	// Prowess
//                     new int[]{  4,  4,  4,  4,  4}),
//		new WynnItem(new int[]{ 50,  0,  0,  0,  0},	// Shatterglass
//		             new int[]{  7,  0,  0, -3,  0}),
//		};
//		int[] skillpoints = new int[] {21, 40, 73, 28, 29};
//		WynnItem[] items = new WynnItem[]{	// This case gives failures for wynn algorithm, currently.
//											// The only valid equip order is a -> b -> c.
//		new WynnItem(new int[]{  1,  0,  0,  0,  0},	// a
//				     new int[]{  0,  2, -1,  0,  0}),
//		new WynnItem(new int[]{  0,  2,  0,  0,  0},	// b
//				     new int[]{  0,  0,  1,  0,  0}),
//		new WynnItem(new int[]{  0,  0,  1,  0,  0},	// c
//	     	 	 	 new int[]{  0,  0,  0,  1,  0}),
//		};
//		int[] skillpoints = new int[] {1, 0, 1, 0, 0};
		WynnItem[] items = new WynnItem[]{	// This case gives failures for wynn algorithm, currently.
				// The only valid equip order is a -> b -> c.
		new WynnItem(new int[]{  1,  0,  1,  0,  0},	// a
					 new int[]{  3,  0,  3,  0,  0}),
		new WynnItem(new int[]{  0,  0,  4,  0,  0},	// b
					 new int[]{ -1,  0,  0,  0,  0}),
		};
		int[] skillpoints = new int[] {1, 0, 1, 0, 0};

//		SkillpointChecker solver = new WynnAlgorithm();
//		SCCGraphAlgorithm solver = new SCCGraphAlgorithm();
//		solver.check(items0, skillpoints0);
		OptimizedDFS.solve(Arrays.asList(items0), skillpoints0);

		List<WynnItem> l = Arrays.asList(items);
		long l1 = System.nanoTime();
		int[] result = OptimizedDFS.solve(l, skillpoints);
//		boolean[] equipOK = solver.check(items, skillpoints);

		long l2 = System.nanoTime();
		System.out.println("Solve took " + ((l2-l1) / 1e6) + " ms");
//		System.out.println("SCC Graph construction took " + solver.ctx.res.elapsedNS / 1000000.0 + " ms");
//		int[] result = solver.ctx.bestOrder;
//		System.out.println(solver.ctx.evals + " evals.");
//		for (boolean b : equipOK) {
//			System.out.println(b);
//		}
		for (int i : result) {
			System.out.println(i);
		}
	}
}
