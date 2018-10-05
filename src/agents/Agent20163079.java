package agents;

import java.util.ArrayDeque;

import hanabAI.Action;
import hanabAI.ActionType;
import hanabAI.Agent;
import hanabAI.Colour;
import hanabAI.IllegalActionException;
import hanabAI.State;

public class Agent20163079 implements Agent {
	class CardHistory {
		private boolean[] notNumber;
		private boolean[] notColour;
		private int number;
		private Colour colour;
		
		CardHistory() {
			notNumber = new boolean[5];
			notColour = new boolean[5];
			number = -1;
			colour = null;
		}
		
		public void reset(int[][] deck) {
			notNumber = new boolean[5];
			notColour = new boolean[5];
			for(int i=0; i<deck.length; i++) {
				for(int j=0; j<deck[0].length; j++) {
					if(deck[i][j] == 0) {
						
					}
				}
			}
		}
	}

	class Perspective {
		private CardHistory[] cards;

		Perspective(int number_cards) {
			cards = new CardHistory[number_cards];
		}
	}

	private Perspective[] history;
	private int[][] currentdeck;
	private boolean firstAction = true;

	/**
	 * Initializes variables on the first call to do action.
	 * @param s the State of the game at the first action
	 **/
	public void init(State s){
		history = new Perspective[s.getPlayers().length];
		firstAction = false;
		currentdeck = new int[5][5];
		for(int i=0; i<currentdeck.length; i++) {
			for(int j=0; j<currentdeck[0].length; j++) {
				switch(i) {
				case 0:
					currentdeck[i][j] = 3;
					break;
				case 1:
				case 2:
				case 3:
					currentdeck[i][j] = 2;
					break;
				default:
					currentdeck[i][j] = 1;
				}

			}
		}
	}

	/**
	 * Given the state, return the action that the strategy chooses for this state.
	 * @return the action the agent chooses to perform
	 * */
	public Action doAction(State s) {
		if(firstAction){
			init(s);
		} 
		return null;
	}

	public void updateHand(State s) {
		int num = s.getPlayers().length;
		ArrayDeque<State> stack = new ArrayDeque<State>();
		State current = s;

		for(int i=0; i<num; i++) {
			if(current.getPreviousState() == null) {
				break;
			}
			stack.push(s);
			current = current.getPreviousState();
		}
		
		while(!stack.isEmpty())
		{
			current = stack.pop();
			Action act = current.getPreviousAction();
			ActionType type = act.getType();
			int player = act.getPlayer();
			
			//PLAY,DISCARD,HINT_COLOUR,HINT_VALUE
			switch (type) {
				case PLAY:
					try {
						int cardIndex = act.getCard();
						history[player].cards[cardIndex] = new CardHistory();
						
					} catch (IllegalActionException e) {
						e.printStackTrace();
					}
					break;
				case DISCARD:
					
					
				
			}
			
			
			
		}
	}

	/**
	 * PlayProbablySafeCard(Threshold between [0, 1]): Plays the
	 * card that is the most likely to be playable if it is at least
	 * as probable as Threshold
	 * @param s State of the game
	 * @param probability probability between 0 and 1
	 * @return action
	 */
	public Action playProbablySafeCard(State s, double probability) {

		return null;
	}

	/**
	 * Reports the agents name
	 * */
	public String toString() {
		return "Agent Smith";
	}

}

