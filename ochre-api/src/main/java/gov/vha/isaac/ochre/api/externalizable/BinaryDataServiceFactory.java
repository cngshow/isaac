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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import org.jvnet.hk2.annotations.Contract;

/**
 *
 * @author kec
 */
@Contract
public interface BinaryDataServiceFactory {
    /**
     * 
     * @param dataPath data file location
     * @return the BinaryDataReaderService for the given dataPath
     * @throws java.io.FileNotFoundException
     */
    BinaryDataReaderService getReader(Path dataPath) throws FileNotFoundException;
    
    /**
     * 
     * @param dataPath data file location
     * @return the BinaryDataReaderService for the given dataPath
     * @throws java.io.FileNotFoundException
     */
    BinaryDataReaderQueueService getQueueReader(Path dataPath) throws FileNotFoundException;
    
    /**
     * 
     * @param dataPath data file location
     * @return the BinaryDataWriterService for the given dataPath
     * @throws java.io.FileNotFoundException
     */
    BinaryDataWriterService getWriter(Path dataPath) throws IOException;
}
