ISAAC Changelog 

This changelog summarizes changes and fixes which are a part of each revision.  For more details on the fixes, refer tracking numbers 
where provided, and the git commit history.

* 2016/12/?? - 3.30 - PENDING
    * Adding MAPPING_QUALIFIER_UNMAPPED as child of MAPPING_QUALIFIERS

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
    * Ensure system shuts down cleanly preventing database locks from corrupting database
	* Add mechanism to review database to ensure isn't corrupt

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
