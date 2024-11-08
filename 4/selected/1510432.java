package core.exception;

/**
 * An {@code AnalyzeException} is the superclass of all exceptions thrown
 * during the {@link core.sim.Simulator Simulator}'s analysing process if the
 * input restrictions are broken. This happens if the priorities form a
 * directed cycle or if both readers and writers from a high-low priority pair
 * are not disjunct. Both cases are ambiguous in their ordering solution and
 * behaviour, and theirfore not supported by the {@link core.sim.Simulator
 * Simulator}.
 *
 * @see DirectedPriorityCycleException
 * @see NondisjunctReadersWritersException
 * 
 * @author torben
 */
public abstract class AnalyzeException extends Exception {

    /**
	 * An {@code DirectedPriorityCycleException} is thrown during the {@link
	 * core.sim.Simulator Simulator}'s analysing process if the priorities form
	 * a directed cycle.
	 */
    public static final class DirectedPriorityCycleException extends AnalyzeException {

        @Override
        public final String getMessage() {
            return "A directed priority cycle was found during the analysing process";
        }
    }

    /**
	 * An {@code NondisjunctReadersWritersException} is thrown during the {@link
	 * core.sim.Simulator Simulator}'s analysing process if both readers and
	 * writers from a high-low priority pair are not disjunct.
	 */
    public static final class NondisjunctReadersWritersException extends AnalyzeException {

        @Override
        public final String getMessage() {
            return "A priority pair with nondisjunct readers and writers was found during the analyzsing process";
        }
    }
}
