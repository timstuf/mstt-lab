package nure.com.agents.lb2.wumpusworld.core.probability.mdp.impl;

import java.util.HashMap;
import java.util.Map;

import nure.com.agents.lb2.wumpusworld.core.agent.Action;
import nure.com.agents.lb2.wumpusworld.core.probability.mdp.Policy;

/**
 * Default implementation of the Policy interface using an underlying Map to
 * look up an action associated with a state.
 *
 * @param <S> the state type.
 * @param <A> the action type.
 * @author Ciaran O'Reilly
 */
public class LookupPolicy<S, A extends Action> implements Policy<S, A> {
	private Map<S, A> policy = new HashMap<S, A>();

	public LookupPolicy(Map<S, A> aPolicy) {
		policy.putAll(aPolicy);
	}

	//
	// START-Policy
	@Override
	public A action(S s) {
		return policy.get(s);
	}

	// END-Policy
	//
}
