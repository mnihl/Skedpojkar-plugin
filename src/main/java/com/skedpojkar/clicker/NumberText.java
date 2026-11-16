package com.skedpojkar.clicker;

import java.math.BigDecimal;

/**
 * Turns Runeclicker's numbers into text for the panel. Kept apart from the
 * panel so it can be unit-tested.
 */
public final class NumberText
{
	private NumberText()
	{
	}

	/**
	 * A whole number with all its digits and thousand separators
	 * ("1,000,000"), no suffixes and no scientific notation, however large.
	 * Fractions are dropped.
	 */
	public static String format(double value)
	{
		if (Double.isNaN(value) || Double.isInfinite(value))
		{
			return String.valueOf(value);
		}
		// A long holds up to ~9.2 quintillion; prestige costs pass that after
		// a few ascensions, so bigger values go through BigInteger, which has
		// no upper limit.
		if (Math.abs(value) < Long.MAX_VALUE)
		{
			return String.format("%,d", (long) value);
		}
		return String.format("%,d", new BigDecimal(value).toBigInteger());
	}

	/** Like {@link #format}, but small values keep one decimal ("0.5", "2", "1,500"). */
	public static String formatRate(double value)
	{
		if (value >= 1_000)
		{
			return format(value);
		}
		return value == Math.floor(value)
			? String.valueOf((long) value)
			: String.format("%.1f", value);
	}
}
