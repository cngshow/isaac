package gov.vha.isaac.ochre.api.index;

/**
 * {@link SearchResult}
 * Interface to serve as a common parent to ComponentSearchResult and ConceptSearchResult
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public interface SearchResult
{
	public int getNid();

	public float getScore();
}
