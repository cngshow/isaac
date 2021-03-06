package gov.vha.isaac.ochre.mapping.data;

import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.util.StringUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;

public class MappingObject extends StampedItem {
	
	protected UUID editorStatusConcept = null;
	protected int editorStatusConceptNid = 0;
	protected final SimpleStringProperty editorStatusConceptProperty = new SimpleStringProperty();
	protected HashMap<UUID, String> cachedValues = new HashMap<>();
	
	/**
	 * @return the editorStatusConcept
	 */
	public UUID getEditorStatusConcept()
	{
		return editorStatusConcept;
	}

	/**
	 * @param editorStatusConcept the editorStatusConcept to set
	 */
	public void setEditorStatusConcept(UUID editorStatusConcept)
	{
		this.editorStatusConcept = editorStatusConcept;
		this.editorStatusConceptNid = getNidForUuidSafe(editorStatusConcept);
		propertyLookup(editorStatusConcept, editorStatusConceptProperty);
	}

	public int getEditorStatusConceptNid() {
		return editorStatusConceptNid;
	}

	public SimpleStringProperty getEditorStatusConceptProperty()
	{
		return editorStatusConceptProperty;
	}

	public String getEditorStatusName() {
		return editorStatusConceptProperty.get();
	}
	
	protected void propertyLookup(UUID uuid, SimpleStringProperty property)	{
		if (uuid == null) {
			property.set(null);
		} else {
			String cachedValue = cachedValues.get(uuid);
			if (cachedValue != null) {
				property.set(cachedValue);
			} else {
				property.set("-");
				Get.workExecutors().getExecutor().execute(() -> {
					String s =  Get.conceptDescriptionText(Get.identifierService().getConceptSequenceForUuids(uuid));
					cachedValues.put(uuid, s);
					Platform.runLater(() -> {
						property.set(s);
					});
				});
			}
		}
	}
	
	public static int getNidForUuidSafe(UUID uuid) {
		return (uuid == null)? 0 : Get.identifierService().getNidForUuids(uuid);
	}

	public static final Comparator<MappingObject> editorStatusComparator = new Comparator<MappingObject>() {
		@Override
		public int compare(MappingObject o1, MappingObject o2) {
			return StringUtils.compareStringsIgnoreCase(o1.getEditorStatusName(), o2.getEditorStatusName());
		}
	};
}
