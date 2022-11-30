package nure.com.agents.lb2.wumpusworld.core.my;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nure.com.agents.lb2.wumpusworld.core.environment.wumpusworld.WumpusPercept;

@AllArgsConstructor
@ToString
@Getter
@NoArgsConstructor
public class State {
	WumpusPercept percept;
	int tick;
}
