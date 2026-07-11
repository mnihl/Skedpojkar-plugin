package com.skedpojkar.panel;

import java.awt.BorderLayout;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * Shows a random entry from a built-in pool. The pool is hardcoded for now;
 * a bundled JSON file (or per-user custom entries) can replace it later.
 */
public class FactsPanel extends JPanel
{
	private static final String[] FACTS = {
		"Corgi means 'dwarf dog' in Welsh.",
		"Corgis were bred to herd cattle by nipping at their heels.",
		"Queen Elizabeth II owned more than 30 corgis in her lifetime.",
		"A corgi's butt is scientifically recognised as a bread loaf. (Citation needed.)",
		"Corgis have a double coat and shed roughly one extra corgi per week.",
		"According to Welsh legend, fairies rode corgis into battle.",
		"There are two corgi breeds: Pembroke and Cardigan. The Cardigan has the tail.",
		"Old School RuneScape launched in 2013 from a 2007 backup of the game.",
		"The max total level in OSRS is 2277.",
		"Gnomes invented the crossbow, according to RuneScape lore.",
		"A corgi in a party hat outranks all other players. House rule.",
		"Corgis can run up to 25 mph despite their tiny legs.",
		"Yagsipilif.",
		"Test"
	};

	private final JTextArea factArea = new JTextArea();
	private final Random random = new Random();

	public FactsPanel()
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		factArea.setLineWrap(true);
		factArea.setWrapStyleWord(true);
		factArea.setEditable(false);
		factArea.setOpaque(false);

		JButton nextButton = new JButton("Another fact!");
		nextButton.addActionListener(e -> showRandomFact());

		add(factArea, BorderLayout.CENTER);
		add(nextButton, BorderLayout.SOUTH);

		showRandomFact();
	}

	private void showRandomFact()
	{
		factArea.setText(FACTS[random.nextInt(FACTS.length)]);
	}
}
