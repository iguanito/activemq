/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.transport.amqp;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.activemq.util.ByteArrayOutputStream;
import org.apache.activemq.util.ByteSequence;
import org.apache.activemq.wireformat.WireFormat;
import org.fusesource.hawtbuf.Buffer;

public class AmqpWireFormat implements WireFormat {

    public static final long DEFAULT_MAX_FRAME_SIZE = Long.MAX_VALUE;
    public static final int NO_AMQP_MAX_FRAME_SIZE = -1;

    private int version = 1;
    private long maxFrameSize = DEFAULT_MAX_FRAME_SIZE;
    private int maxAmqpFrameSize = NO_AMQP_MAX_FRAME_SIZE;
    private String anonymousNodeName = "$relay";
    private boolean useByteDestinationTypeAnnotation = false;

    @Override
    public ByteSequence marshal(Object command) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        marshal(command, dos);
        dos.close();
        return baos.toByteSequence();
    }

    @Override
    public Object unmarshal(ByteSequence packet) throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(packet);
        DataInputStream dis = new DataInputStream(stream);
        return unmarshal(dis);
    }

    @Override
    public void marshal(Object command, DataOutput dataOut) throws IOException {
        if (command instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer) command;

            if (dataOut instanceof OutputStream) {
                WritableByteChannel channel = Channels.newChannel((OutputStream) dataOut);
                channel.write(buffer);
            } else {
                while (buffer.hasRemaining()) {
                    dataOut.writeByte(buffer.get());
                }
            }
        } else {
            Buffer frame = (Buffer) command;
            frame.writeTo(dataOut);
        }
    }

    boolean magicRead = false;

    @Override
    public Object unmarshal(DataInput dataIn) throws IOException {
        if (!magicRead) {
            Buffer magic = new Buffer(8);
            magic.readFrom(dataIn);
            magicRead = true;
            return new AmqpHeader(magic);
        } else {
            int size = dataIn.readInt();
            if (size > maxFrameSize) {
                throw new AmqpProtocolException("Frame size exceeded max frame length.");
            }
            Buffer frame = new Buffer(size);
            frame.bigEndianEditor().writeInt(size);
            frame.readFrom(dataIn);
            frame.clear();
            return frame;
        }
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * @return the version of the wire format
     */
    @Override
    public int getVersion() {
        return this.version;
    }

    public long getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setMaxFrameSize(long maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public int getMaxAmqpFrameSize() {
        return maxAmqpFrameSize;
    }

    public void setMaxAmqpFrameSize(int maxAmqpFrameSize) {
        this.maxAmqpFrameSize = maxAmqpFrameSize;
    }

    public String getAnonymousNodeName() {
        return anonymousNodeName;
    }

    public void setAnonymousNodeName(String anonymousNodeName) {
        this.anonymousNodeName = anonymousNodeName;
    }

    public boolean isUseByteDestinationTypeAnnotation() {
        return useByteDestinationTypeAnnotation;
    }

    public void setUseByteDestinationTypeAnnotation(boolean useByteDestinationTypeAnnotation) {
        this.useByteDestinationTypeAnnotation = useByteDestinationTypeAnnotation;
    }
}
