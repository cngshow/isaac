package gov.vha.isaac.ochre.api.index;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link ConceptSearchResult}
 * Class to support merging search results based on the concepts that are associated with.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ConceptSearchResult implements SearchResult
{
	/**
	 * The sequence ID of the concept most closely related to the search result (the concept referenced by a description, for example)
	 */
	public int conceptSequence;

	/**
	 * The native id of the component(s) that matches the search.
	 */
	public Set<Integer> nids = new HashSet<>();

	/**
	 * The score of the component with the best score, relative to the other matches.
	 */
	public float bestScore;

	public ConceptSearchResult(int conceptSequence, int componentNid, float score)
	{
		this.conceptSequence = conceptSequence;
		nids.add(componentNid);
		this.bestScore = score;
	}
	
	public int getConceptSequence()
	{
		return conceptSequence;
	}

	public float getBestScore()
	{
		return bestScore;
	}

	public Collection<? extends Integer> getMatchingComponents()
	{
		return nids;
	}
	
	public void merge(ConceptSearchResult other)
	{
		if (conceptSequence != other.conceptSequence)
		{
			throw new RuntimeException("Unmergeable!");
		}
		if (other.bestScore > bestScore)
		{
			bestScore = other.bestScore;
		}
		nids.addAll(other.getMatchingComponents());
	}
	
	public void merge(SearchResult other)
	{
		if (other.getScore() > bestScore)
		{
			bestScore = other.getScore();
		}
		nids.add(other.getNid());
	}

	/**
	 * Returns (an arbitrary) match nid from the set of component match nids
	 */
	@Override
	public int getNid()
	{
		return nids.iterator().next();
	}

	/**
	 * returns {@link #getBestScore()}
	 */
	@Override
	public float getScore()
	{
		return getBestScore();
	}
}
