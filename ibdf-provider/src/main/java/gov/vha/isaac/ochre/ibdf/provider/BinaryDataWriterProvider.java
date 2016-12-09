/*
 * Copyright 2015 kec.
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
package gov.vha.isaac.ochre.ibdf.provider;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.ochre.api.externalizable.ByteArrayDataBuffer;
import gov.vha.isaac.ochre.api.externalizable.DataWriterService;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.util.TimeFlushBufferedOutputStream;

/**
 * @author kec
 */
@Service(name="ibdfWriter")
@PerLookup
public class BinaryDataWriterProvider implements DataWriterService {

    private static final int BUFFER_SIZE = 1024;
    private Logger logger = LoggerFactory.getLogger(BinaryDataWriterProvider.class);
    private Semaphore pauseBlock = new Semaphore(1);
    
    Path dataPath;
    ByteArrayDataBuffer buffer = new ByteArrayDataBuffer(BUFFER_SIZE);
    DataOutputStream output;

    private BinaryDataWriterProvider() throws IOException {
        //for HK2
    }
    
    /**
     * For non-HK2 use cases
     * @param dataPath
     * @throws IOException
     */
    public BinaryDataWriterProvider(Path dataPath) throws IOException {
        this();
        configure(dataPath);
    }

    @Override
    public void configure(Path path) throws IOException {
        if (this.output != null) {
            throw new RuntimeException("Reconfiguration is not supported");
        }
        dataPath = path;
        output = new DataOutputStream(new TimeFlushBufferedOutputStream(new FileOutputStream(dataPath.toFile())));
        buffer.setExternalData(true);
        logger.info("ibdf changeset writer has been configured to write to " + dataPath.toAbsolutePath().toString());
    }

    @Override
    public void put(OchreExternalizable ochreObject) throws RuntimeException 
    {
        try 
        {
            pauseBlock.acquireUninterruptibly();
            buffer.clear();
            ochreObject.putExternal(buffer);
            output.writeByte(ochreObject.getOchreObjectType().getToken());
            output.writeByte(ochreObject.getDataFormatVersion());
            output.writeInt(buffer.getLimit());
            output.write(buffer.getData(), 0, buffer.getLimit());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            pauseBlock.release();
        }
    }

    @Override
    public void close() throws IOException {
        try 
        {
            output.flush();
            output.close();
        } 
        finally
        {
            output =  null;
        }
    }

    /**
     * @throws IOException 
     * @see gov.vha.isaac.ochre.api.externalizable.DataWriterService#flush()
     */
    @Override
    public void flush() throws IOException
    {
        if (output != null)
        {
            output.flush();
        }
    }

    @Override
    public void pause() throws IOException
    {
        if (output == null)
        {
            logger.warn("already paused!");
            return;
        }
        pauseBlock.acquireUninterruptibly();
        close();
        logger.debug("ibdf writer paused");
    }

    @Override
    public void resume() throws IOException
    {
        if (output != null)
        {
            logger.warn("asked to resume but not paused!");
            return;
        }
        configure(dataPath);
        pauseBlock.release();
        logger.debug("ibdf writer resumed");
    }
}
