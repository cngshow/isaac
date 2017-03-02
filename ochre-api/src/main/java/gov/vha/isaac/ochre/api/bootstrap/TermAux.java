package gov.vha.isaac.ochre.api.bootstrap;

import gov.vha.isaac.ochre.api.ConceptProxy;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import java.util.UUID;

/**
 * This class only contains manually created, hard coded constants which need to exist earlier in the classpath 
 * than {@link MetaData} is available.  If you are down the classpath far enough to have MetaData, use that class.
 * 
 * Data created here does NOT automatically get created as actual concepts.  All concepts here must be referenced 
 * by {@link IsaacMetadataAuxiliary}, or they will not get created at runtime, and you will have broken references in your DB.
 * {@link TermAux}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class TermAux {

    public static ConceptSpecification ISAAC_ROOT
            = new ConceptProxy("ISAAC root",
                    UUID.fromString("7c21b6c5-cf11-5af9-893b-743f004c97f5"));
    public static ConceptSpecification DATABASE_UUID
            = new ConceptProxy("Database UUID",
                    UUID.fromString("49b882a1-05e4-52cf-96d8-5de024b24632"));
    public static ConceptSpecification USER
            = new ConceptProxy("user",
                    UUID.fromString("f7495b58-6630-3499-a44e-2052b5fcf06c"));
    public static ConceptSpecification IS_A
            = new ConceptProxy("is a (relationship type)",
                    UUID.fromString("46bccdc4-8fb6-11db-b606-0800200c9a66"));
    public static ConceptSpecification IHTSDO_CLASSIFIER
            = new ConceptProxy("IHTSDO Classifier",
                    UUID.fromString("7e87cc5b-e85f-3860-99eb-7a44f2b9e6f9"));
    public static ConceptSpecification PATH
            = new ConceptProxy("path",
                    UUID.fromString("4459d8cf-5a6f-3952-9458-6d64324b27b7"));
    public static ConceptSpecification PATH_ORIGIN_ASSEMBLAGE // IsaacMetadataAuxiliary has "path origins assemblage", TermAux has "Path origin reference set"
            = new ConceptProxy("Path origin reference set",
                    UUID.fromString("1239b874-41b4-32a1-981f-88b448829b4b"));
    public static ConceptSpecification PATH_ASSEMBLAGE // IsaacMetadataAuxiliary has "paths assemblage", TermAux has "paths"
            = new ConceptProxy("paths",
                    UUID.fromString("fd9d47b7-c0a4-3eea-b3ab-2b5a3f9e888f"));
    public static ConceptSpecification SNOMED_IDENTIFIER
            = new ConceptProxy("SNOMED integer id",
                    UUID.fromString("0418a591-f75b-39ad-be2c-3ab849326da9"),   // 'SNOMED integer id'
                    UUID.fromString("87360947-e603-3397-804b-efd0fcc509b9"));  //'SNOMED CT integer identifier (core metadata concept)' - 900000000000294009
    public static ConceptSpecification ASSEMBLAGE  //formerly known as REFSET_IDENTITY
            = new ConceptProxy("assemblage",
                    UUID.fromString("3e0cd740-2cc6-3d68-ace7-bad2eb2621da"));
    public static ConceptSpecification UNSPECIFIED_MODULE
            = new ConceptProxy("Module (core metadata concept)",  //simply labeled 'module' in IsaacMetadataAuxiliary
                    UUID.fromString("40d1c869-b509-32f8-b735-836eac577a67"));
    public static ConceptSpecification ISAAC_MODULE
            = new ConceptProxy("ISAAC Module",
                    UUID.fromString("f680c868-f7e5-5d0e-91f2-615eca8f8fd2"));

    //ConceptSpecs for Description types
    // SCT ID:    900000000000013009
    public static ConceptSpecification SYNONYM_DESCRIPTION_TYPE
            = new ConceptProxy("synonym",
                    UUID.fromString("8bfba944-3965-3946-9bcb-1e80a5da63a2"));

    // IsaacMetadataAuxiliary has "fully specified name", TermAux has "Fully specified name (core metadata concept)"
     public static ConceptSpecification FULLY_SPECIFIED_DESCRIPTION_TYPE 
            = new ConceptProxy("Fully specified name (core metadata concept)",
                    UUID.fromString("00791270-77c9-32b6-b34f-d932569bd2bf"));
    // IsaacMetadataAuxiliary has "definition description type", TermAux has "Definition (core metadata concept)"
    public static ConceptSpecification DEFINITION_DESCRIPTION_TYPE 
            = new ConceptProxy("Definition (core metadata concept)",
                    UUID.fromString("700546a3-09c7-3fc2-9eb9-53d318659a09"));
    
    //Needed within DynamicSememeMetadata constants, which can't referenced IsaacMetadataAuxiliary
    public static ConceptSpecification DESCRIPTION_TYPE_IN_SOURCE_TERMINOLOGY
            = new ConceptProxy("description type in source terminology",
                    UUID.fromString("ef7d9808-a839-5119-a604-b777268eb719"));
    
    public static ConceptSpecification RELATIONSHIP_TYPE_IN_SOURCE_TERMINOLOGY
            = new ConceptProxy("relationship type in source terminology",
                    UUID.fromString("46bc0e6b-0e64-5aa6-af27-a823e9156dfc"));
    
    //ConceptSpecs for language refsets
    // IsaacMetadataAuxiliary has "US English dialect", TermAux has "United States of America English language reference set"
    public static ConceptSpecification US_DIALECT_ASSEMBLAGE 
            = new ConceptProxy("United States of America English language reference set",
                    UUID.fromString("bca0a686-3516-3daf-8fcf-fe396d13cfad"));
    
    // IsaacMetadataAuxiliary has "GB English dialect", TermAux has "Great Britain English language reference set"
    public static ConceptSpecification GB_DIALECT_ASSEMBLAGE 
            = new ConceptProxy("Great Britain English language reference set",
                    UUID.fromString("eb9a5e42-3cba-356d-b623-3ed472e20b30"));

    public static ConceptSpecification ENGLISH_LANGUAGE
            = new ConceptProxy("English language", 
                    UUID.fromString("06d905ea-c647-3af9-bfe5-2514e135b558"));

    public static ConceptSpecification SPANISH_LANGUAGE
            = new ConceptProxy("Spanish language", 
                    "0fcf44fb-d0a7-3a67-bc9f-eb3065ed3c8e");

    public static ConceptSpecification FRENCH_LANGUAGE
            = new ConceptProxy("French language", 
                    "8b23e636-a0bd-30fb-b8e2-1f77eaa3a87e");

    public static ConceptSpecification DANISH_LANGUAGE
            = new ConceptProxy("Danish language", 
                    "7e462e33-6d94-38ae-a044-492a857a6853");

    public static ConceptSpecification POLISH_LANGUAGE
            = new ConceptProxy("Polish language", 
                    "c924b887-da88-3a72-b8ea-fa86990467c9");

    public static ConceptSpecification DUTCH_LANGUAGE
            = new ConceptProxy("Dutch language", 
                    "674ad858-0224-3f90-bcf0-bc4cab753d2d");

    public static ConceptSpecification LITHUANIAN_LANGUAGE
            = new ConceptProxy("Lithuanian language", 
                    "e9645d95-8a1f-3825-8feb-0bc2ee825694");

    public static ConceptSpecification CHINESE_LANGUAGE
            = new ConceptProxy("Chinese language", 
                    "ba2efe6b-fe56-3d91-ae0f-3b389628f74c");

    public static ConceptSpecification JAPANESE_LANGUAGE
            = new ConceptProxy("Japanese language", 
                    "b90a1097-29e3-42bc-8576-8e8eb6715c44");

    public static ConceptSpecification SWEDISH_LANGUAGE
            = new ConceptProxy("Swedish language", 
                    "9784a791-8fdb-32f7-88da-74ab135fe4e3");

    public static ConceptSpecification ENGLISH_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("English description assemblage", 
                    "45021920-9567-11e5-8994-feff819cdc9f");
    
    public static ConceptSpecification SPANISH_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("Spanish description assemblage", 
                    "45021c36-9567-11e5-8994-feff819cdc9f");
    
    public static ConceptSpecification FRENCH_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("French description assemblage", 
                    "45021dbc-9567-11e5-8994-feff819cdc9f");
    
    public static ConceptSpecification DANISH_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("Danish description assemblage", 
                    "45021f10-9567-11e5-8994-feff819cdc9f");
    
    public static ConceptSpecification POLISH_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("Polish description assemblage", 
                    "45022140-9567-11e5-8994-feff819cdc9f");
    
    public static ConceptSpecification DUTCH_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("Dutch description assemblage", 
                    "45022280-9567-11e5-8994-feff819cdc9f");
    
    public static ConceptSpecification LITHUANIAN_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("Lithuanian description assemblage", 
                    "45022410-9567-11e5-8994-feff819cdc9f");
    
    public static ConceptSpecification CHINESE_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("Chinese description assemblage", 
                    "45022532-9567-11e5-8994-feff819cdc9f");
    
    public static ConceptSpecification JAPANESE_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("Japanese description assemblage", 
                    "450226cc-9567-11e5-8994-feff819cdc9f");

    public static ConceptSpecification SWEDISH_DESCRIPTION_ASSEMBLAGE
            = new ConceptProxy("Swedish description assemblage", 
                    "45022848-9567-11e5-8994-feff819cdc9f");

     // SCT ID:    900000000000548007
    public static ConceptSpecification PREFERRED
            = new ConceptProxy("preferred",
                    UUID.fromString("266f1bc3-3361-39f3-bffe-69db9daea56e"));
    // SCT ID:    900000000000549004
    public static ConceptSpecification ACCEPTABLE
            = new ConceptProxy("acceptable",
                    UUID.fromString("12b9e103-060e-3256-9982-18c1191af60e"));
    
    // SCT ID:    900000000000017005
    public static ConceptSpecification DESCRIPTION_CASE_SENSITIVE
            = new ConceptProxy("description case sensitive",
                    UUID.fromString("0def37bc-7e1b-384b-a6a3-3e3ceee9c52e"));
    
    // SCT ID:    900000000000448009
    public static ConceptSpecification DESCRIPTION_NOT_CASE_SENSITIVE
            = new ConceptProxy("description not case sensitive",
                    UUID.fromString("ecea41a2-f596-3d98-99d1-771b667e55b8"));
    // SCT ID:    900000000000020002
    public static ConceptSpecification DESCRIPTION_INITIAL_CHARACTER_SENSITIVE
            = new ConceptProxy("description initial character sensitive",
                    UUID.fromString("17915e0d-ed38-3488-a35c-cda966db306a"));
    
    public static ConceptSpecification DEVELOPMENT_PATH
            = new ConceptProxy("development path",
                    UUID.fromString("1f200ca6-960e-11e5-8994-feff819cdc9f"));
    
    public static ConceptSpecification MASTER_PATH
            = new ConceptProxy("master path",
                    UUID.fromString("1f20134a-960e-11e5-8994-feff819cdc9f"));
    
    public static ConceptSpecification VHA_MODULES
            = new ConceptProxy("VHA modules",
                    UUID.fromString("8aa5fda8-33e9-5eaf-88e8-dd8a024d2489"));
    
    public static ConceptSpecification SOLOR_OVERLAY_MODULE
            = new ConceptProxy("SOLOR overlay module",
                    UUID.fromString("9ecc154c-e490-5cf8-805d-d2865d62aef3" ));
    
    public static ConceptSpecification EL_PLUS_PLUS_INFERRED_ASSEMBLAGE
            = new ConceptProxy("EL++ inferred form assemblage",
                    UUID.fromString("1f20182c-960e-11e5-8994-feff819cdc9f"));
    
    public static ConceptSpecification EL_PLUS_PLUS_STATED_ASSEMBLAGE
            = new ConceptProxy("EL++ stated form assemblage",
                    UUID.fromString("1f201994-960e-11e5-8994-feff819cdc9f"));
    
    public static ConceptSpecification EL_PLUS_PLUS_LOGIC_PROFILE // IsaacMetadataAuxiliary has "EL++ profile", TermAux has "EL++ logic profile"
            = new ConceptProxy("EL++ logic profile",
                    UUID.fromString("1f201e12-960e-11e5-8994-feff819cdc9f"));
    
    public static ConceptSpecification SNOROCKET_CLASSIFIER // IsaacMetadataAuxiliary has "SnoRocket classifier", TermAux has "Snorocket classifier"
            = new ConceptProxy("Snorocket classifier",
                    UUID.fromString("1f201fac-960e-11e5-8994-feff819cdc9f"));
    
    public static ConceptSpecification ROLE_GROUP
            = new ConceptProxy("role group",
                    UUID.fromString("a63f4bf2-a040-11e5-8994-feff819cdc9f"));

    public static int getDescriptionAssemblageConceptSequence(int languageConceptSequence) {
        if (languageConceptSequence == ENGLISH_LANGUAGE.getConceptSequence()) {
            return ENGLISH_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        if (languageConceptSequence == SPANISH_LANGUAGE.getConceptSequence()) {
            return SPANISH_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        if (languageConceptSequence == FRENCH_LANGUAGE.getConceptSequence()) {
            return FRENCH_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        if (languageConceptSequence == DANISH_LANGUAGE.getConceptSequence()) {
            return DANISH_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        if (languageConceptSequence == POLISH_LANGUAGE.getConceptSequence()) {
            return POLISH_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        if (languageConceptSequence == DUTCH_LANGUAGE.getConceptSequence()) {
            return DUTCH_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        if (languageConceptSequence == LITHUANIAN_LANGUAGE.getConceptSequence()) {
            return LITHUANIAN_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        if (languageConceptSequence == CHINESE_LANGUAGE.getConceptSequence()) {
            return CHINESE_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        if (languageConceptSequence == JAPANESE_LANGUAGE.getConceptSequence()) {
            return JAPANESE_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        if (languageConceptSequence == SWEDISH_LANGUAGE.getConceptSequence()) {
            return SWEDISH_DESCRIPTION_ASSEMBLAGE.getConceptSequence();
        }
        throw new RuntimeException("No description assemblage for: " + 
                Get.conceptDescriptionText(languageConceptSequence));
    }
    
    public static ConceptSpecification getConceptSpecificationForLanguageSequence(int languageConceptSequence) {
        if (languageConceptSequence == ENGLISH_LANGUAGE.getConceptSequence()) {
            return ENGLISH_LANGUAGE;
        }
        if (languageConceptSequence == SPANISH_LANGUAGE.getConceptSequence()) {
            return SPANISH_LANGUAGE;
        }
        if (languageConceptSequence == FRENCH_LANGUAGE.getConceptSequence()) {
            return FRENCH_LANGUAGE;
        }
        if (languageConceptSequence == DANISH_LANGUAGE.getConceptSequence()) {
            return DANISH_LANGUAGE;
        }
        if (languageConceptSequence == POLISH_LANGUAGE.getConceptSequence()) {
            return POLISH_LANGUAGE;
        }
        if (languageConceptSequence == DUTCH_LANGUAGE.getConceptSequence()) {
            return DUTCH_LANGUAGE;
        }
        if (languageConceptSequence == LITHUANIAN_LANGUAGE.getConceptSequence()) {
            return LITHUANIAN_LANGUAGE;
        }
        if (languageConceptSequence == CHINESE_LANGUAGE.getConceptSequence()) {
            return CHINESE_LANGUAGE;
        }
        if (languageConceptSequence == JAPANESE_LANGUAGE.getConceptSequence()) {
            return JAPANESE_LANGUAGE;
        }
        if (languageConceptSequence == SWEDISH_LANGUAGE.getConceptSequence()) {
            return SWEDISH_LANGUAGE;
        }
        return Get.conceptSpecification(languageConceptSequence);
    }

    public static int caseSignificanceToConceptSequence(boolean initialCaseSignificant) {
        if (initialCaseSignificant) {
            return Get.identifierService().getConceptSequenceForUuids(TermAux.DESCRIPTION_CASE_SENSITIVE.getUuids());
        }
        return Get.identifierService().getConceptSequenceForUuids(TermAux.DESCRIPTION_NOT_CASE_SENSITIVE.getUuids());
    }

    public static boolean conceptIdToCaseSignificance(int id) {
        int nid = Get.identifierService().getConceptNid(id);
        return TermAux.DESCRIPTION_INITIAL_CHARACTER_SENSITIVE.getNid() == nid;
    }
}