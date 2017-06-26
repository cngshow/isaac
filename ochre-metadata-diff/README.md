Isaac Metadata Auxiliary IBDF Differ
========================================

### Set up Eclipse Run Configuration

### Base Directory
${project_loc:ochre-metadata-diff}

#### Goals
install
	
#### Profiles
diff-ibdf
	
#### User settings
Use your a already defined settings. 

#### JRE Settings
Suggested JRE settings.  These may need to change over time.
-Xmx7g -Xms1g -XX:+UseG1GC -XX:MetaspaceSize=100M
	

#### Parameters
##### Required
| Parameter Name | Value | Example |
| -------------- | ----- | ------- |
| isaac.metadata.old | File path to **old** IsaacMetadataAuxiliary.ibdf. | C:\\Issac\idbf-diff\old\IsaacMetadataAuxiliary.ibdf |
| isaac.metadata.new | File path to **new** IsaacMetadataAuxiliary.ibdf. | C:\\Issac\idbf-diff\new\IsaacMetadataAuxiliary.ibdf |
| delta.ibdf | Output file directory and file name. |  C:\\Issac\idbf-diff\IsaacMetadataAuxiliary-1.0.0-to-1.1.0.ibdf |
| import.date | Date of file generation. | 2017-06-10 |

##### Optional
| Parameter Name | Value |
| -------------- | ----- |
| diff.on.status | true or false, default to true |
| diff.on.author | true or false, default to true |
| diff.on.module | true or false, default to true |
| diff.on.path | true or false, default to true |


#### Naming idbf file.
IsaacMetadataAuxiliary-<**old** version number>-to-<**new** version number>.ibdf
Example: IsaacMetadataAuxiliary-1.0.0-to-1.1.0.ibdf