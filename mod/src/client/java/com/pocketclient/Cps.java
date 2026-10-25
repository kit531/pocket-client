package com.pocketclient;

import java.util.ArrayDeque;

/** Counts mouse clicks over the last second. Touch taps arrive as mouse clicks on Zalith/Pojav. */
public final class Cps {
	private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();

	private Cps() {
	}

	public static void click(int button) {
		long now = System.currentTimeMillis();
		if (button == 0) {
			LEFT.addLast(now);
		} else if (button == 1) {
			RIGHT.addLast(now);
		}
	}

	public static int left() {
		return count(LEFT);
	}

	public static int right() {
		return count(RIGHT);
	}

	private static int count(ArrayDeque<Long> clicks) {
		long cutoff = System.currentTimeMillis() - 1000L;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) {
			clicks.pollFirst();
		}
		return clicks.size();
	}
}
