/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.vha.isaac.ochre.mojo;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IsaacTaxonomy;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.constants.MetadataConceptConstant;
import gov.vha.isaac.ochre.api.constants.ModuleProvidedConstants;

/**
 *
 * @author kec
 */
@Mojo( name = "export-taxonomy")
public class ExportTaxonomy extends AbstractMojo {

    @Parameter(required = true)
    private String bindingPackage;
    
    @Parameter(required = true)
    private String bindingClass;
    
    @Parameter(required = true, defaultValue = "${project.build.directory}") 
    File buildDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Get.configurationService().setBootstrapMode();
            Get.configurationService().setDBBuildMode();
            IsaacTaxonomy taxonomy = LookupService.get().getService(IsaacTaxonomy.class); 
            File javaDir = new File(buildDirectory, "src/generated");
            javaDir.mkdirs();
            File metadataDirectory = new File(buildDirectory, "generated-resources");
            metadataDirectory.mkdirs();
            File metadataXmlDataFile = new File(metadataDirectory, taxonomy.getClass().getSimpleName() + ".xml");
            String bindingFileDirectory = bindingPackage.concat(".").concat(bindingClass).replace('.', '/');
            //Write out the java binding file before we read in the MetadataConceptConstant objects, as these already come from classes
            //and I don't want to have duplicate constants in the system
            File bindingFile = new File(javaDir, bindingFileDirectory + ".java");
            bindingFile.getParentFile().mkdirs();
            
            try ( Writer javaWriter = new BufferedWriter(new FileWriter(bindingFile));)
               {
                   taxonomy.exportJavaBinding(javaWriter, bindingPackage,  bindingClass);
               }
            
            ArrayList<MetadataConceptConstant> constantsForYamlOnly = new ArrayList<>();
            
            //Read in the MetadataConceptConstant constant objects
            for (ModuleProvidedConstants mpc : LookupService.get().getAllServices(ModuleProvidedConstants.class))
            {
                getLog().info("Adding metadata constants from " + mpc.getClass().getName());
                int count = 0;
                for (MetadataConceptConstant mc : mpc.getConstantsToCreate())
                {
                    taxonomy.createConcept(mc);
                    count++;
                }
                getLog().info("Created " + count + " concepts (+ their children)");
                if (mpc.getConstantsForInfoOnly() != null)
                {
                    for (MetadataConceptConstant mc : mpc.getConstantsForInfoOnly())
                    {
                        constantsForYamlOnly.add(mc);
                    }
                    if (mpc.getConstantsForInfoOnly().length > 0)
                    {
                        getLog().info("Added " + mpc.getConstantsForInfoOnly().length + " constants to the YAML file for info only");
                    }
                }
            }
            
            //Now write out the other files, so they have all of the constants.
            try ( DataOutputStream xmlData = new DataOutputStream(
                            new BufferedOutputStream(new FileOutputStream(metadataXmlDataFile)));
                    FileWriter yamlFile = new FileWriter(new File(metadataDirectory.getAbsolutePath(), 
                       taxonomy.getClass().getSimpleName() + ".yaml"));)
               {
                   
                   taxonomy.exportYamlBinding(yamlFile, bindingPackage, bindingClass, constantsForYamlOnly);
                   taxonomy.exportJaxb(xmlData);
               }
            Path ibdfPath = Paths.get(metadataDirectory.getAbsolutePath(), taxonomy.getClass().getSimpleName() + ".ibdf");
            Path jsonPath = Paths.get(metadataDirectory.getAbsolutePath(), taxonomy.getClass().getSimpleName() + ".json");
            taxonomy.export(Optional.of(jsonPath), Optional.of(ibdfPath));
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
        }
    }
}
