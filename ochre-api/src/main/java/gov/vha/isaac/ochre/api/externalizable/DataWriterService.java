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
public interface DataWriterService extends AutoCloseable {
    
    /**
     * Used when constructed via a no arg constructor (HK2 patterns) to configure the writer after the initial 
     * construct.  Implements are free to not support reconfiguration after the initial call to configure 
     * (or to not support this method at all, if the only configuration route is via the constructor)
     * @param path
     * @throws IOException
     * @throws UnsupportedOperationException - when method not supported at all, or for reconfiguration
     */
    public void configure(Path path) throws IOException, UnsupportedOperationException;

    /**
     * This does not throw an IOException, rather, they are possible, but mapped to runtime exceptions for stream convenience.
     * they still may occur, however, and should be handled.
     * @param ochreObject
     * @throws RuntimeException
     */
    public void put(OchreExternalizable ochreObject) throws RuntimeException;
    
    @Override
    public void close() throws IOException;;
    
    /**
     * flush any buffered data out to disk
     */
    public void flush() throws IOException;
    
    /**
     * flush any unwritten data, close the file writer, and block any {@link DataWriterService#put(OchreExternalizable)} calls until 
     * resume is called.  This feature is useful when you want to ensure the file on disk doesn't change while another thread picks
     * up the file and pushes it to git, for example.
     * 
     * Ensure that if pause() is called, that resume is called from the same thread.
     * @throws IOException 
     */
    public void pause() throws IOException;
    
    /**
     * open the file writer (closed by a {@link #pause()}) and unblock any blocked  {@link DataWriterService#put(OchreExternalizable)} calls.
     * Ensure that if pause() is called, that resume is called from the same thread.
     * @throws IOException 
     */
    public void resume() throws IOException;
}
