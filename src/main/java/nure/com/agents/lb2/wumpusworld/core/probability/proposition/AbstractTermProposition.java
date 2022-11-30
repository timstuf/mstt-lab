package nure.com.agents.lb2.wumpusworld.core.probability.proposition;

import nure.com.agents.lb2.wumpusworld.core.probability.RandomVariable;

public abstract class AbstractTermProposition extends AbstractProposition
		implements TermProposition {

	private RandomVariable termVariable = null;

	public AbstractTermProposition(RandomVariable var) {
		if(null == var) {
			throw new IllegalArgumentException(
					"The Random Variable for the Term must be specified.");
		}
		this.termVariable = var;
		addScope(this.termVariable);
	}

	public RandomVariable getTermVariable() {
		return termVariable;
	}
}
