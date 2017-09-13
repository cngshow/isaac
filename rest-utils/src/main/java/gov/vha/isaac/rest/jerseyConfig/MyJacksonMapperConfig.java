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
package gov.vha.isaac.rest.jerseyConfig;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * 
 * {@link MyJacksonMapperConfig}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@Provider
public class MyJacksonMapperConfig implements ContextResolver<ObjectMapper>
{
	final ObjectMapper defaultObjectMapper;

	public MyJacksonMapperConfig()
	{
		defaultObjectMapper = createDefaultMapper();
	}

	@Override
	public ObjectMapper getContext(final Class<?> type)
	{
		return defaultObjectMapper;
	}

	private static ObjectMapper createDefaultMapper()
	{
		final ObjectMapper result = new ObjectMapper();
		result.enable(SerializationFeature.INDENT_OUTPUT);
		result.setSerializationInclusion(Include.NON_NULL);
		result.setAnnotationIntrospector(createJaxbJacksonAnnotationIntrospector());

		return result;
	}

	private static AnnotationIntrospector createJaxbJacksonAnnotationIntrospector()
	{

		final AnnotationIntrospector jaxbIntrospector = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
		final AnnotationIntrospector jacksonIntrospector = new JacksonAnnotationIntrospector();

		return AnnotationIntrospector.pair(jacksonIntrospector, jaxbIntrospector);
	}
}
