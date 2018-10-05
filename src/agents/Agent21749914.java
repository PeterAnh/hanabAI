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
					cardIndex = previous.getCard();
					c = temp.getHand(player)[cardIndex].getColour();
					colourIndex = getColourIndex(c); 
					v = temp.getHand(player)[cardIndex].getValue();
					
					currentDeck[v-1][colourIndex]--;

					history[player].cards[cardIndex].reset(currentDeck);
					break;
				case DISCARD:
					cardIndex = previous.getCard();
					c = temp.getHand(player)[cardIndex].getColour();
					colourIndex = getColourIndex(c); 
					v = temp.getHand(player)[cardIndex].getValue();
					
					currentDeck[v-1][colourIndex]--;

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
		return null;
	}
	
	/*
	 * playSafeCard: Plays a card only if it is guaranteed 
	 * that it is playable
	 */
	public Action playSafeCard (State s)
	{
		Action a = null;
		int player = s.getObserver();
		//TODO: Check the fireworks to see which card can be played for each color
		for(int i = 0; i < colourArray.length; i++)
		{
			int v = s.getFirework(colourArray[i]).peek().getValue();
			Colour c = s.getFirework(colourArray[i]).peek().getColour();
			
			//TODO: Check which card you already know
			
			for(int j = 0; j < history[player].cards.length; j++)
			{
				if(history[player].cards[j].number == v+1)
				{
					if(history[player].cards[j].colour == c)
					{
						a = new Action(player,toString(),)
					}
				}
			}
		}
		
		
		//TODO: Play the damn card
		return null;
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
