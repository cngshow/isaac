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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataWriterService;
import gov.vha.isaac.ochre.api.externalizable.ByteArrayDataBuffer;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;

/**
 * @author kec
 */
@Service(name="ibdfWriter")
@PerLookup
public class BinaryDataWriterProvider implements BinaryDataWriterService {

    private static final int MAX_DEBUG_COUNT = 10;
    private boolean DEBUG = false;

    private static final int BUFFER_SIZE = 1024;
    Path dataPath;
    ByteArrayDataBuffer buffer = new ByteArrayDataBuffer(BUFFER_SIZE);
    DataOutputStream output;
    int writtenObjects = 0;
    int debugCount = 0;
    OchreExternalizableObjectType lastObjectType;

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
        if (this.dataPath != null) {
            throw new RuntimeException("Reconfiguration is not supported");
        }
        this.dataPath = path;
        this.output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dataPath.toFile())));
        this.buffer.setExternalData(true);
        DEBUG = Get.configurationService().enableVerboseDebug();
	}

	@Override
    public void put(OchreExternalizable ochreObject) {
        if (ochreObject.getOchreObjectType() != lastObjectType) {
            debugCount = 0;
            lastObjectType = ochreObject.getOchreObjectType();
        }
        try {
            buffer.clear();
            ochreObject.putExternal(buffer);
            output.writeByte(ochreObject.getOchreObjectType().getToken());
            output.writeByte(ochreObject.getDataFormatVersion());
            output.writeInt(buffer.getLimit());
            output.write(buffer.getData(), 0, buffer.getLimit());
            if (DEBUG && debugCount < MAX_DEBUG_COUNT) {
                System.out.println("Writing "+ debugCount +" : " + ochreObject);
                //byte[] data = new byte[buffer.getLimit()];
                //System.arraycopy(buffer.getData(), 0, data, 0, buffer.getLimit());
                //System.out.println("Data: " + DatatypeConverter.printHexBinary(data));
            }
            writtenObjects++;
            debugCount++;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() {
        try {
            this.output.flush();
            this.output.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
