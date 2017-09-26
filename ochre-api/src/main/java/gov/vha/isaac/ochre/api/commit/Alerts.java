package gov.vha.isaac.ochre.api.commit;

public class Alerts
{
	private static class AlertImpl implements Alert
	{
		private final AlertType type;
		private final String text;
		private final int componentNid;

		private AlertImpl(AlertType type, String text, int componentNid) {
			this.type = type;
			this.text = text;
			this.componentNid = componentNid;
		}

		@Override
		public Object[] getFixups()
		{
			return null;
		}

		@Override
		public int getComponentNidForAlert()
		{
			return componentNid;
		}

		@Override
		public AlertType getAlertType()
		{
			return type;
		}

		@Override
		public String getAlertText()
		{
			return text;
		}
		public String toString()
		{
			return type.name() + " Alert: " + getAlertText() + " on nid " + getComponentNidForAlert();
		}
	};
	
	public static Alert error(String text, int componentNid)
	{
		return new AlertImpl(AlertType.ERROR, text, componentNid);
	}
	public static Alert warning(String text, int componentNid)
	{
		return new AlertImpl(AlertType.WARNING, text, componentNid);
	}
}
