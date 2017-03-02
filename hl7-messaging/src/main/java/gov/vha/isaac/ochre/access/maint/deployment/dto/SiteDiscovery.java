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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.access.maint.deployment.dto;

import java.util.ArrayList;

/**
 * An interface for returning the necessary site data/discovery organized in
 * headers (column names) and values (rows).
 * 
 * {@link SiteDiscovery}
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
public interface SiteDiscovery
{
	/**
	 * @param Refset name for the response.
	 */
	public void setRefset(String refset);
	
	/**
	 * 
	 * @return the Refset name for the response.
	 */
	public String getRefset();
		
	/**
	 * @param headers ArrayList<String> of column headers.
	 */
	public void setHeaders(ArrayList<String> headers);
	
	/**
	 * 
	 * @return ArrayList<String> of column headers.
	 */
	public ArrayList<String> getHeaders();
	
	/**
	 * @param values ArrayList<ArrayList<String>> of rows of data values for each MFE group.
	 */
	public void setValues(ArrayList<ArrayList<String>> values);
	
	/**
	 * 
	 * @return ArrayList<ArrayList<String>> of rows of data values for each MFE group.
	 */
	public ArrayList<ArrayList<String>> getValues();
	
	
		
}
