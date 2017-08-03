package gov.vha.isaac.ochre.api.commit;

public class Alerts
{
	public static Alert error(String text, int componentNid)
	{
		return new Alert()
		{

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
				return AlertType.ERROR;
			}

			@Override
			public String getAlertText()
			{
				return text;
			}
			public String toString()
			{
				return "ERROR Alert: " + getAlertText() + " on nid " + getComponentNidForAlert();
			}
		};
	}
}
