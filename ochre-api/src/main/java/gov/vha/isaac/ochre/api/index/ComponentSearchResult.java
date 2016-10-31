package gov.vha.isaac.ochre.api.index;

/**
 * Class to termstore the nid of a component that matches a search, and the
 * sore of that component's match.
 * @author kec
 */
public class ComponentSearchResult implements SearchResult {

    /**
     * The native id of the component that matches the search.
     */
    public int nid;

    /**
     * The score of the components match relative to other matches.
     */
    public float score;

    public ComponentSearchResult(int nid, float score) {
        this.nid   = nid;
        this.score = score;
    }

    @Override
    public int getNid() {
        return nid;
    }

    public void setNid(int nid) {
        this.nid = nid;
    }

    @Override
    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}
