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
	private int[][] currentDeck;
	private Colour[] colourArray = new Colour[]{Colour.RED, Colour.BLUE, Colour.GREEN, Colour.WHITE, Colour.YELLOW};
	
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
		
		currentDeck = new int[5][5];
		for(int number = 0; number < 5; number++)
		{
			for(int colour = 0; colour < 5; colour++)
			{
				switch (number) {
				case 0:
					currentDeck[number][colour] = 3;
					break;
				case 1:
				case 2:
				case 3:
					currentDeck[number][colour] = 2;
					break;
				default:
					currentDeck[number][colour] = 1;
				}
			}
		}
		firstAction = false;
	}
	
	public void updatePerspective(State s) throws IllegalActionException
	{
		State currentState = s;
		Stack<State> stack = new Stack<>();
		
		for(int i = 0; i < numPlayers; i++)
		{
			if(currentState.getPreviousAction() == null)
			{
				break;
			}
			
			stack.push(currentState);
			currentState = currentState.getPreviousState();

		}
	
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
			
			switch (type) 
			{
				case PLAY:
				case DISCARD:
					cardIndex = previous.getCard();
					c = temp.getHand(player)[cardIndex].getColour();
					colourIndex = getColourIndex(c); 
					v = temp.getHand(player)[cardIndex].getValue();
					
					currentDeck[v-1][colourIndex]--;
					numCardRemaining--;
					history[player].cards[cardIndex].reset(currentDeck);
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
	public Action playProbablySafeCard(State s, double probability)
	{
		Action action = null;
		int player = s.getObserver();
		
		Card[] playableCard = getPlaybleCards(s);
		
		//TODO: Loop through our hand
		//TODO: Loop through the boolean table of each card
		//TODO: Check if the playable cards are ticked or not
		//TODO: Take the sum of the number of the playable cards from the currentDeck
		//TODO: Take the sum of the number of all the not-ticked cards
		//TODO: Divide to get the probability of playability
		//TODO: Play the card with highest probability
		
		
		return action;
	}
	
	/*
	 * Get a set of cards that are playable
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
				if(value == 5)
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
		Action action = null;
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
						action = new Action(player,this.toString(),ActionType.PLAY,j);
						break;
					}
				}
			}
			
			if(action != null)
			{
				break;
			}
		}
	
		//TODO: Play the damn card
		return action;
	}

	public Action tellAnyoneAboutUsefulCard(State s)
	{
		return null;
	}
	
	public Action osawaDiscard(State s)
	{
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
		return "Tuturu!";
	}
	
	/*
	 * Store the perspective of what each player knows about their card
	 */
	class Perspective 
	{
		private CardHistory[] cards;
		
		public Perspective(int n)
		{
			cards = new CardHistory[n];
		}
	}
	
	/*
	 * Store the probability what a card could be
	 */
	class CardHistory 
	{
		//change the cardhistory to 2-d boolean
		private boolean[][] notCard;
		private int number;
		private Colour colour;
		
		public CardHistory()
		{
			notCard = new boolean[5][5];
			number = -1;
			colour = null;
		}
		
		public void reset(int[][] deck)
		{
			for(int row = 0; row < deck.length; row++)
			{
				for(int col = 0; col < deck[0].length; col++)
				{
					if(deck[row][col] == 0)
					{
						notCard[row][col] = true;
					}
				}
			}
			number = -1;
			colour = null;
		}
		
		public void updateNotColour(Colour c)
		{
			int column = getColourIndex(c);
			for(int i = 0; i < notCard.length; i++)
			{
				notCard[i][column] = true;
			}
		}
		
		public void updateColour(Colour c)
		{
			int column = getColourIndex(c);
			for(int i = 0; i < notCard.length; i++)
			{
				if(i == column)
				{
					continue;
				}
				for(int j = 0; j < notCard.length; j++)
				{
					notCard[j][i] = true;
				}
			}
		}
		
		public void updateNotValue(int v)
		{
			int row = v-1;
			for(int i = 0; i < notCard.length; i++)
			{
				notCard[row][i] = true;
			}
		}
		
		public void updateValue(int v)
		{
			int row = v-1;
			for(int i = 0; i < notCard.length; i++)
			{
				if(i == row)
				{
					continue;
				}
				for(int j = 0; j < notCard.length; j++)
				{
					notCard[i][j] = true;
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
