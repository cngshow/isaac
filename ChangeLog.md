ISAAC Changelog 

This changelog summarizes changes and fixes which are a part of each revision.  For more details on the fixes, refer tracking numbers where provided, and the git commit history.

* 2017/08/?? - 5.01 - PENDING
    * Minor metadata naming change to be more consistent in the GUI
    * Fix for Property sememe lookup to get the needed string value in the XML Delta import
    * Fix for association sememe lookups to match the expected DynamicSememeDataType of UUID in the XML Delta Import
    * Fix for RuntimeExceptions thrown during Designation 'update' import directives in the XML Delta Import
    * Added VHAT_ASSOCIATION_TYPES constant
    * Changes to look for duplicate VUIDs in imported XML file prior to processing

* 2017/08/25 - 5.00
    * 537659 - Fixes for us extension processing
    * Rework UUID generation for central consistency, to allow delta processing to work later
    * Performance improvements on the Lucene indexer

* 2017/08/24 - 4.22
    * Fix yet more problems with the caching of users and roles.
    * Debug and other logging improvements.
    * VHATIsAHasParent updates.

* 2017/08/23 - 4.21
    * Fix a silly bug accidently introduced in the 4.20 build relating to identifer type sememes (code, vuid, etc)
    * Changes to VHATIsAHasParent code

* 2017/08/22 - 4.20
    * Cleaning up prior to code complete, minor bug fixes found during development testing.

* 2017/08/17 - 4.19
    * Updates to Frills, improving documentation in VHATIsAHasParentSynchronizingChronologyChangeListener and changing VHATIsAHasParentSynchronizingChronologyChangeListener to reuse retired has_parent association sememes where possible


* 2017/08/15 - 4.18
    * Updates for VHAT has parent bug fixes
    * Updating Lucene search capabilities to support native searching / filtering of queries by module / path.
    * Fix a VHAT import bug of a duplicate UUID during creation of a new relationship type.

* 2017/08/11 - 4.17
    * Lots of API changes to fix consistency issues.
    * Updates to IBDF merging algorithm.
    * Updates for VHAT has parent changes.

* 2017/08/03 - 4.16
    * Much internal refactoring to make proper use of constants rather that hard-coded UUIDs.
    * Added various VHAT constants to a VHAT package
    * published new VHAT constants as part of the YAML constants file
    * Fixed XML import bugs related to invalid constants being used
    * Fixed XML import commit bugs
    * Fixed XML import null pointer bugs when items for update had no designations
    * Fixed XML import null pointer bugs when vuid and code were not specified and autogenerate was expected
    * Fixed XML import bugs with recursive retire upon remove of a concept (nested items not being retired as expected)
    * Fixed XML import bug with moveFromConcept

* 2017/07/27 - 4.15
    * DATABASE COMPATIBILITY ISSUE - Updating Lucene version to current.  Upon start with an old database, the indexes will be deleted, and new indexes will be built.  This will take much longer than a normal startup.  The automatic reindex feature is a new feature.
    * Bug fixed in lucene index design, where queries with sememe filters did not find appropriate results on edits that had occurred after the initial index.
    * Bugs fixed relating to timing issues on when an item was committed, vs when it is available in the index.  Commit now does not complete until the index is also up to date.

* 2017/07/25 - 4.14
    * Changes for has_parent code found during testing.

* 2017/07/20 - 4.13
    * Adding isA / hasA relationship / association synchronization for commits to VHAT content.
    * Experimental hack fix to IsomorphicResultsBottomUp to prevent null pointer when processing logic graphs with multiple parents.

* 2017/07/20 - 4.12
    * Implementing vuid validation, vuid auto-generate, and moveFromConceptCode in the XML import.
    * update SCTID and VUID ID search convenience methods in Frills to wait for any in-progress index updates

* 2017/07/13 - 4.11
    * Changes for ISAAC-rest API

* 2017/07/06 - 4.10
    * In Frills.getTerminologyTypes, add metadata terminology type flag to any concept that is a child of ISAAC Metadata
    * Add a bit to the lucene index to mark which concepts are part of the metadata hierarchy - vastly improves the performance of metadata style searches.

* 2017/06/30 - 4.09
    * VHAT Delta import initial implementation
    * Fortify finding - XML External Entity (XXE).
    * Metadata changes for IBDF diff work.
    *  Adding SememeIndexerBI to ochre-api and adding query methods accepting Predicate filter to be available in Frills for use by new getVuidSememeNidsForVUID() method.

* 2017/06/22 - 4.08
    * Import cleanup.
    * Added missing documentation, swap the use of $$$ in the returned strings to --- to make the return truely URL safe.

* 2017/06/16 - 4.07
    * Adding support for NTRT role.
    * Initial stub out changes preparing for XML import work.

* 2017/06/08 - 4.06
    * Correct Frills.getDescriptionExtendedTypeConcept to only look at active values.
    * Fortify fixes.
    * Creating RestBoolean and modifying RestVuidBlockData to hold long
    instead of int.

* 2017/06/01 - 4.05
    * Changes to address Fortify issues
    * Commit latest changes to enable producing IBDF file of differences between two IBDF files to correctly obtain MetadataAuxiliaryVersion description.
    * Prisme integration changes

* 2017/05/21 - 4.04
    * fix a bug in the setRunLevel method, so that it doesn't fail if you ask it to shut down while it is still in the process of coming up from a previous request.
    * fix bugs in utility code that helped identify terminology types.
    * refactor more isaac-rest API code here for reuse.

* 2017/05/18 - 4.03
    * Lowercase extension names of things like snomed us extension when creating the artifact groups for consistency.
    * Fix DYNAMIC_SEMEME_REFERENCED_COMPONENT_RESTRICTION validator
    * Changed VHA_MODULES to VHAT_MODULES.  Add descriptions to other modules. Add VHAT_EDIT module.

* 2017/05/11 - 4.02
    * Improved debugging output during builds.
    * Added HL7 domain/subset checksum calculator and terminology configs.

* 2017/05/05 - 4.01
    * Moving User and UserCache service interface up from rest (refactoring for reuse)

* 2017/05/02 - 4.00
    * Jazz - 509497 - fix mistake in icd10 converter integration
    * Jazz 488177 and 488182 - minor wording changes to metadata equivalence type constants.

* 2017/04/27 - 3.45
    * Added SOPT as a supported converter for PRISME to pick up.
    * Fixed the documentation for the US Extension PRISME integration to point at the latest paths for download of the US Extension from the NLM. 

* 2017/04/20 - 3.44
    * ICD-10 CM/PCS PRISMe configurations added for source upload (Jazz: 501801, 501807)
    * Added CPT copyright and license files to PRISMe source uploader configuration
    * Changing to only expose loadTerminologyMetadataAttributes() that sets
terminologyMetadataRootConcept to getModule()
    * Adding to addParents() check of appropriate cache of concepts to which
parents have been added to prevent adding parents twice

* 2017/04/11 - 3.43
    * Fix a bug with Frills.getAllChildrenOfConcept where it didn't return the proper list of children in certain cases, which inadvertently led to 
        the warnings "SememeAPIs.get(...) didn't filter properly..." being frequently logged by isaac-rest.  
    * Added metadata entry for ICD10 modules for ICD10 terminology importer
    * Updating libraries in master pom file to current

* 2017/03/20 - 3.42
    * More Fortify fixes
    * Better loggin on DB creation failures
    * Production build for Release 3

* 2017/03/16 - 3.41
    * Cleanup debug logging
    * Close HL7 response listener
    * Fortify fix

* 2017/03/15 - 3.40
    * prevent duplicate tag push / push failure issue with source upload and content converter creator by locking on the repo.  Note, this simplistic 
        solution only works if we assume that this PRISME instace is the only instance using a particular changeset config repo (which should be
        the case in our VA deployments) Jazz 479051
    * Add missing documentation to CPT upload dialog to correct Jazz 476865
    * Fix issues with missing temp folder cleanup after git operations
    * Fix various parsing / alignment bugs with HL7 message parsing / processing

* 2017/03/08 - 3.39
    * Improved debugging code for chasing various issues in AITC.

* 2017/03/02 - 3.38
    * update loader to allow the passing a a folder that contains ibdf files, rather than requiring specific lists.  part of jazz 468085
    * add nucc and cvx to prisme integrations.  jazz 469791, 469788
    * refactoring for reduced bugs on prisme integrations, updates for content-per-module design changes.  jazz 468087
    * Much work on the backend code for HL7 messaging protocol to VistA sites via VIE (checksum, discovery)

* 2017/02/16 - 3.37
    * refix 460134 defect with HL7v3 source upload (again)
    * VetsExporter code modified to no rely on string identifiers for mapping types
    * Update metadata constant descriptions, definitions for mapping related column headers (jazz ids 440248 and 440195)
    * Adding constants for IPO mapset columns (related to jazz id 462456) 
    * Adding a constant for Name
    * VetsExporter fix for defect 461049
    * read any state description (active or inactive) when reading dynamic sememe column names

* 2017/02/09 - 3.36
    * Adding code for mapping dynamic columns.
    * HL7 messageing integration work.
    * Changing remaining instances of qualifier to equivalence type.
    * Disabled unused VHAT Export code path.

* 2017/02/04 - 3.35
    * Bug fix for terminology converter shared code

* 2017/02/03 - 3.34
    * Fix for Property exports, defect 452239
    * 444799 - HL7 upload configuration details added for PRISME to pick up

* 2017/01/26 - 3.33
    * Make the GitPublish methods used by the source upload / convert / db builder portions of prisme automatically create the remote git repository if it doesn't exist.
    * HL7 messaging callback and testing.
    * Fortify code changes.
    
* 2017/01/19 - 3.32
    * Added configuration options for remote service configs (like git)
    * Tweaked the way that DBIDs were created / stored, added the IDs to a sememe on root, and to the changeset data files.
    * Fix bugs with shutdown sequence, which would crop up during a corrupt DB recovery
    * fix a bug with the lucene index config, where it cached a reference to the data store location which it shouldn't have
    * Added the git syncronization service.  If the configuration service is configured with valid GIT details, changeset files
        will be automatically synced up and down from a database specific git repository. 

* 2017/01/17 - 3.31
    * Workflow changes to align with web, correct time conversion.
    * Refactoring the gitblit repo create code.
    * Add a UUID to the DB, when the db is created, to aid in changeset repo alignment.
    * Adding ISAAC_DEPENDENTS_RUNLEVEL = 5

* 2017/01/05 - 3.30
    * Adding MAPPING_QUALIFIER_UNMAPPED as child of MAPPING_QUALIFIERS
    * Ensure system shuts down cleanly preventing database locks from corrupting database
    * Add mechanism to review database to ensure isn't corrupt
    * Fix an issue that caused invalid UUIDs to be generated after adding the semantic tag into FSNs of metadata concepts
    * Clean up APIs to make it clear what is going on
    * Adding non-logging getServiceWithNoLog() and Removing getServiceWithNoLog() from LookupService

* 2016/12/21 - 3.29
    * Fixed NPE found during SQA testing (SubsetMemberships)
    * Big MapSets commit with miscellaneous cleanup and comments
    * Fixed fragile MapEntry code
    * Fixed minor Subset issue of including MapSets during a full export

* 2016/12/13 - 3.28
    * Fixed Association code to use correct Actions and NewTargetCode/OldTargetCode values.
    * Fixed a 'null dereference' issue that was flagged by Fortify.
    * Long overdue metadata cleanup / alignment between ISAAC and DB Loaders
    * Added a time-based write to the changeset writers, to ensure they are flushed to disk frequently

* 2016/12/07 - 3.27
    * Fix properties on to ensure they export and have correct action. Make sure new value is not present when the same as old value. Fix coded
        concept to ensure correct action is updated.

* 2016/12/06 - 3.26
    * Add ValueOld to VetsExporter in buildProperty and getDesignations.
    * Fix a nasty bug dealing with the metadata for VHAT Module and SOLOR Module Overlay, which lead to phantom / missing 
        modules in the database.

* 2016/12/04 - 3.25
    * Add threading to ChangesSetWriterHandler.  Add functionality to disable and enable writing. 
    * Adding a db build mode, so that indexes and changesets aren't done incrementally while building
        a database
    * Adjust some configuration on the lucene indexer to enhance performance during DB build.

* 2016/12/02 - 3.24
    * Fixing bugs with VHAT XML export (workaround for data model issue, many other fixes)

* 2016/11/30 - 3.23
    * (Jazz 418368) Fix the lucene indexer so it doesn't miss commit notifications
    * Clean up some errors that were being (erroneously) logged internally

* 2016/11/29 - 3.22
    * Move the changesets down into a changesets folder to resolve db build issues.
    * Fix some indexer config API issues that previously went undetected (but came out due to better edge case checking)

* 2016/11/28 - 3.21
    * Fixed a null pointer in association reading.

* 2016/11/28 - 3.20
    * Changes to make the concept builder more flexible with respect to creation of FSN and preferred term.
    * Integrating commit handling / notification

* 2016/11/22 - 3.19
    * Enhancements and bug fixes to VETs XML Export

* 2016/11/17 - 3.18: 
    * Enhancements to VETs XML Export
    * Added more convenience methods to Frills
    * Rename a couple of API methods to have sensible names that actually match their behavior
    * Fix a bug with the concept builder that caused extra description builders to get lost (or the default one to get lost)
    * Fix defect 392895 by restricting XML upload to XML files only.

* 2016/11/02 - 3.17: 
    * Add a scheduled thread pool executor
    * Enhancements to the VETs XMl Export

* 2016/11/01 - 3.16: 
    * See the GIT changelog for updates prior to this release.
