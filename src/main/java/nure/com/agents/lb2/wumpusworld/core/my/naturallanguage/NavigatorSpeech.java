package nure.com.agents.lb2.wumpusworld.core.my.naturallanguage;

import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusAction;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusPercept;

public interface NavigatorSpeech {
	String tellAction(WumpusAction action);

	WumpusPercept recognize(String speech);
}
