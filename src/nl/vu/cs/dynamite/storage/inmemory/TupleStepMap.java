package nl.vu.cs.dynamite.storage.inmemory;

import java.util.Map;
import java.util.Set;

import nl.vu.cs.ajira.data.types.Tuple;
import nl.vu.cs.dynamite.storage.Pattern;

/**
 * An in-memory tuples set that enables retrieval through Pattern matching
 */
public interface TupleStepMap extends Map<Tuple, Integer> {

	/**
	 * Return the subset of all the elements that satisfy the given pattern
	 * 
	 * @param p
	 *            the pattern
	 * @return the subset of all the elements that satisfy p
	 * @throws Exception
	 *             if pattern evaluation is not possible
	 */
	public Set<Tuple> getSubset(Pattern p) throws Exception;

}
