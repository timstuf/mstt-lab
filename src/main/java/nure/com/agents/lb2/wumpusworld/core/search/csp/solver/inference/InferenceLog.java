package nure.com.agents.lb2.wumpusworld.core.search.csp.solver.inference;

import nure.com.agents.lb2.wumpusworld.core.search.csp.CSP;
import nure.com.agents.lb2.wumpusworld.core.search.csp.Variable;

/**
 * Provides information about (1) whether changes have been performed, (2) possibly inferred empty domains , and
 * (3) how to restore the CSP.
 *
 * @author Ruediger Lunde
 */
public interface InferenceLog<VAR extends Variable, VAL> {
	/**
	 * Returns an empty inference log.
	 */
	static <VAR extends Variable, VAL> InferenceLog<VAR, VAL> emptyLog() {
		return new InferenceLog<VAR, VAL>() {
			@Override
			public boolean isEmpty() {
				return true;
			}

			@Override
			public boolean inconsistencyFound() {
				return false;
			}

			@Override
			public void undo(CSP<VAR, VAL> csp) {
			}
		};
	}

	boolean isEmpty();

	boolean inconsistencyFound();

	void undo(CSP<VAR, VAL> csp);
}
