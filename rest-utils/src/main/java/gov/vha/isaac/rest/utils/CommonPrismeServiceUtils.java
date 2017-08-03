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

package gov.vha.isaac.rest.utils;

import java.net.URL;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * {@link CommonPrismeServiceUtils}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class CommonPrismeServiceUtils {
	private static Logger log = LogManager.getLogger(CommonPrismeServiceUtils.class);
	
	private CommonPrismeServiceUtils() {}

	public static String getTargetFromUrl(URL url) {
		try {
			StringBuilder target = new StringBuilder();
			target.append(url.getProtocol());
			target.append("://");
			target.append(url.getHost());
			if (url.getPort() > 0) {
				target.append(":" + url.getPort());
			}
			
			return target.toString();
		} catch (RuntimeException e) {
			log.error("FAILED getting target from URL '" + url + "'", e);
			throw e;
		}
	}
	public static String postJsonToPrisme(WebTarget targetWithPath, String json, Map<String, String> params) {
		if (params != null) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				targetWithPath = targetWithPath.queryParam(entry.getKey(), entry.getValue());
			}
		}
		Response response = targetWithPath.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(json));

		if (response.getStatus() != Status.OK.getStatusCode()) {
			throw new RuntimeException("Failed performing POST " + targetWithPath + " of \"" + json + "\" + with CODE=" + response.getStatus() + " and REASON=" + response.getStatusInfo());
		}

		String responseJson = response.readEntity(String.class);
	
		return responseJson;
	}

	public static String postJsonToPrisme(WebTarget targetWithPath, String json) {
		return postJsonToPrisme(targetWithPath, json, (Map<String, String>)null);
	}

	public static String getResultJsonFromPrisme(WebTarget targetWithPath, Map<String, String> params) {
		for (Map.Entry<String, String> entry : params.entrySet()) {
			targetWithPath = targetWithPath.queryParam(entry.getKey(), entry.getValue());
		}
		Response response = targetWithPath.request().accept(MediaType.APPLICATION_JSON).get();
	
		if (response.getStatus() != Status.OK.getStatusCode()) {
			throw new RuntimeException("Failed performing GET " + targetWithPath + " with CODE=" + response.getStatus() + " and REASON=" + response.getStatusInfo());
		}
	
		String responseJson = response.readEntity(String.class);
		log.debug("Request '{}' returned '{}'", targetWithPath.toString(), responseJson);
	
		return responseJson;
	}
}
