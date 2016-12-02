package gov.vha.isaac.ochre.utility.export;

import static gov.vha.isaac.ochre.api.constants.Constants.DATA_STORE_ROOT_LOCATION_PROPERTY;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.util.DBLocator;

public class TestVetsExporter
{

	private static Logger log = LogManager.getLogger();

	public static void main(String[] args)
	{
		new TestVetsExporter();
		issacInit();
		VetsExporter ve = new VetsExporter();
		ve.export(System.out, 1451628000000l, System.currentTimeMillis(), false);
		isaacStop();
		javafx.application.Platform.exit();
	}

	private static void issacInit()
	{
		log.info("Isaac Init called");

		try
		{
			log.info("ISAAC Init thread begins");

			if (StringUtils.isBlank(System.getProperty(DATA_STORE_ROOT_LOCATION_PROPERTY)))
			{
				//if there isn't an official system property set, check this one.
				String sysProp = System.getProperty("isaacDatabaseLocation");
				//File temp = new File(sysProp);

				File dataStoreLocation = DBLocator.findDBFolder(new File(""));//temp

				if (!dataStoreLocation.exists())
				{
					throw new RuntimeException("Couldn't find a data store from the input of '" + dataStoreLocation.getAbsoluteFile().getAbsolutePath() + "'");
				}
				if (!dataStoreLocation.isDirectory())
				{
					throw new RuntimeException("The specified data store: '" + dataStoreLocation.getAbsolutePath() + "' is not a folder");
				}

				//use the passed in JVM parameter location
				LookupService.getService(ConfigurationService.class).setDataStoreFolderPath(dataStoreLocation.toPath());
				System.out.println("  Setup AppContext, data store location = " + dataStoreLocation.getAbsolutePath());
			}

			//status_.set("Starting ISAAC");
			LookupService.startupIsaac();

			//status_.set("Ready");
			System.out.println("Done setting up ISAAC");

		}
		catch (Exception e)
		{
			log.error("Failure starting ISAAC", e);
		}
	}

	private static void isaacStop()
	{
		log.info("Stopping ISAAC");
		LookupService.shutdownIsaac();
		log.info("ISAAC stopped");
	}
}