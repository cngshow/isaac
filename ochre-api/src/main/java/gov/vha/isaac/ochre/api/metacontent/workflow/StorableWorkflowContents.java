/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright 
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.metacontent.workflow;

/**
 * Entries for Workflow Content Store
 * 
 * {@link AvailableAction}
 * {@link UserPermission}
 * {@link HistoricalWorkflow} 
 * {@link ProcessInstance}
 * {@link DefinitionDetail}
 * {@link DomainStandard}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a> 
 */
import java.io.IOException;
import java.util.UUID;

/**
 * The Interface StorableWorkflowContents.
 */
public abstract class StorableWorkflowContents {

	/**
	 * The Enum WorkflowDomain.
	 */
	public enum WorkflowDomain {

		/** The allergies. */
		ALLERGIES("Allergies (multiple types)"),

		/** The medications. */
		MEDICATIONS("Medications"),

		/** The immunizations. */
		IMMUNIZATIONS("Immunizations"),

		/** The problem lists. */
		PROBLEM_LISTS("Problem Lists"),

		/** The vital signs. */
		VITAL_SIGNS("Vital Signs"),

		/** The documents. */
		DOCUMENTS("Documents (many types)"),

		/** The lab chemistry hematology. */
		LAB_CHEMISTRY_HEMATOLOGY("Results - Lab Chemistry & Hematology"),

		/** The lab anatomic pathology. */
		LAB_ANATOMIC_PATHOLOGY("Results - Lab Anatomic Pathology"),

		/** The lab microbiology. */
		LAB_MICROBIOLOGY("Results - Lab Microbiology"),

		/** The radiology reports. */
		RADIOLOGY_REPORTS("Results - Radiology Reports"),

		/** The appointments. */
		APPOINTMENTS("Encounter Data – Appointments"),

		/** The admissions. */
		ADMISSIONS("Encounter Data – Admissions"),

		/** The procedures. */
		PROCEDURES("Procedures"),

		/** The demographics. */
		DEMOGRAPHICS("Demographics"),

		/** The social history. */
		SOCIAL_HISTORY("Social History"),

		/** The family history. */
		FAMILY_HISTORY("Family History"),

		/** The paper records. */
		PAPER_RECORDS("Scanned & Imported Paper Records & Non-Radiology Images"),

		/** The pending orders. */
		PENDING_ORDERS("Plan of Care- Pending Orders (multiple types)"),

		/** The radiology images. */
		RADIOLOGY_IMAGES("Radiology Images"),

		/** The payers. */
		PAYERS("Payers"),

		/** The functional status. */
		FUNCTIONAL_STATUS("Functional Status"),

		/** The providers. */
		PROVIDERS("Providers"),

		/** The advance directives. */
		ADVANCE_DIRECTIVES("Advance Directives (metadata only)"),

		/** The medical equipment. */
		MEDICAL_EQUIPMENT("Medical Equipment"),

		/** The questionnaires. */
		QUESTIONNAIRES("Questionnaires (general & standard instruments)");

		/** The text. */
		private final String text;

		/**
		 * Instantiates a new domain.
		 *
		 * @param text
		 *            the text
		 */
		private WorkflowDomain(final String text) {
			this.text = text;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return text;
		}
	};

	/**
	 * The Enum DataElement.
	 */
	public enum WorkflowDataElement {

		/** The drug. */
		DRUG("Drug Allergen"),

		/** The drug class. */
		DRUG_CLASS("Drug Class Allergen"),

		/** The food. */
		FOOD("Food Allergen"),

		/** The ingredient. */
		INGREDIENT("Ingredient"),

		/** The medication. */
		MEDICATION("Medication"),

		/** The vaccine. */
		VACCINE("Vaccine Administered"),

		/** The problem. */
		PROBLEM("Problem"),

		/** The vitals. */
		VITALS("Vitals Type"),

		/** The document type. */
		DOCUMENT_TYPE("Document Type"),

		/** The result. */
		RESULT("Result Type"),

		/** The encounter. */
		ENCOUNTER("Encounter Type"),

		/** The encounter diagnosis. */
		ENCOUNTER_DIAGNOSIS("Encounter Diagnosis (Dx)"),

		/** The procedure outpatient clinician. */
		PROCEDURE_OUTPATIENT_CLINICIAN("Procedure Type-Outpatient/Clinician"),

		/** The procedure hospital. */
		PROCEDURE_HOSPITAL("Procedure Type-Hospital"),

		/** The ethnicity race. */
		ETHNICITY_RACE("Ethnicity & Race"),

		/** The language. */
		LANGUAGE("Preferred Language"),

		/** The social history. */
		SOCIAL_HISTORY("Social History Entry"),

		/** The family history. */
		FAMILY_HISTORY("Family History Entry"),

		/** The med order. */
		MED_ORDER("Med Order Item"),

		/** The lab order. */
		LAB_ORDER("Lab Order Item"),

		/** The rad order. */
		RAD_ORDER("Rad Order Item"),

		/** The consult. */
		CONSULT("Consult Item"),

		/** The image format. */
		IMAGE_FORMAT("Image Format"),

		/** The insurance. */
		INSURANCE("Insurance Type"),

		/** The functional status. */
		FUNCTIONAL_STATUS("Functional Status Entry"),

		/** The provider. */
		PROVIDER("Provider Types"),

		/** The directive. */
		DIRECTIVE("Advanced Directives Type"),

		/** The medical equipment. */
		MEDICAL_EQUIPMENT("Medical Equipment Type"),

		/** The document. */
		DOCUMENT("Document");

		/** The text. */
		private final String text;

		/**
		 * Instantiates a new data element.
		 *
		 * @param text
		 *            the text
		 */
		private WorkflowDataElement(final String text) {
			this.text = text;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return text;
		}
	};

	/**
	 * The Enum WorkflowTerminology.
	 */
	public enum WorkflowTerminology {

		/** The asc. */
		ASC("ASC X12N (Health Insurance Type)"),

		/** The CP t4. */
		CPT4("CPT4"),

		/** The CP t4_ hcpcs. */
		CPT4_HCPCS("CPT4/HCPCS"),

		/** The cvx. */
		CVX("CVX"),

		/** The dicom. */
		DICOM("DICOM (a format, not a terminology)"),

		/** The IC d_10_ pcs. */
		ICD_10_PCS("ICD-10 PCS"),

		/** The IC d_9_ cm. */
		ICD_9_CM("ICD-9 CM"),

		/** The IS o_639. */
		ISO_639("ISO 639-2 alpha-3 codes"),

		/** The loinc. */
		LOINC("LOINC"),

		/** The loinc doc. */
		LOINC_DOC("LOINC (Document Types)"),

		/** The loinc vital. */
		LOINC_VITAL("LOINC (Vitals Subset)"),

		/** The ndf rt. */
		NDF_RT("NDF RT"),

		/** The none. */
		NONE("None"),

		/** The nucc. */
		NUCC("NUCC Taxonomy"),

		/** The omb cdc. */
		OMB_CDC("OMB/CDC Race codes"),

		/** The rxnorm. */
		RXNORM("RxNorm (Med Orders)"),

		/** The snomed. */
		SNOMED("SNOMED CT"),

		/** The umls. */
		UMLS("UMLS"),

		/** The umls snomed. */
		UMLS_SNOMED("UMLS-SNOMED CT"),

		/** The unii. */
		UNII("UNII"),

		/** The unstructered loinc document. */
		UNSTRUCTERED_LOINC_DOCUMENT("Unstructured Document with LOINC Document Type");

		/** The text. */
		private final String text;

		/**
		 * Instantiates a new terminology.
		 *
		 * @param text
		 *            the text
		 */
		private WorkflowTerminology(final String text) {
			this.text = text;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return text;
		}
	};

	/**
	 * The Enum SubjectMatter.
	 */
	public enum SubjectMatter {

		/** The mapping. */
		MAPPING,
		/** The concept. */
		CONCEPT
	};

	/** The id. */
	protected UUID id;

	/**
	 * Turn the concrete class into a suitable byte[] for storage.
	 *
	 * @return the byte[]
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public abstract byte[] serialize() throws IOException;

	/**
	 * Sets the id.
	 *
	 * @param key
	 *            the new id
	 */
	public void setId(UUID key) {
		id = key;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public UUID getId() {
		return id;
	}
}
