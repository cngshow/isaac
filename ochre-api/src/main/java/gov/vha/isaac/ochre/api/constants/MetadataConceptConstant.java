package gov.vha.isaac.ochre.api.constants;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class MetadataConceptConstant implements ConceptSpecification {

    private final String primaryName_;
    private final List<String> synonyms_ = new ArrayList<>();
    private final List<String> definitions_ = new ArrayList<>();
    private final UUID uuid_;
    private ConceptSpecification parent_ = null;  //Optional use - most constants have their parent set by the IsaacMetadataAuxiliary parent/child mechanism

    protected MetadataConceptConstant(String primaryName, UUID uuid) {
        primaryName_ = primaryName;
        uuid_ = uuid;
    }

    protected MetadataConceptConstant(String primaryName, UUID uuid, String definition) {
        primaryName_ = primaryName;
        uuid_ = uuid;
        addDefinition(definition);
    }
    
    protected MetadataConceptConstant(String primaryName, UUID uuid, String definition, ConceptSpecification parent) {
        primaryName_ = primaryName;
        uuid_ = uuid;
        addDefinition(definition);
        setParent(parent);
    }

    protected void addSynonym(String synonym) {
        synonyms_.add(synonym);
    }

    protected void addDefinition(String definition) {
        definitions_.add(definition);
    }

    /**
     * @return The name for this concept, used to construct the FSN and preferred term.  
     * This method is identical to {@link #getConceptDescriptionText()}
     */
    public String getPrimaryName() {
        return primaryName_;
    }

    /**
     * @return The alternate synonyms for this concept (if any) - does not
     * include the preferred synonym. Will not return null.
     */
    public List<String> getSynonyms() {
        return synonyms_;
    }

    /**
     * @return The descriptions for this concept (if any). Will not return null.
     */
    public List<String> getDefinitions() {
        return definitions_;
    }

    /**
     * @return The UUID for the concept
     */
    public UUID getUUID() {
        return uuid_;
    }
    
    public ConceptSpecification getParent() {
        return parent_;
    }
    
    protected void setParent(ConceptSpecification parent)
    {
        parent_ = parent;
    }
    
    @Override
    public UUID getPrimordialUuid() {
        return getUUID();
    }

    /**
     * This method is identical to {@link #getPrimaryName()}
     * @see gov.vha.isaac.ochre.api.component.concept.ConceptSpecification#getConceptDescriptionText()
     */
    @Override
    public String getConceptDescriptionText() {
        return primaryName_;
    }

    @Override
    public List<UUID> getUuidList() {
        return Arrays.asList(new UUID[] { uuid_ });
    }

    /**
     * @return The nid for the concept.
     */
    public int getNid() {
        return Get.conceptService().getConcept(getUUID()).getNid();
    }

    /**
     * @return The concept sequence for the concept.
     */
    public int getSequence() {
        return Get.conceptService().getConcept(getUUID()).getConceptSequence();
    }

}
