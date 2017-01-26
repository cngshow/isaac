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
package gov.vha.isaac.ochre.deployment.listener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChannelCallback
{
    private static Logger log = LogManager.getLogger(ResponseListener.class.getPackage().getName());
    
    private SocketChannel channel;
    private StringBuffer buffer;

    public ChannelCallback( SocketChannel channel )
    {
        this.channel = channel;
        this.buffer = new StringBuffer();
    }

    public void execute() throws IOException
    {
        log.debug( this.buffer.toString() );
        writeMessage( this.channel, this.buffer.toString() );
        buffer = new StringBuffer();
    }

    public SocketChannel getChannel()
    {
        return this.channel;
    }

    public void append( String values )
    {
        buffer.append( values );
    }
    
    public void writeMessage( SocketChannel theChannel, String message ) throws IOException
    {
        ByteBuffer buf = ByteBuffer.wrap( message.getBytes()  );
        int nbytes = theChannel.write( buf );
        log.debug( "Wrote " + nbytes + " to channel." );
    }
}
