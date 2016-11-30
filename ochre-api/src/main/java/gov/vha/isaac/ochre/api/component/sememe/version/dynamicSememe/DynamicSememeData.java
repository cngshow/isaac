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
package gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe;

import java.io.IOException;
import java.util.function.Supplier;
import javafx.beans.property.ReadOnlyObjectProperty;

/**
 * {@link DynamicSememeData}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public interface DynamicSememeData {
    /**
     * @return The data object itself, in its most compact, serialized form. You
     *         probably don't want this method unless you are doing something clever.... 
     *         For a getData() method that doesn't require deserialization, see the {@link #getDataObject()} method. 
     *         For a method that doesn't require casting the output, see the getDataXXX() method available within 
     *         implementations of the {@link DynamicSememeData} interface.
     */
    public byte[] getData();

    /**
     * @return The data object itself. 
     *         For a getData() method that doesn't  require casting of the output, see the getDataXXX() method
     *         available within implementations of the {@link DynamicSememeData} interface.
     */
    public Object getDataObject();
    
    /**
     * @return The data object itself. 
     *         For a getDataProperty() method that doesn't  require casting of the output, see the getDataXXXProperty() methods
     *         available within implementations of the {@link DynamicSememeData} interface.
     * @throws ContradictionException 
     * @throws IOException 
     */
    public ReadOnlyObjectProperty<?> getDataObjectProperty();

    /**
     * @return The type information of the data
     */
    public DynamicSememeDataType getDynamicSememeDataType();
    
    /**
     * Return a string representation of the data fields
     * @return
     */
    public String dataToString();
    
    /**
     * In some cases, data objects are created without the necessary data to calculate their names.
     * If necessary, the missing information can be set via this method, so that the toString and getDataXXXProperty() methods
     * can have an appropriate name in them.  This method does nothing, if it already had the information necessary to calculate
     * 
     * @param nameProvider
     */
    public void configureNameProvider(int assemblageSequence, int columnNumber);
}
