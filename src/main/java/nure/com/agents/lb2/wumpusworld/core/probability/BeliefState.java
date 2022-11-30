package nure.com.agents.lb2.wumpusworld.core.probability;

public interface BeliefState<Action, Percept> {
	void update(Action action, Percept percept);

}
