/*
 * Copyright 2015 U.S. Department of Veterans Affairs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.externalizable;

import java.io.IOException;
import java.nio.file.Path;
import org.jvnet.hk2.annotations.Contract;

/**
 *
 * @author kec
 */
@Contract
public interface BinaryDataWriterService extends AutoCloseable {
    
    /**
     * Used when constructed via a no arg constructor (HK2 patterns) to configure the writer after the initial 
     * construct.  Implements are free to not support reconfiguration after the initial call to configure 
     * (or to not support this method at all, if the only configuration route is via the constructor)
     * @param path
     * @throws IOException
     * @throws UnsupportedOperationException - when method not supported at all, or for reconfiguration
     */
    public void configure(Path path) throws IOException, UnsupportedOperationException;

    public void put(OchreExternalizable ochreObject);
    
    @Override
    public void close();
}
