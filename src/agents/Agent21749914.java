package agents;

import java.util.Stack;

import hanabAI.Action;
import hanabAI.ActionType;
import hanabAI.Agent;
import hanabAI.Card;
import hanabAI.Colour;
import hanabAI.IllegalActionException;
import hanabAI.State;

public class Agent21749914 implements Agent {
	
	private int numCardRemaining = 50;
	private int numPlayers;
	private Perspective[] history;
	private int numCard;
	private boolean firstAction = true;
	private Colour[] colourArray = new Colour[]{Colour.BLUE, Colour.GREEN, Colour.RED, Colour.WHITE, Colour.YELLOW};
	
	@Override
	public Action doAction(State s) 
	{
		if(firstAction)
		{
			init(s);
		}
		return null;
	}
	
	/*
	 * From BasicAgent.java by Tim French
	 */
	public void init(State s)
	{
		numPlayers = s.getPlayers().length;
		history = new Perspective[numPlayers];

		if(numPlayers == 5)
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
			if(i == s.getObserver())
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
		*Traverse the stack to update the perspective of each agent 
		*i.e. what each agent has known so far
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
					numCardRemaining--;
					
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
					break;
				case HINT_COLOUR:
					receiver = previous.getHintReceiver();
					c = previous.getColour();
					
					for(int i = 0; i < previous.getHintedCards().length; i++)
					{
						if(previous.getHintedCards()[i])
						{
							history[receiver].cards[i].colour = c;
							history[receiver].cards[i].updateColour(c);
						} else {
							history[receiver].cards[i].updateNotColour(c);
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
							history[receiver].cards[i].updateValue(v);
						} else {
							history[receiver].cards[i].updateNotValue(v);
						}
					}
					break;
			}
		}
	}
	

	
	/*
	 * playProbablySafeCard(Threshold [0, 1]): Plays the
	 * card that is the most likely to be playable if it is at least
	 * as probable as Threshold.
	 */
	public Action playProbablySafeCard(State s, double probability) throws IllegalActionException
	{
		int player = s.getObserver();
		double highestPlayability = 0;
		int cardToPlay = -1;
		
		Card[] playableCard = getPlaybleCards(s);
		
		//Loop through our hand
		for(int i = 0; i < history[player].cards.length; i++)
		{	
			int sumPossibleCard = 0;
			int sumAllRemainingCard = 0;
			
			int[][] temp = history[player].cards[i].notCard;

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
			return new Action(player,this.toString(),ActionType.PLAY,cardToPlay);
		}
		return null;
	}
	
	/*
	 * Get a set of cards that are playable of the current state
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
					value = -1;
				}
			}
			result[i] = new Card(colour, value+1);
		}
		
		return result;
	}
	/*
	 * playSafeCard: Plays a card only if it is guaranteed 
	 * that it is playable
	 */
	public Action playSafeCard (State s) throws IllegalActionException
	{
		int player = s.getObserver();

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
			for(int j = 0; j < history[player].cards.length; j++)
			{
				if(history[player].cards[j].number == value+1)
				{
					if(history[player].cards[j].colour == colourArray[i])
					{
						//Play the damn card
						return new Action(player,this.toString(),ActionType.PLAY,j);
					}
				}
			}
		}
	
		return null;
	}
	/*
	 * tellAnyoneAboutUsefulCard: Tells the next player
	 * with a useful card either the remaining unknown suit of
	 * the card or the rank of the card.
	 */
	public Action tellAnyoneAboutUsefulCard(State s) throws IllegalActionException
	{
		Card[] playableCards = getPlaybleCards(s);

		for(int player =  s.getNextPlayer(); player != s.getObserver(); player = (player+1) % numPlayers)
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

			for(int card = 0; card < playerPlaybleCards.length; card++)
			{
				if(playerPlaybleCards[card])
				{
					int cardValue = history[player].cards[card].number;
					Colour cardColour = history[player].cards[card].colour;
					boolean numfound = cardValue != -1;
					boolean colfound = cardColour != null;
					if(numfound ^ colfound)
					{
						if(numfound)
						{
							return new Action(s.getObserver(),this.toString(),
												ActionType.HINT_COLOUR,player,
												getColourHint(playerHand,cardColour),cardColour);
						} else {
							return new Action(s.getObserver(),this.toString(),
												ActionType.HINT_VALUE,player,
												getNumberHint(playerHand,cardValue),cardValue);
						}
					}
				}
			}

			for(int card = 0; card < playerPlaybleCards.length; card++)
			{
				if(playerPlaybleCards[card])
				{
					int cardValue = history[player].cards[card].number;
					Colour cardColour = history[player].cards[card].colour;
					boolean numfound = cardValue != -1;
					boolean colfound = cardColour != null;
					if(!numfound && !colfound)
					{
						int countNumber = 0;
						int countColour = 0;
						boolean[] colourhint = getColourHint(playerHand,cardColour);
						boolean[] valuehint = getNumberHint(playerHand,cardValue);

						for(int i = 0; i < colourhint.length; i++)
						{
							if(colourhint[i])
							{
								countColour++;
							}
							if(valuehint[i])
							{
								countNumber++;
							}
						}
						if(countColour > countNumber)
						{
							return new Action(s.getObserver(),this.toString(),
												ActionType.HINT_COLOUR,player,
												getColourHint(playerHand,cardColour),cardColour);
						} else {
							return new Action(s.getObserver(),this.toString(),
												ActionType.HINT_VALUE,player,
												getNumberHint(playerHand,cardValue),cardValue);
						}
					}
				}
			}
		}

		return null;
	}

	public boolean[] getColourHint(Card[] cards, Colour c)
	{
		boolean[] colourHints = new boolean[cards.length];

		for(int i=0; i<cards.length; i++)
		{
			if(cards[i].getColour() == c)
			{
				colourHints[i] = true;
			}
		}
		
		return colourHints;
	}

	public boolean[] getNumberHint(Card[] cards, int n)
	{
		boolean[] numberHints = new boolean[cards.length];

		for(int i=0; i<cards.length; i++)
		{
			if(cards[i].getValue() == n)
			{
				numberHints[i] = true;
			}
		}
		
		return numberHints;
	}


	public Action tellDispensible(State s)
	{
		return null;
	}
	
	/* 		
	 *	osawaDiscard: Discards a card if it cannot be played at
	 *	the end of the turn. This will discard cards that we know
	 *	enough about to disqualify them from being playable. For
	 *	example, a card with an unknown suit but a rank of 1 will
	 *	not be playable if all the stacks have been started. This
	 *	rule also considers cards that can not be played because
	 *	their pre-requisite cards have already been discarded. 
	 */
	
	public Action osawaDiscard(State s) throws IllegalActionException
	{
		int player = s.getObserver();

		int minimumThrowableNumber = 9999;
		boolean[] throwAbleColour = new boolean[colourArray.length];

		for(int i = 0; i < colourArray.length; i++)
		{
			if(!s.getFirework(colourArray[i]).isEmpty())
			{
				Card currentCard = s.getFirework(colourArray[i]).peek();
				if(minimumThrowableNumber > currentCard.getValue())
				{
					minimumThrowableNumber = currentCard.getValue();
				}
			} else {
				minimumThrowableNumber = 0;
			}
		}
		
		for(int i = 0; i < colourArray.length; i++)
		{
			int value = 0;
			if(!s.getFirework(colourArray[i]).isEmpty())
			{
				value = s.getFirework(colourArray[i]).peek().getValue();
			}
			if(value == 5 || history[player].deck[value][getColourIndex(colourArray[i])] == 0)
			{
				throwAbleColour[getColourIndex(colourArray[i])] = true;
			}
		}

		for(int i = 0; i < history[player].cards.length; i++)
		{
			int cardValue = history[player].cards[i].number;
			Colour cardColour = history[player].cards[i].colour;

			if(cardValue != -1)
			{
				if(cardValue < minimumThrowableNumber)
				{
					return new Action(player, this.toString(), ActionType.DISCARD, i);
				}
			} else if(cardColour != null)
			{
				int colourIndex = getColourIndex(cardColour);
				if(throwAbleColour[colourIndex])
				{
					return new Action(player, this.toString(), ActionType.DISCARD, i);
				}
			} else 
			{
				int value = 0;
				if(!s.getFirework(cardColour).isEmpty())
				{
					value = s.getFirework(cardColour).peek().getValue();
				}
				if(cardValue <= value)
				{
					return new Action(player, this.toString(), ActionType.DISCARD, i);
				} else {
					for(int j = value+1; j < cardValue; j++)
					{
						if(history[player].deck[j][getColourIndex(cardColour)] == 0)
						{
							return new Action(player, this.toString(), ActionType.DISCARD, i);
						}
					}
				}
			}
		}
		return null;
	}
	
	public Action discardOldestFirst(State s)
	{
		return null;
	}
	
	public Action tellRandomly(State s)
	{
		return null;
	}
	
	public Action discardRandomly(State s)
	{
		return null;
	}
	
	public String toString() 
	{
		return "トゥットゥルー♪"; //Tuturu ~♪
	}
	
	/*
	 * Store the perspective of what each player knows about their card
	 */
	class Perspective 
	{
		private CardHistory[] cards;
		private int[][] deck;
		
		public Perspective(int n)
		{
			cards = new CardHistory[n];
			for(int i=0; i<cards.length; i++)
			{
				cards[i] = new CardHistory(deck);
			}
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
		}

		public void updateHandNotCard(int valueIndex, int colorIndex)
		{
			this.deck[valueIndex][colorIndex]--;
			for(int i=0; i<cards.length; i++)
			{
				cards[i].notCard[valueIndex][colorIndex]--;
			}
		}
	}
	
	/*
	 * Store the probability what a card could be
	 */
	class CardHistory 
	{
		//change the cardhistory to 2-d boolean
		private int[][] notCard;
		private int[][] deck;
		private int number;
		private Colour colour;		
		private boolean[] notNumber;
		private boolean[] notColour;
		
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
			notNumber = new boolean[5];
			notColour = new boolean[5];			
		}
		
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
			notNumber = new boolean[5];
			notColour = new boolean[5];			
		}
		
		public void updateNotColour(Colour c)
		{
			int column = getColourIndex(c);
			for(int i = 0; i < notCard.length; i++)
			{
				notCard[i][column] = 0;
			}

			//check if there are enough hints to guess colour
			if(colour != null)
			{
				return;
			}
			notColour[column] = true;
			int count = 0;
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
		
		public void updateColour(Colour c)
		{
			colour = c;
			int column = getColourIndex(c);
			for(int i = 0; i < notCard.length; i++)
			{
				if(i == column)
				{
					continue;
				}
				for(int j = 0; j < notCard.length; j++)
				{
					notCard[j][i] = 0;
				}
			}
		}
		
		public void updateNotValue(int v)
		{
			int row = v-1;
			for(int i = 0; i < notCard.length; i++)
			{
				notCard[row][i] = 0;
			}

			//check if there are enought hints to guess the number
			if(number != -1)
			{
				return;
			}
			notNumber[row] = true;
			int count = 0;
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
		
		public void updateValue(int v)
		{
			number = v;
			int row = v-1;
			for(int i = 0; i < notCard.length; i++)
			{
				if(i == row)
				{
					continue;
				}
				for(int j = 0; j < notCard.length; j++)
				{
					notCard[i][j] = 0;
				}
			}
		}
		
	}
	
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
