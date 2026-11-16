package com.skedpojkar.clicker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class NumberTextTest
{
	/**
	 * The thousand separator depends on the computer's language settings
	 * (a comma in English, a space in Swedish), so the tests compare digits only.
	 */
	private static String digits(String text)
	{
		return text.replaceAll("[^0-9]", "");
	}

	@Test
	public void wholeNumbersKeepAllTheirDigits()
	{
		assertEquals("0", NumberText.format(0));
		assertEquals("999", NumberText.format(999));
		assertEquals("1234567", digits(NumberText.format(1_234_567)));
		assertEquals(9, NumberText.format(1_234_567).length()); // two separators
	}

	@Test
	public void fractionsAreDropped()
	{
		assertEquals("12", NumberText.format(12.9));
	}

	@Test
	public void numbersBeyondALongStillShowFullDigits()
	{
		// 2^70 is exact as a double and far above Long.MAX_VALUE (~9.2e18)
		String text = NumberText.format(Math.pow(2, 70));
		assertEquals("1180591620717411303424", digits(text));
		assertFalse(text.contains("e"));
		assertFalse(text.contains("E"));
	}

	@Test
	public void theLastAscensionCostsAreReadable()
	{
		// The final Ascend cost after three ascensions, about 1.15e19
		String text = NumberText.format(Math.floor(9.18e16 * 125));
		assertEquals(20, digits(text).length());
	}

	@Test
	public void smallRatesKeepOneDecimal()
	{
		assertEquals("2", NumberText.formatRate(2));
		assertEquals("5", digits(NumberText.formatRate(0.5)).substring(1));
		assertEquals("1500", digits(NumberText.formatRate(1_500)));
	}
}
