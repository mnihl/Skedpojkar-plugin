package com.skedpojkar.announce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for the text matching behind the clan-broadcast and secret-PM
 * triggers. The player names here are made up; matching never depends on them.
 */
public class ChatMatchingTest
{
	// ---- Clan broadcasts ----

	@Test
	public void beingDefeatedIsADeathNotAKill()
	{
		assertEquals(ClanBroadcast.DEATH,
			ClanBroadcast.classify("Player One has been defeated by Player Two in The Wilderness."));
		assertEquals(ClanBroadcast.DEATH, ClanBroadcast.classify("Player One has died."));
	}

	@Test
	public void defeatingSomeoneIsAKill()
	{
		assertEquals(ClanBroadcast.KILL,
			ClanBroadcast.classify("Player One has defeated Player Two and received (1,000 coins) worth of loot!"));
	}

	@Test
	public void receivingADropIsADrop()
	{
		assertEquals(ClanBroadcast.DROP,
			ClanBroadcast.classify("Player One received a drop: Abyssal whip (1,500,000 coins)."));
	}

	@Test
	public void otherBroadcastsAreIgnored()
	{
		assertEquals(ClanBroadcast.OTHER, ClanBroadcast.classify("Player One has reached Runecraft level 99."));
		assertEquals(ClanBroadcast.OTHER, ClanBroadcast.classify(""));
	}

	// ---- Secret PM sequence ----

	@Test
	public void theSequenceCompletesInOrder()
	{
		PmSequence sequence = new PmSequence("one", "two", "three");
		assertFalse(sequence.advance("one"));
		assertFalse(sequence.advance("two"));
		assertTrue(sequence.advance("three"));
	}

	@Test
	public void matchingIgnoresCaseAndSurroundingSpaces()
	{
		PmSequence sequence = new PmSequence("one", "two");
		assertFalse(sequence.advance("  ONE "));
		assertTrue(sequence.advance("Two"));
	}

	@Test
	public void aWrongMessageStartsOver()
	{
		PmSequence sequence = new PmSequence("one", "two", "three");
		sequence.advance("one");
		sequence.advance("two");
		assertFalse(sequence.advance("hello"));
		assertFalse("progress was lost", sequence.advance("three"));
	}

	@Test
	public void repeatingTheFirstPhraseCountsAsAFreshStart()
	{
		PmSequence sequence = new PmSequence("one", "two", "three");
		sequence.advance("one");
		sequence.advance("two");
		assertFalse(sequence.advance("one"));
		assertFalse(sequence.advance("two"));
		assertTrue(sequence.advance("three"));
	}

	@Test
	public void partOfAPhraseIsNotEnough()
	{
		PmSequence sequence = new PmSequence("one", "two");
		assertFalse(sequence.advance("one two"));
		assertFalse(sequence.advance("two"));
	}

	@Test
	public void theSequenceCanBeCompletedAgain()
	{
		PmSequence sequence = new PmSequence("one", "two");
		sequence.advance("one");
		assertTrue(sequence.advance("two"));
		sequence.advance("one");
		assertTrue(sequence.advance("two"));
	}

	@Test
	public void resetForgetsProgress()
	{
		PmSequence sequence = new PmSequence("one", "two");
		sequence.advance("one");
		sequence.reset();
		assertFalse(sequence.advance("two"));
	}
}
