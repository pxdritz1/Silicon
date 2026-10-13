package silicon.px.debug;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Face and quad counts of every section Silicon drew, for the F3 line.
 */
public final class Stats {
	private static final Map<Long, long[]> SECTIONS = new ConcurrentHashMap<>();

	private static long[] totals = new long[4];
	private static long summedAt;

	private Stats() {
	}

	/**
	 * @param vertexBytes size of one vertex in the renderer's format, to turn quad counts into memory
	 */
	public static void record(long section, int faces, int quads, int vertexBytes) {
		SECTIONS.put(section, new long[]{faces, quads, vertexBytes});
	}

	public static void forget(long section) {
		SECTIONS.remove(section);
	}

	public static void clear() {
		SECTIONS.clear();
	}

	/**
	 * @return faces, quads, vertex bytes without Silicon, vertex bytes with Silicon
	 */
	public static long[] totals() {
		long now = System.currentTimeMillis();
		if (now - summedAt > 1000) {
			long[] t = new long[4];
			for (long[] s : SECTIONS.values()) {
				t[0] += s[0];
				t[1] += s[1];
				t[2] += s[0] * 4 * s[2];
				t[3] += s[1] * 4 * s[2];
			}
			totals = t;
			summedAt = now;
		}
		return totals;
	}
}
