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
public class SiteDiscoveryDTO implements SiteDiscovery
{
	private String refset;
	private ArrayList<String> headers;
	private ArrayList<ArrayList<String>> values;

	public SiteDiscoveryDTO(String subset, ArrayList<String> headers, ArrayList<ArrayList<String>> values) {
		this.refset = refset;
		this.headers = headers;
		this.values = values;
	}
	
	public SiteDiscoveryDTO() {
	}
	
	@Override
	public void setRefset(String refset) {
		this.refset = refset;
	}
	
	@Override
	public String getRefset() {
		return refset;
	}
		
	@Override
	public void setHeaders(ArrayList<String> headers) {
		this.headers = headers;
	}
	
	@Override
	public ArrayList<String> getHeaders() {
		return headers;
	}
	
	@Override
	public void setValues(ArrayList<ArrayList<String>> values) {
		this.values = values;
	}
	
	@Override
	public ArrayList<ArrayList<String>> getValues() {
		return values;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("refset: ").append(refset).append(";\n");
		sb.append("headers: ").append(String.join(", ", headers)).append(";\n");
		
		for(ArrayList<String> group : values) {
			sb.append("values: ").append(String.join(", ", group)).append(";\n");
		}
		
		return sb.toString();
	}
}
