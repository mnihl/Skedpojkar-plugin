package com.skedpojkar.achievements;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Every achievement in the plugin. Secret ones display as "???" until earned.
 * The enum name is the persisted id — rename entries and everyone loses them.
 */
@Getter
@RequiredArgsConstructor
public enum Achievement
{
	// Runeclicker
	POINTS_10K("Pocket change", "Earn 10,000 lifetime Runeclicker points", false),
	POINTS_1M("Point millionaire", "Earn 1,000,000 lifetime points", false),
	POINTS_1B("Billionaire boys club", "Earn 1,000,000,000 lifetime points", false),
	POINTS_1T("Trillion? In this economy?", "Earn 1,000,000,000,000 lifetime points", false),
	CLICKS_1K("Carpal crafter", "Click the rune 1,000 times", false),
	FIRST_PRESTIGE("New rune, who dis", "Prestige for the first time", false),
	NATURE_TIER("Profit at last", "Reach Nature runes", false),
	WRATH_TIER("The end of the ladder", "Reach Wrath runes", false),
	FIRST_ASCENSION("Runespan ascendant", "Ascend the Runespan", false),
	GOLDEN_10("Golden collector", "Catch 10 golden runes", false),
	GOLDEN_50("Golden hoarder", "Catch 50 golden runes", false),
	BUTTERFINGERS("Butterfingers", "Let 10 golden runes slip away", false),
	CRIT_100("Critical mass", "Land 100 critical clicks", false),
	MAXED_POUCHES("Bottomless pockets", "Own every pouch at once", false),
	BIG_SPENDER("Big spender", "Own 50 of a single upgrade", false),
	FULLY_ATTUNED("Fully attuned", "Buy Runespan attunement three times", false),

	// The rest of the plugin
	TOLL_DODGER("Toll dodger", "Pass the Al Kharid gate 10 times", false),
	FLOOR_5_ENJOYER("Floor 5 enjoyer", "Complete floor 5 of the Hallowed Sepulchre", false),
	CORRUPTED("Corrupted", "Complete the Corrupted Gauntlet", false),
	PVP_MENACE("PvP menace", "Defeat 10 players", false),
	WHIFF_MASTER("Whiff master", "Hit 25 zeroes on other players", false),
	HOMEOWNERS_SHAME("Homeowner's shame", "Get judged for your house amenities", false),
	BROKIE_CERTIFIED("Brokie certified", "Receive the low bank value warning", false),
	THE_SEQUENCE("The sequence", "You know what you did", true),
	TACTICIAN("Tactician", "Beat the tic-tac-toe AI", false),
	UNDEFEATED("Undefeated", "Win 5 party tic-tac-toe games", false),
	SCHOLAR("Scholar", "Read 50 facts", false);

	private final String displayName;
	private final String description;
	private final boolean secret;
}
