/**
 * CITS3001 Project: Implement an agent for the card game Hanabi
 * The University of Western Australis
 * @author Anh Tuan Hoang (Student ID: 27149914)
 * @author Joshua Ng (Student ID: 20163079)
 */
package agents;

import java.util.Stack;
import java.util.Random;

import hanabAI.Action;
import hanabAI.ActionType;
import hanabAI.Agent;
import hanabAI.Card;
import hanabAI.Colour;
import hanabAI.IllegalActionException;
import hanabAI.State;

/**
 * An implentation of Piers' agent for the Hanabi card game 
 * from the paper "Evaluating and Modelling Hanabi-Playing Agents".
 * Retrieved from: http://teaching.csse.uwa.edu.au/units/CITS3001/project/2018/papers/MCTSHanabi.pdf
 * The University of Western Australia
 * @author Anh Tuan Hoang (Student ID: 27149914)
 * @author Joshua Ng (Student ID: 20163079)
 */
public class Agent21749914 implements Agent {
	
	private int numPlayers;
	private Perspective[] history;
	private int numCard;
	private boolean firstAction = true;
	private int agentIndex;
	private int numCardRemaining = 50;
	private Colour[] colourArray = new Colour[]{Colour.BLUE, Colour.GREEN, Colour.RED, Colour.WHITE, Colour.YELLOW};
	

	@Override
	public Action doAction(State s)
	{
		Action a = null;
		int fuseTokens = s.getFuseTokens();
		int hintTokens = s.getHintTokens();
		if(firstAction)
		{
			init(s);
		}
		try{
			updatePerspective(s);

			if(fuseTokens > 1 && numCardRemaining == 0)
			{
				a = playProbablySafeCard(s, 0.0);
				if(a!=null) return a;
			}

			a = playSafeCard(s);
			if(a != null) return a;

			if(fuseTokens > 1)
			{
				a = playProbablySafeCard(s, 0.6);
				if(a != null) return a;
			}

			if(hintTokens > 0)
			{
				a = tellAnyoneAboutUsefulCard(s);
				if(a != null) return a;
			}

			if(hintTokens > 0 && hintTokens < 4)
			{
				a = tellDispensible(s);
				if(a != null) return a;
			}

			if(hintTokens < 8)
			{
				a = osawaDiscard(s);
				if(a != null) return a;
			}

			if(hintTokens < 8)
			{
				a = discardOldestFirst(s);
				if(a != null) return a;
			}

			if(hintTokens > 0)
			{
				a = tellRandomly(s);
				if(a != null) return a;
			}

			if(hintTokens < 8)
			{
				a = discardRandomly(s);
				if(a != null) return a;
			}
			return a;
		}
		catch (IllegalActionException e) {
			e.printStackTrace();
			throw new RuntimeException("Something is wrong");
		}
	}
	
	/** 
	 * Idea from BasicAgent.java by Tim French.
	 * Initialise our agent.
	 * @param s the first state to initiliase our agent.
	 */
	public void init(State s)
	{
		agentIndex = s.getObserver();
		numPlayers = s.getPlayers().length;
		history = new Perspective[numPlayers];

		if(numPlayers > 3)
		{
			numCard = 4;
		} else {
			numCard = 5;
		}

		//Initialise players' perspective
		for(int i = 0; i < numPlayers; i++)
		{
			history[i] = new Perspective(numCard);

		}

		//Updating each player's perspective
		for(int i = 0; i < numPlayers; i++)
		{
			if(i == agentIndex)
			{
				continue;
			}
			Card[] cards = s.getHand(i);
			//looping through players cards
			for(int j = 0; j < cards.length; j++)
			{
				//updating each other player per card
				for(int k = 0; k < numPlayers; k++)
				{
					if(k == i)
					{
						continue;
					}
					int value = cards[j].getValue();
					int colour = getColourIndex(cards[j].getColour());
					history[i].updateHandNotCard(value-1, colour);
				}
			}
		}
		
		firstAction = false;
	}
	
	/**
	 * Update the perspective of all the players,
	 * aka what they know about their own hand.
	 * @param s The current State.
	 * @throws IllegalActionException
	 */
	public void updatePerspective(State s) throws IllegalActionException
	{
		State currentState = s;
		Stack<State> stack = new Stack<>();
		
		//Push all the previous states prior to the agent's turn
		for(int i = 0; i < numPlayers; i++)
		{
			if(currentState.getPreviousAction() == null)
			{
				break;
			}
			stack.push(currentState);
			currentState = currentState.getPreviousState();

		}
		
		/*
		 * Traverse the stack to update the perspective of each agent 
		 * i.e. what each agent has known so far
		 */
		while(!stack.isEmpty())
		{
			State temp = stack.pop();
			Action previous = temp.getPreviousAction();
			int player = previous.getPlayer();
			ActionType type = previous.getType();
			int cardIndex = 0;
			Colour c = null;
			int v = 0;
			int receiver = 0;
			int colourIndex = 0;
			Card lastCard;
			
			switch (type) 
			{
				/* PLAY or DISCARD means the agent (who performs it) 
				 * would lose a card from their hand, 
				 * and pick up a new card which they know nothing about.
				 */
				case PLAY:
				case DISCARD:
					lastCard = temp.previousCardPlayed();
					cardIndex = previous.getCard();
					c = lastCard.getColour();
					colourIndex = getColourIndex(c); 
					v = lastCard.getValue();
					
					//update player's hand
					history[player].cards[cardIndex].reset();
					history[player].updateHandNotCard(v-1, colourIndex);
					
					//update other players so they know they don't have what was just drawn
					if(temp.getHand(player)[cardIndex] != null)
					{
						c = temp.getHand(player)[cardIndex].getColour();
						colourIndex = getColourIndex(c); 
						v = temp.getHand(player)[cardIndex].getValue();

						for(int i=0; i<history.length; i++) 
						{
							if(i==player)
							{
								continue;
							}
							history[i].updateHandNotCard(v-1, colourIndex);
						}
					}
					numCardRemaining--;
					break;
				case HINT_COLOUR:
					receiver = previous.getHintReceiver();
					c = previous.getColour();
					
					for(int i = 0; i < previous.getHintedCards().length; i++)
					{
						if(previous.getHintedCards()[i])
						{
							history[receiver].cards[i].colour = c;
							history[receiver].cards[i].setColour(c);
						} else {
							history[receiver].cards[i].setNotColour(c);
						}
					}
					break;
				case HINT_VALUE:
					receiver = previous.getHintReceiver();
					v = previous.getValue();
					
					for(int i = 0; i < previous.getHintedCards().length; i++)
					{
						if(previous.getHintedCards()[i])
						{
							history[receiver].cards[i].number = v;
							history[receiver].cards[i].setValue(v);
						} else {
							history[receiver].cards[i].setNotValue(v);
						}
					}
					break;
			}
			for(Perspective p : history)
			{
				p.updateNumTurn();
			}
		}
		
	}
	

	
	/**
	 * playProbablySafeCard(Threshold [0, 1]): Plays the
	 * card that is the most likely to be playable if it is at least
	 * as probable as Threshold.
	 * @param s The current State.
	 * @param probability A card safe probability must be equal or higher than the threshold.
	 * @return A PLAY action with a card that is probably safe to play or null if no such card is found.
	 */
	public Action playProbablySafeCard(State s, double probability) throws IllegalActionException
	{
		double highestPlayability = 0;
		int cardToPlay = -1;
		
		Card[] playableCard = getPlaybleCards(s);
		
		//Loop through our hand
		for(int i = 0; i < history[agentIndex].cards.length; i++)
		{	
			int sumPossibleCard = 0;
			int sumAllRemainingCard = 0;
			
			int[][] temp = history[agentIndex].cards[i].notCard;

			//Loop through the boolean table of each card
			//Take the sum of the number of all the not-ticked cards
			for(int row = 0; row < temp.length; row++)
			{
				for(int col = 0; col < temp[row].length; col++)
				{
					if(temp[row][col] > 0)
					{
						sumAllRemainingCard = sumAllRemainingCard + temp[row][col];
					}
				}
			}

			//Check if the playable cards are ticked or not
			for(int card = 0; card < playableCard.length; card++)
			{
				if(playableCard[card] == null) {
					continue;
				}
				
				int value = playableCard[card].getValue();
				int colour = getColourIndex(playableCard[card].getColour());

				//If value == 0, the card is not playable, i.e. the firework is completed
				if(value == 0)
				{
					continue;
				}

				//Take the sum of the number of the playable cards from the currentDeck
				if(temp[value-1][colour] > 0)
				{
					sumPossibleCard = sumPossibleCard + temp[value-1][colour];
				}
			}

			//Get the playability
			double playability = (double) sumPossibleCard / sumAllRemainingCard;

			if(playability > probability && playability > highestPlayability)
			{
				highestPlayability = playability;
				cardToPlay = i;
			}
		}

		//Play the card with highest probability (if there is a card to play)
		if(cardToPlay != -1)
		{
			return new Action(agentIndex,this.toString(),ActionType.PLAY,cardToPlay);
		}
		return null;
	}
	
	/**
	 * Get a set of cards that are playable of the current state
	 * @param s The current State.
	 * @return An array of playable cards in the current state. 
	 * A card with value 0 indicates the matching firework for it is completed.
	 */
	private Card[] getPlaybleCards(State s)
	{
		Card[] result = new Card[5];
		int value = 0;
		Colour colour = null;
		for(int i = 0; i < colourArray.length; i++)
		{
			value = 0;
			colour = colourArray[i];
			if(!s.getFirework(colourArray[i]).isEmpty())
			{
				value = s.getFirework(colourArray[i]).peek().getValue();
				if(value == 5) //If the rocket is already completed
				{
					result[i] = null;
					continue;
				}
			}
			result[i] = new Card(colour, value+1);
		}
		
		return result;
	}
	
	/**
	 * playSafeCard: Plays a card only if it is guaranteed 
	 * that it is playable.
	 * @param s The current state.
	 * @return An action which plays a card that is guaranteed (100%) to be safe to play
	 * or null if there is no card that 100% safe to play.
	 * @throws IllegalActionException
	 */
	public Action playSafeCard (State s) throws IllegalActionException
	{
		//Check the fireworks to see which card can be played for each color
		for(int i = 0; i < colourArray.length; i++)
		{
			int value = 0;
			//Check if the current firework is empty
			if(!s.getFirework(colourArray[i]).isEmpty())
			{
				value = s.getFirework(colourArray[i]).peek().getValue();				
			}

			//Check which card you already know that is playable
			for(int j = 0; j < history[agentIndex].cards.length; j++)
			{
				if(history[agentIndex].cards[j].number == value+1)
				{
					if(history[agentIndex].cards[j].colour == colourArray[i])
					{
						//Play the damn card
						return new Action(agentIndex,this.toString(),ActionType.PLAY,j);
					}
				}
			}
		}
	
		return null;
	}
	
	/**
	 * tellAnyoneAboutUsefulCard: Tells the next player
	 * with a useful card either the remaining unknown suit of
	 * the card or the rank of the card.
	 * @param s The current State.
	 * @return A HINT_COLOUR or HINT_VALUE which would tell a player (who our agent determines to have useful card)
	 * the colour or value of the useful card they have or null if no one has any useful card that we can give hint
	 * or there is no hint token left.
	 * @throws IllegalActionException
	 */
	public Action tellAnyoneAboutUsefulCard(State s) throws IllegalActionException
	{
		Card[] playableCards = getPlaybleCards(s);

		for(int player = (agentIndex+1) % numPlayers; player != agentIndex; player = (player+1) % numPlayers)
		{
			Card[] playerHand = s.getHand(player);
			CardHistory[] playerHistory = history[player].cards;
			
			int maxHints = 0;
			int maxCard = 0;
			ActionType type = ActionType.PLAY;
			boolean halfKnownCardFound = false;
			for(int card=0; card<playerHand.length; card++)
			{
				if(!isCardPlayable(playableCards, playerHand[card]) || playerHistory[card].isCardKnown())
				{
					continue;
				} 
				if(!playerHistory[card].isCardUnknown())
				{
					halfKnownCardFound = true;
				}
				if(playerHistory[card].isNumberKnown() || !halfKnownCardFound)
				{
					int numHints = newHintsGiven(playerHistory, playerHand, ActionType.HINT_COLOUR, getColourIndex(playerHand[card].getColour()));
					if(numHints > maxHints)
					{
						maxHints = numHints;
						maxCard = card;
						type = ActionType.HINT_COLOUR;
					}
				}
				if(playerHistory[card].isColourKnown() || !halfKnownCardFound)
				{
					int numHints = newHintsGiven(playerHistory, playerHand, ActionType.HINT_VALUE, playerHand[card].getValue());
					if(numHints > maxHints)
					{
						maxHints = numHints;
						maxCard = card;
						type = ActionType.HINT_VALUE;
					}
				}
			}

			if(maxHints > 0)
			{
				switch(type){
					case HINT_VALUE:
						return new Action(agentIndex,this.toString(),
							ActionType.HINT_VALUE,player,
							getNumberHint(playerHand,playerHand[maxCard].getValue()),playerHand[maxCard].getValue());
					case HINT_COLOUR:
						return new Action(agentIndex,this.toString(),
							ActionType.HINT_COLOUR,player,
							getColourHint(playerHand,playerHand[maxCard].getColour()),playerHand[maxCard].getColour());
					default:
				}
			}
		}	
		return null;
	}

	/**
	 * Returns the number of new pieces of info in a player's card history given for a specific hint.
	 * @param playerCardHistory The array contains all the history of the cards in the player's hand.
	 * @param playerhand The current hand of the player.
	 * @param type The type of hint.
	 * @param value The colour index or the number index depending which type of hint is.
	 * @return the number of new pieces of info in a player's card history given for a specific hint. 
	 */
	public int newHintsGiven(CardHistory[] playerCardHistory,Card[] playerhand, ActionType type, int value)
	{

		int count = 0;
		switch(type)
		{
			case HINT_VALUE:
				for(int i=0; i<playerCardHistory.length; i++)
				{
					if(playerhand[i] == null)
					{
						continue;
					}
					if(!playerCardHistory[i].isNumberKnown() 
							&& playerhand[i].getValue()==value)
					{
						count++;
					}
				}
				break;
			case HINT_COLOUR:
				Colour colour = colourArray[value];
				for(int i=0; i<playerCardHistory.length; i++)
				{
					if(playerhand[i] == null)
					{
						continue;
					}
					if(!playerCardHistory[i].isColourKnown() 
							&& playerhand[i].getColour().equals(colour))
					{
						count++;
					}
				}
				break;
			default:
		}
		return count;
	}

	/**
	 * Check if a card is playable.
	 * @param playableCards The set of cards that are playable.
	 * @param card The card to be checked.
	 * @return true if it is playable, false otherwise.
	 */
	public boolean isCardPlayable(Card[] playableCards, Card card) 
	{
		if(card == null)
		{
			return false;
		}
		for(int i=0; i<playableCards.length; i++)
		{
			if(playableCards[i] != null && 
					card.equals(playableCards[i]))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Return a boolean array indicating which cards are playable in the player hand.
	 * @param s The current State
	 * @param playableCards The array contains cards that can be played.
	 * @param player The player index
	 * @return A boolean array indicating which cards are playable in the player hand.
	 */
	public boolean[] getPlayableHand(State s, Card[] playableCards, int player)
	{
		Card[] playerHand = s.getHand(player);
		boolean[] playerPlaybleCards = new boolean[numCard];
		for(int card = 0; card < playerHand.length; card++)
		{
			for(int pCard = 0; pCard < playableCards.length; pCard++)
			{
				if(playerHand[card].equals(playableCards[pCard]))
				{
					playerPlaybleCards[card] = true;
				}
			}
		}
		return playerPlaybleCards;
	}

	/**
	 * Return an array of cards we are hinting about their colour.
	 * @param cards The hand of the player our agent is giving hint to.
	 * @param c The colour of the hint.
	 * @return A boolean array which an element is set to true if the card is part of the hint.
	 */
	public boolean[] getColourHint(Card[] cards, Colour c)
	{
		boolean[] colourHints = new boolean[cards.length];

		for(int i=0; i<cards.length; i++)
		{
			if(cards[i] == null)
			{
				continue;
			}
			if(cards[i].getColour() == c)
			{
				colourHints[i] = true;
			}
		}
		
		return colourHints;
	}

	/**
	 * Return an array of cards we are hinting about their number.
	 * @param cards The hand of the player our agent is giving hint to.
	 * @param n The number of the hint.
	 * @return A boolean array which an element is set to true if the card is part of the hint.
	 */
	public boolean[] getNumberHint(Card[] cards, int n)
	{
		boolean[] numberHints = new boolean[cards.length];

		for(int i=0; i<cards.length; i++)
		{
			if(cards[i] == null)
			{
				continue;
			}
			if(cards[i].getValue() == n)
			{
				numberHints[i] = true;
			}
		}
		
		return numberHints;
	}

	/**
	 *  TellDispensable: Tells the next player with an unknown
	 *	dispensible card the information needed to correctly identify
	 *	that the card is dispensible. This rule will only target
	 *	cards that can be identified to the holder as dispensible
	 *	with the addition of a single piece of information.
	 * @param s The curent state.
	 * @return A HINT_VALUE or HINT_COLOUR action or null if not possible.
	 * @throws IllegalActionException
	 */
	public Action tellDispensible(State s) throws IllegalActionException
	{		
		int minimumThrowableNumber = getMinimumThrowableNumber(s);
			
		boolean[] throwAbleColour =  noLongerPlayableColours(s);

		for(int player =  (agentIndex+1) % numPlayers; player != agentIndex; player = (player+1) % numPlayers)
		{
			Card[] playerHand = s.getHand(player);
			
			int maxHints = 0;
			int maxCard = 0;
			ActionType type = ActionType.PLAY;
			for(int card = 0; card < playerHand.length; card++)
			{
				CardHistory cardhistory = history[player].cards[card];

				if(cardhistory.isCardKnown() || cardhistory.isCardUnknown())
				{
					continue;
				} 
				if(cardhistory.isNumberKnown())
				{
					if(cardhistory.number < minimumThrowableNumber)
					{
						int numHints = newHintsGiven(history[player].cards, playerHand, ActionType.HINT_COLOUR, getColourIndex(playerHand[card].getColour()));
						if(numHints > maxHints)
						{
							maxHints = numHints;
							maxCard = card;
							type = ActionType.HINT_COLOUR;
						}
					}
				} 
				if(cardhistory.isColourKnown())
				{
					int colourIndex = getColourIndex(cardhistory.colour);
					if(throwAbleColour[colourIndex])
					{
						int numHints = newHintsGiven(history[player].cards, playerHand, ActionType.HINT_VALUE, playerHand[card].getValue());
						if(numHints > maxHints)
						{
							maxHints = numHints;
							maxCard = card;
							type = ActionType.HINT_VALUE;
						}
					}
				}
			}

			if(maxHints > 0)
			{
				switch(type){
					case HINT_VALUE:
						return new Action(agentIndex,this.toString(),
							ActionType.HINT_VALUE,player,
							getNumberHint(playerHand,playerHand[maxCard].getValue()),playerHand[maxCard].getValue());
					case HINT_COLOUR:
						return new Action(agentIndex,this.toString(),
							ActionType.HINT_COLOUR,player,
							getColourHint(playerHand,playerHand[maxCard].getColour()),playerHand[maxCard].getColour());
					default:
				}
			}
		}
		return null;
	}

	/**
	 * Return the number which any card can be safely discarded if equal to or below the number
	 * @param s The current State
	 * @return the number which any card can be safely discarded.
	 */
	public int getMinimumThrowableNumber(State s)
	{
		int value = 9999;
		for(int i = 0; i < colourArray.length; i++)
		{
			if(!s.getFirework(colourArray[i]).isEmpty())
			{
				Card currentCard = s.getFirework(colourArray[i]).peek();
				if(value > currentCard.getValue())
				{
					value = currentCard.getValue();
				}
			} else {
				value = 0;
			}
		}
		return value;
	}

	//returns an boolean array identifying which colours are still playable
	/**
	 * Return an boolean array identifying which colours are still playable.
	 * @param s The current State.
	 * @return An boolean array identifying which colours are still playable.
	 * If a colour is not playable, its index is set to true, false otherwise.
	 */
	public boolean[] noLongerPlayableColours(State s) 
	{
		boolean[] throwAbleColour = new boolean[colourArray.length];
		for(int i = 0; i < colourArray.length; i++)
		{
			int value = 0;
			if(!s.getFirework(colourArray[i]).isEmpty())
			{
				value = s.getFirework(colourArray[i]).peek().getValue();
			}
			if(value == 5 || history[agentIndex].deck[value][getColourIndex(colourArray[i])] == 0)
			{
				throwAbleColour[getColourIndex(colourArray[i])] = true;
			}
		}
		return throwAbleColour;
	}
	
	/**
	 * osawaDiscard: Discards a card if it cannot be played at
	 * the end of the turn. This will discard cards that we know
	 * enough about to disqualify them from being playable. For
	 * example, a card with an unknown suit but a rank of 1 will
	 * not be playable if all the stacks have been started. This
	 * rule also considers cards that can not be played because
	 * their pre-requisite cards have already been discarded.
	 * @param s The current state.
	 * @return An action which discards a card that is guaranteed to not be playable 
	 * or null if there is not such card or there are still 8 hints token.
	 * @throws IllegalActionException
	 */
	public Action osawaDiscard(State s) throws IllegalActionException
	{
		int minimumThrowableNumber = getMinimumThrowableNumber(s);
		
		boolean[] throwAbleColour =  noLongerPlayableColours(s);

		for(int i = 0; i < history[agentIndex].cards.length; i++)
		{
			CardHistory cardhistory = history[agentIndex].cards[i];

			if(cardhistory.isCardKnown())
			{
				int value = 0;
				if(!s.getFirework(cardhistory.colour).isEmpty())
				{
					value = s.getFirework(cardhistory.colour).peek().getValue();
				}
				if(cardhistory.number < value || isCardDisconnected(value,cardhistory,agentIndex))
				{
					return new Action(agentIndex, this.toString(), ActionType.DISCARD, i);
				}
			} else {
				if(cardhistory.isNumberKnown())
				{
					if(cardhistory.number < minimumThrowableNumber)
					{
						return new Action(agentIndex, this.toString(), ActionType.DISCARD, i);
					}
				} 
				if(cardhistory.isColourKnown())
				{
					int colourIndex = getColourIndex(cardhistory.colour);
					if(throwAbleColour[colourIndex])
					{
						return new Action(agentIndex, this.toString(), ActionType.DISCARD, i);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Check if a card is no longer usable because cards connect to it are all discarded.
	 * @param fireWorkValue The highest value of a particular firework.
	 * @param cardhistory The history of the card we are examining.
	 * @param player The index of the player.
	 * @return true if the card is no longer usable, false otherwise.
	 */
	public boolean isCardDisconnected(int fireWorkValue, CardHistory cardhistory, int player) {
		for(int j = fireWorkValue+1; j < cardhistory.number; j++)
		{
			if(history[player].deck[j][getColourIndex(cardhistory.colour)] == 0)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * discardOldestFirst: Discards the card that has been held
	 * in the hand the longest amount of time.
	 * @param s the current State
	 * @return An action which discards a card that has been held for the longest amount of time or null if not possible.
	 * @throws IllegalActionException
	 */
	public Action discardOldestFirst(State s) throws IllegalActionException
	{
		for(int i = 0; i < numCard; i++)
		{
			CardHistory card = history[agentIndex].cards[i];
			if(card.turn > 5)
			{
				return new Action(agentIndex, this.toString(), ActionType.DISCARD, i);
			}
		}
		return null;
	}
	/**
	 * tellRandomly: Tells the next player a random fact about any card in their hand.
	 * From BasicAgent.java
	 * @author Tim French
	 * @param s The current State.
	 * @return A HINT_VALUE or HINT_COLOUR action or null if there is no hint token left.
	 */
	public Action tellRandomly(State s) throws IllegalActionException
	{
		if(s.getHintTokens()>0){
			int hintee = (agentIndex+1)%numPlayers;
			Card[] hand = s.getHand(hintee);
	
			java.util.Random rand = new java.util.Random();
			int cardIndex = rand.nextInt(hand.length);
			while(hand[cardIndex]==null) cardIndex = rand.nextInt(hand.length);
			Card c = hand[cardIndex];
	
			if(Math.random()>0.5){//give colour hint
			  boolean[] col = new boolean[hand.length];
			  for(int k = 0; k< col.length; k++){
				col[k]=c.getColour().equals((hand[k]==null?null:hand[k].getColour()));
			  }
			  return new Action(agentIndex,toString(),ActionType.HINT_COLOUR,hintee,col,c.getColour());
			}
			else{//give value hint
			  boolean[] val = new boolean[hand.length];
			  for(int k = 0; k< val.length; k++){
				if (hand[k] == null) continue;
				val[k]=c.getValue() == (hand[k]==null?-1:hand[k].getValue());
			  }
			  return new Action(agentIndex,toString(),ActionType.HINT_VALUE,hintee,val,c.getValue());
			}
	
		  }
		return null;
	}
	
	/**
	 * discardRandomly: Randomly discards a card from the hand.
	 * Idea from BasicAgent.Java by Tim French.
	 * @param s The current State.
	 * @return An action which discards a card randomly or null if there are still 8 hints token.
	 * @throws IllegalActionException
	 */
	public Action discardRandomly(State s) throws IllegalActionException
	{
		if(s.getHintTokens() != 8)
		{
			Random rand = new Random();
			int cardIndex = rand.nextInt(numCard);
			history[agentIndex].cards[cardIndex].reset();
			return new Action(agentIndex, toString(), ActionType.DISCARD, cardIndex);
		}
		return null;
	}
	
	/**
	 * Return agent's name.
	 * @return My agent name (Tuturu!).
	 */
	public String toString() 
	{
		return "Tuturu ~♪"; //Tuturu ~♪
	}
	
	/**
	 * Store the perspective of what each player knows about their card.
	 */
	class Perspective 
	{
		private CardHistory[] cards;
		private int[][] deck;
		
		/**
		 * Constructor for Perspective.
		 * @param n The number of cards in a player hand.
		 */
		public Perspective(int n)
		{
			deck = new int[5][5];
			for(int number = 0; number < 5; number++)
			{
				for(int colour = 0; colour < 5; colour++)
				{
					switch (number) {
					case 0:
						deck[number][colour] = 3;
						break;
					case 1:
					case 2:
					case 3:
						deck[number][colour] = 2;
						break;
					default:
						deck[number][colour] = 1;
					}
				}
			}
			cards = new CardHistory[n];
			for(int i=0; i<cards.length; i++)
			{
				cards[i] = new CardHistory(deck);
			}
		}

		/**
		 * Update the number of turn a card is held.
		 */
		public void updateNumTurn() 
		{
			for(int i = 0; i < cards.length; i++)
			{
				cards[i].turn++;
			}
		}
		
		/**
		 * Update the fact that our agent's hand cannot contain this card.
		 * @param valueIndex The value of the card.
		 * @param colourIndex The colour of the card.
		 */
		public void updateHandNotCard(int valueIndex, int colourIndex)
		{
			this.deck[valueIndex][colourIndex]--;
			for(int i=0; i<cards.length; i++)
			{
				cards[i].notCard[valueIndex][colourIndex]--;
				if(cards[i].notCard[valueIndex][colourIndex] == 0)
				{
					cards[i].updateNotNumber(valueIndex);
					cards[i].updateNotColour(colourIndex);
					cards[i].checkIfNumberIsKnown();
					cards[i].checkIfColourIsKnown();
				}
			}
		}
	}
	
	/**
	 * Store history of a card via hints and not hints.
	 */
	class CardHistory 
	{
		private int[][] notCard;
		private int[][] deck;
		private int number;
		private Colour colour;		
		private boolean[] notNumber;
		private boolean[] notColour;
		private int turn;
		/**
		 * Constructor for card history
		 * @param d the deck of card to initialise
		 */	
		public CardHistory(int[][] d)
		{			
			deck = d;
			notCard = new int[5][5];
			for(int number = 0; number < 5; number++)
			{
				for(int colour = 0; colour < 5; colour++)
				{
					switch (number) {
					case 0:
						notCard[number][colour] = 3;
						break;
					case 1:
					case 2:
					case 3:
						notCard[number][colour] = 2;
						break;
					default:
						notCard[number][colour] = 1;
					}
				}
			}
			this.number = -1;
			this.colour = null;
			this.turn = 1;
			notNumber = new boolean[5];
			notColour = new boolean[5];	
		}

		/** Check if the number is known
		 * @return true if the number is known or false otherwise.
		 */
		public boolean isNumberKnown()
		{
			return number != -1;
		}

		/** Check if the colour is known
		 * @return true if the colour is known or false otherwise.
		 */
		public boolean isColourKnown()
		{
			return colour != null;
		}

		
		/**
		 * Check if the card is known (i.e. both colour & number are unknown)
		 * @return true if both colour & number are known, false otherwise.
		 */
		public boolean isCardKnown()
		{
			return isColourKnown() && isNumberKnown();
		}
		
		//returns if card is unknown
		/**
		 * Check if the card is unknown (i.e. both colour & number are unknown)
		 * @return true if both colour & number are unknown, false otherwise.
		 */
		public boolean isCardUnknown()
		{
			return !isColourKnown() && !isNumberKnown();
		}

		/**
		 * Remove all previous hint a player knows about their card.
		 * This occurs when the card is played or discarded.
		 */
		public void reset()
		{
			for(int row = 0; row < deck.length; row++)
			{
				for(int col = 0; col < deck[0].length; col++)
				{
					notCard[row][col] = deck[row][col];
				}
			}
			number = -1;
			colour = null;
			turn = 0;

			notNumber = new boolean[5];
			notColour = new boolean[5];
			updateNotNumberArray();
			updateNotColourArray();
		}

		/**
		 * Update the number the card cannot be by checking notCard
		 */
		public void updateNotNumberArray()
		{
			for(int row=0; row<notCard.length; row++)
			{
				updateNotNumber(row);
			}
			checkIfNumberIsKnown();
		}
		
		/**
		 * Update the particular number the card cannot be.
		 * @param num The number the card cannot be.
		 */
		public void updateNotNumber(int num)
		{
			if(notNumber[num])
			{
				return;
			}
			int count = 0;
			for(int col=0; col<notCard[0].length; col++)
			{
				if(notCard[num][col] != 0)
				{
					break;
				}
				count++;
			}
			if(count == 5)
			{
				notNumber[num] = true;
			}
		}

		/**
		 * Update the colour the card cannot be by checking notCard
		 */
		public void updateNotColourArray()
		{
			for(int col=0; col<notCard[0].length; col++)
			{
				updateNotColour(col);
			}
			checkIfColourIsKnown();
		}
		
		/**
		 * Update the particular colour the card cannot be.
		 * @param colour The colour the card cannot be.
		 */
		public void updateNotColour(int colour)
		{
			if(notColour[colour])
			{
				return;
			}

			int count = 0;
			for(int row=0; row<notCard.length; row++)
			{
				if(notCard[row][colour] != 0)
				{
					break;
				}
				count++;
			}
			if(count == 5)
			{
				notColour[colour] = true;
			}
		}
		
		/**
		 * Set the colour the card cannot be via notColour.
		 * @param c the colour the card cannot be.
		 */
		public void setNotColour(Colour c)
		{
			int column = getColourIndex(c);
			for(int i = 0; i < notCard.length; i++)
			{
				notCard[i][column] = 0;
			}
			notColour[column] = true;
			updateNotNumberArray();
			checkIfColourIsKnown();								
		}

		/**
		 * Check if our agent has enough hints to guess the colour.
		 */
		public void checkIfColourIsKnown()
		{
			if(colour != null)
			{
				return;
			}
			int count = 0;
			int column =0;
			for(int i=0; i<notColour.length; i++)
			{
				if(!notColour[i])
				{
					count++;
					column = i;
				}				
			}
			if(count == 1)
			{
				colour = colourArray[column];
			}	
		}
		
		/**
		 * Set the card to be the colour c (because we know its colour is c).
		 * @param c The colour we know is the card's colour.
		 */
		public void setColour(Colour c)
		{
			colour = c;
			int column = getColourIndex(c);
			for(int i = 0; i < notCard.length; i++)
			{
				if(i == column)
				{
					//updates notNumber
					for(int j=0; j < notCard.length; j++)
					{
						if(notCard[j][column] == 0)
						{
							notNumber[j] = true;
						}
					}
				} else {
					//sets other possible colours to zero
					for(int j = 0; j < notCard.length; j++)
					{
						notCard[j][i] = 0;
					}
				}
			}
			checkIfNumberIsKnown();
		}
		
		/**
		 * Set the card to be not this value
		 * @param v The value the card cannot be.
		 */
		public void setNotValue(int v)
		{
			int row = v-1;
			for(int i = 0; i < notCard.length; i++)
			{
				notCard[row][i] = 0;
			}
			notNumber[row] = true;
			updateNotColourArray();
			checkIfNumberIsKnown();
		}

		/**
		 * Check if our agent has enough hints to guess the number.
		 */
		public void checkIfNumberIsKnown()
		{
			if(number != -1)
			{
				return;
			}
			int count = 0;
			int row = 0;
			for(int i=0; i<notNumber.length; i++)
			{
				if(!notNumber[i])
				{
					count++;
					row = i;
				}
			}
			if(count == 1)
			{
				number = row;
			}
		}
		
		/**
		 * Set the card to be the value v (because we know its value is v).
		 * @param v The value we know is the card's value.
		 */
		public void setValue(int v)
		{
			number = v;
			int row = v-1;
			for(int i = 0; i < notCard.length; i++)
			{
				if(i == row)
				{
					//update notColour
					for(int j = 0; j < notCard.length; j++)
					{
						if(notCard[row][j] == 0)
						{
							notColour[j] = true;
						}
					}
				} else {
					//set all other card numbers to 0;
					for(int j = 0; j < notCard.length; j++)
					{
						notCard[i][j] = 0;
					}
				}
			}
			checkIfColourIsKnown();
		}		
	}

	/**
	 * Return index of a colour (in this agent)
	 * @param c the colour that we need its index
	 * @return index of a colour (-1 if no colour is found)
	 */
	private int getColourIndex(Colour c) {
		switch (c) {
		case BLUE:
			return 0;
		case GREEN:
			return 1;
		case RED:
			return 2;
		case WHITE:
			return 3;
		case YELLOW:
			return 4;
		default:
			return -1;
		}
	}

}
