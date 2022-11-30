package nure.com.agents.lb2.wumpusworld.core.probability.mdp;

import java.util.Set;

import nure.com.agents.lb2.wumpusworld.core.agent.Action;

/**
 * An interface for MDP action functions.
 *
 * @param <S> the state type.
 * @param <A> the action type.
 * @author Ciaran O'Reilly
 * @author Ravi Mohan
 */
public interface ActionsFunction<S, A extends Action> {
	/**
	 * Get the set of actions for state s.
	 *
	 * @param s the state.
	 * @return the set of actions for state s.
	 */
	Set<A> actions(S s);
}
