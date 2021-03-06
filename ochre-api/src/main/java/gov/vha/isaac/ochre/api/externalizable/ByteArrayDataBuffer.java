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
package gov.vha.isaac.ochre.api.externalizable;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IdentifierService;
import gov.vha.isaac.ochre.api.commit.StampService;

import javax.xml.bind.DatatypeConverter;
import java.io.UTFDataFormatException;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.StampedLock;

/**
 *
 * @author kec
 */
public class ByteArrayDataBuffer  {

    private static final int MAX_DATA_SIZE = Integer.MAX_VALUE - 16;

    private static final int DEFAULT_SIZE = 1024;

    protected static final byte FALSE = 0;
    protected static final byte TRUE = 1;
    protected int position = 0;
    protected final int positionStart;
    protected boolean readOnly = false;
    protected byte objectDataFormatVersion = 0;
    protected boolean externalData = false;
    protected IdentifierService identifierService;
    protected StampService stampService;
    /**
     * The StampedLock is to ensure the backing array does not grow underneath a
     * concurrent operation. The locks do not prevent concurrent threads from
     * reading or writing to the same fields.
     */
    protected final StampedLock sl = new StampedLock();
    protected int used = 0;
    
    private byte[] data;

    public ByteArrayDataBuffer(byte[] data, int positionStart) {
        this.data = data;
        this.used = data.length;
        this.positionStart = positionStart;
    }

    public ByteArrayDataBuffer(byte[] data) {
        this(data, 0);
    }

    public ByteArrayDataBuffer() {
        this(DEFAULT_SIZE);
    }

    public ByteArrayDataBuffer(int size) {
        this.data = new byte[size];
        this.positionStart = 0;
    }
    
    public UUID getUuid() {
        return new UUID(getLong(), getLong());
    }

    public void putUuid(UUID uuid) {
            putLong(uuid.getMostSignificantBits());
            putLong(uuid.getLeastSignificantBits());
    }

    public int getNid() {
        if (externalData) {
            return identifierService.getNidForUuids(new UUID(getLong(), getLong()));
        } 
        return getInt();
    }

    /**
     * The current capacity of the buffer. The buffer will grow if necessary, so the current capacity may not
     * reflect the maximum size that the buffer may obtain.
     * @return The currently allocated size of the buffer.
     */
    public int getCapacity() {
        return data.length;
    }
    public void putNid(int nid) {
        if (externalData) {
            Optional<UUID> optionalUuid = identifierService.getUuidPrimordialForNid(nid);
            if (optionalUuid.isPresent()) {
                UUID uuid = optionalUuid.get();
                putLong(uuid.getMostSignificantBits());
                putLong(uuid.getLeastSignificantBits());
            } else {
                throw new RuntimeException("Can't find uuid for nid: " + nid);
            }
        } else {
            putInt(nid);
        }
    }

    public int getConceptSequence() {
        if (externalData) {
            return identifierService.getConceptSequenceForUuids(new UUID(getLong(), getLong()));
        } 
        return getInt();
    }

    public void putConceptSequence(int conceptSequence) {
        if (externalData) {
            UUID uuid = identifierService.getUuidPrimordialForNid(identifierService.getConceptNid(conceptSequence)).get();
            putLong(uuid.getMostSignificantBits());
            putLong(uuid.getLeastSignificantBits());
        } else {
            putInt(conceptSequence);
        }
    }

    public int getSememeSequence() {
        if (externalData) {
            return identifierService.getSememeSequenceForUuids(new UUID(getLong(), getLong()));
        } 
        return getInt();
    }

    public void putSememeSequence(int sememeSequence) {
        if (externalData) {
            UUID uuid = identifierService.getUuidPrimordialForNid(identifierService.getSememeNid(sememeSequence)).get();
            putLong(uuid.getMostSignificantBits());
            putLong(uuid.getLeastSignificantBits());
        } else {
            putInt(sememeSequence);
        }
    }
    
    public void putStampSequence(int stampSequence) {
        if (externalData) {
            StampUniversal.get(stampSequence).writeExternal(this);
        } else {
            putInt(stampSequence);
        }
    }
    
    public int getStampSequence() {
        if (externalData) {
            return StampUniversal.get(this).getStampSequence();
        }
        return getInt();
    }
    

    public boolean isExternalData() {
        return externalData;
    }

    public void setExternalData(boolean externalData) {
        if (externalData) {
            identifierService = Get.identifierService();
            stampService = Get.stampService();
        }
        this.externalData = externalData;
    }

    public boolean getBoolean() {
        return getByte() != FALSE;
    }

    public boolean getBoolean(int position) {
        return getByte(position) != FALSE;
    }

    /**
     * The limit is the index of the first element that should not be read or written, relative to the position start.
     * It represents the end of valid data, and is never negative and is never greater than its capacity.
     * @return the position after the end of written data in the buffer, relative to the position start.
     */
    public int getLimit() {
        this.used = Math.max(this.used, this.position);
        return this.used - positionStart;
    }

    /**
     * The position start for this ByteArrayDataBuffer. Since many ByteArrayDataBuffers may
     * use the same underlying data, the position start must be honored as the origin
     * of data for this buffer, and a rewind or clear operation should only go back to position start.
     * @return the position start for this ByteArrayDataBuffer.
     */
    public int getPositionStart() {
        return positionStart;
    }

    /**
     * The index of the next element to be read or written, relative to the position start.
     * The position is never negative and is never greater than its limit.
     * @return the index of the next element to be read or written.
     */
    public int getPosition() {
        return position - positionStart;
    }

    /**
     * Set the index of the next element to be read or written, relative to the position start.
     * @param position the index of the next element to be read or written, relative to the position start.
     */
    public void setPosition(int position) {
        this.used = Math.max(this.used, this.position);
        this.position = position + this.positionStart;
    }

    public byte getByte() {
        byte result = getByte(position);
        position += 1;
        return result;
    }

    public char getChar() {
        char result = getChar(position);
        position += 2;
        return result;
    }

    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    /**
     *  Makes this buffer ready for a new sequence of put operations:
     *  It sets the limit to positionStart, and the position to positionStart.
     */
    public ByteArrayDataBuffer clear() {
        this.used = 0;
        this.position = this.positionStart;
        return this;
    }

    /**
     * Makes this buffer ready for re-reading the data that it already contains:
     * It leaves the limit unchanged and sets the position to the positionStart.
     *
     */
    public ByteArrayDataBuffer rewind() {
        this.position = positionStart;
        return this;
    }

    /**
     * Makes this buffer ready for a new sequence of get operations:
     * It sets the limit to the current position and then sets the position to zero.
     */
    public ByteArrayDataBuffer flip() {
        getLimit();
        this.position = positionStart;
        return this;
    }

    public void get(byte[] src, int offset, int length) {
        get(position, src, offset, length);
        position += length;
    }

    public int getInt() {
        int result = getInt(position);
        position += 4;
        return result;
    }

    public long getLong() {
        long result = getLong(position);
        position += 8;
        return result;
    }

    public short getShort() {
        short result = getShort(position);
        position += 2;
        return result;
    }

    public ByteArrayDataBuffer newWrapper() {
        ByteArrayDataBuffer newWrapper = new ByteArrayDataBuffer(data);
        newWrapper.readOnly = true;
        newWrapper.position = 0;
        newWrapper.used = this.used;
        return newWrapper;
    }

    public void put(byte[] src) {
        put(src, 0, src.length);
    }

    public void putBoolean(boolean x) {
        if (x) {
            putByte(TRUE);
        } else {
            putByte(FALSE);
        }
    }

    public void putByte(byte x) {
        ensureSpace(position + 1);
        long lockStamp = sl.tryOptimisticRead();
        data[position] = x;
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                data[position] = x;
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        position += 1;
    }

    public void putByteArrayField(byte[] array) {
        putInt(array.length);
        put(array, 0, array.length);
    }

    public void putDouble(double d) {
        putLong(Double.doubleToLongBits(d));
    }

    public void putFloat(float f) {
        putInt(Float.floatToRawIntBits(f));
    }

    public void putInt(int x) {
        ensureSpace(position + 4);
        long lockStamp = sl.tryOptimisticRead();
        data[position] = (byte) (x >> 24);
        data[position + 1] = (byte) (x >> 16);
        data[position + 2] = (byte) (x >> 8);
        data[position + 3] = (byte) (x);
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                data[position] = (byte) (x >> 24);
                data[position + 1] = (byte) (x >> 16);
                data[position + 2] = (byte) (x >> 8);
                data[position + 3] = (byte) (x);
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        position += 4;
    }

    public void putIntArray(int[] src) {
        putInt(src.length);
        ensureSpace(position + (src.length * 4));
        long lockStamp = sl.tryOptimisticRead();
        int startingPosition = position;
        putIntArrayIntoData(src);
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            position = startingPosition;
            try {
                putIntArrayIntoData(src);
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
    }

    private void putIntArrayIntoData(int[] src) {
        for (int anInt : src) {
            data[position] = (byte) (anInt >> 24);
            data[position + 1] = (byte) (anInt >> 16);
            data[position + 2] = (byte) (anInt >> 8);
            data[position + 3] = (byte) (anInt);
            position += 4;
        }
    }

    public void putLong(long x) {
        ensureSpace(position + 8);
        long lockStamp = sl.tryOptimisticRead();
        data[position] = (byte) (x >> 56);
        data[position + 1] = (byte) (x >> 48);
        data[position + 2] = (byte) (x >> 40);
        data[position + 3] = (byte) (x >> 32);
        data[position + 4] = (byte) (x >> 24);
        data[position + 5] = (byte) (x >> 16);
        data[position + 6] = (byte) (x >> 8);
        data[position + 7] = (byte) (x);
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                data[position] = (byte) (x >> 56);
                data[position + 1] = (byte) (x >> 48);
                data[position + 2] = (byte) (x >> 40);
                data[position + 3] = (byte) (x >> 32);
                data[position + 4] = (byte) (x >> 24);
                data[position + 5] = (byte) (x >> 16);
                data[position + 6] = (byte) (x >> 8);
                data[position + 7] = (byte) (x);
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        position += 8;
    }

    public void putShort(short x) {
        ensureSpace(position + 2);
        long lockStamp = sl.tryOptimisticRead();
        data[position] = (byte) (x >> 8);
        data[position + 1] = (byte) (x);
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                data[position] = (byte) (x >> 8);
                data[position + 1] = (byte) (x);
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        position += 2;
    }

    public void putChar(char x) {
        putShort((short) x);
    }

    public void putUTF(String str) {
        int strlen = str.length();
        int utflen = 0;
        int c;
        int count = 0;
        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 1) && (c <= 127)) {
                utflen++;
            } else if (c > 2047) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }
        byte[] bytearr = new byte[utflen];
        int i = 0;
        for (; i < strlen; i++) {
            c = str.charAt(i);
            if (!((c >= 1) && (c <= 127))) {
                break;
            }
            bytearr[count++] = (byte) c;
        }
        for (; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 1) && (c <= 127)) {
                bytearr[count++] = (byte) c;
            } else if (c > 2047) {
                bytearr[count++] = (byte) (224 | ((c >> 12) & 15));
                bytearr[count++] = (byte) (128 | ((c >> 6) & 63));
                bytearr[count++] = (byte) (128 | (c & 63));
            } else {
                bytearr[count++] = (byte) (192 | ((c >> 6) & 31));
                bytearr[count++] = (byte) (128 | (c & 63));
            }
        }
        putInt(utflen);
        put(bytearr, 0, utflen);
    }

    public final String readUTF() {
        int[] positionArray = new int[]{position};
        String result = readUTF(positionArray);
        position = positionArray[0];
        return result;
    }

    public ByteArrayDataBuffer slice() {
        ByteArrayDataBuffer slice = new ByteArrayDataBuffer(data, this.position);
        slice.readOnly = true;
        slice.position = this.position;
        slice.used = this.used;
        return slice;
    }
    
    /**
     *
     * @return the byte[] that backs this buffer.
     */
    public byte[] getData() {
        return data;
    }

    /**
     *
     * @return a byte[] written to the ByteArrayDataBuffer. Does not return the entire
    data buffer as an array.
     */
    public byte[] getByteArrayField() {
        int length = getInt();
        long lockStamp = sl.tryOptimisticRead();
        byte[] results = new byte[length];
        System.arraycopy(data, position, results, 0, length);
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                System.arraycopy(data, position, results, 0, length);
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        position += length;
        return results;
    }


    public int[] getIntArray() {
        int[] array = new int[getInt()];
        int startingPosition = position;
        long lockStamp = sl.tryOptimisticRead();
        for (int i = 0; i < array.length; i++) {
            array[i] = (((data[position]) << 24) | ((data[position + 1] & 255) << 16) | ((data[position + 2] & 255) << 8) | ((data[position + 3] & 255)));
            position += 4;
        }
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            position = startingPosition;
            try {
                for (int i = 0; i < array.length; i++) {
                    array[i] = (((data[position]) << 24) | ((data[position + 1] & 255) << 16) | ((data[position + 2] & 255) << 8) | ((data[position + 3] & 255)));
                    position += 4;
                }
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        return array;
    }

    public byte getByte(int position) {
        long lockStamp = sl.tryOptimisticRead();
        byte result = data[position];
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                result = data[position];
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        return result;
    }
    

    public short getShort(int position) {
        long lockStamp = sl.tryOptimisticRead();
        short result = (short) (((data[position] & 0xff) << 8)
                | (data[position + 1] & 0xff));
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                result = (short) (((data[position] & 0xff) << 8)
                        | (data[position + 1] & 0xff));
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        return result;
    }



    public char getChar(int position) {
        long lockStamp = sl.tryOptimisticRead();
        char result = (char) ((data[position] << 8)
                | (data[position + 1] & 0xff));
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                result = (char) ((data[position] << 8)
                        | (data[position + 1] & 0xff));
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        return result;
    }


    public int getInt(int position) {
        long lockStamp = sl.tryOptimisticRead();
        int result = (((data[position]) << 24)
                | ((data[position + 1] & 0xff) << 16)
                | ((data[position + 2] & 0xff) << 8)
                | ((data[position + 3] & 0xff)));
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                result = (((data[position]) << 24)
                        | ((data[position + 1] & 0xff) << 16)
                        | ((data[position + 2] & 0xff) << 8)
                        | ((data[position + 3] & 0xff)));
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        return result;
    }



    public float getFloat(int position) {
        return Float.intBitsToFloat(getInt(position));
    }


    public long getLong(int position) {
        long lockStamp = sl.tryOptimisticRead();
        long result = getLongResult(position);
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                result = getLongResult(position);
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        return result;
    }

    private long getLongResult(int position) {
        long result;
        result = ((((long) data[position]) << 56)
                | (((long) data[position + 1] & 0xff) << 48)
                | (((long) data[position + 2] & 0xff) << 40)
                | (((long) data[position + 3] & 0xff) << 32)
                | (((long) data[position + 4] & 0xff) << 24)
                | (((long) data[position + 5] & 0xff) << 16)
                | (((long) data[position + 6] & 0xff) << 8)
                | (((long) data[position + 7] & 0xff)));
        return result;
    }


    public double getDouble(int position) {
        return Double.longBitsToDouble(getLong(position));
    }


    public void trimToSize() {
        if (readOnly) {
            throw new ReadOnlyBufferException();
        }
        long lockStamp = sl.writeLock();
        try {
            if (position < data.length && used < data.length) {
                int newSize = Math.max(position, used);
                byte[] newData = new byte[newSize];
                System.arraycopy(data, 0, newData, 0, newSize);
                data = newData;
            }
        } finally {
            sl.unlockWrite(lockStamp);
        }
    }

    private void ensureSpace(int minSpace) {
        if (readOnly) {
            throw new ReadOnlyBufferException();
        }
        if (minSpace > data.length) {
            long lockStamp = sl.writeLock();
            try {
                while (minSpace > data.length) {
                    int newCapacity = data.length << 1;
                    if (newCapacity > MAX_DATA_SIZE) {
                        newCapacity = MAX_DATA_SIZE;
                    }
                    data = Arrays.copyOf(data, newCapacity);
                }
            } finally {
                sl.unlockWrite(lockStamp);
            }
        }
    }


    public void put(byte[] src, int offset, int length) {
        ensureSpace(position + length);
        long lockStamp = sl.tryOptimisticRead();
        System.arraycopy(src, offset, data, position, length);
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                System.arraycopy(src, offset, data, position, length);
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
        position += length;
    }


    public void get(int position, byte[] src, int offset, int length) {
        long lockStamp = sl.tryOptimisticRead();
        System.arraycopy(data, position, src, offset, length);
        if (!sl.validate(lockStamp)) {
            lockStamp = sl.readLock();
            try {
                System.arraycopy(data, position, src, offset, length);
            } finally {
                sl.unlockRead(lockStamp);
            }
        }
    }


    public final String readUTF(int[] position) {
        int utflen = getInt(position[0]);
        byte[] bytearr = new byte[utflen];
        char[] chararr = new char[utflen];

        int c, char2, char3;
        int count = 0;
        int chararr_count = 0;

        get(position[0] + 4, bytearr, 0, utflen);

        position[0] = position[0] + 4 + utflen;
        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            if (c > 127) {
                break;
            }
            count++;
            chararr[chararr_count++] = (char) c;
        }

        while (count < utflen) {
            try {
                c = (int) bytearr[count] & 0xff;
                switch (c >> 4) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        /* 0xxxxxxx*/
                        count++;
                        chararr[chararr_count++] = (char) c;
                        break;
                    case 12:
                    case 13:
                        /* 110x xxxx   10xx xxxx*/
                        count += 2;
                        if (count > utflen) {
                            throw new UTFDataFormatException(
                                    "malformed input: partial character at end");
                        }
                        char2 = (int) bytearr[count - 1];
                        if ((char2 & 0xC0) != 0x80) {
                            throw new UTFDataFormatException(
                                    "malformed input around byte " + count);
                        }
                        chararr[chararr_count++] = (char) (((c & 0x1F) << 6)
                                | (char2 & 0x3F));
                        break;
                    case 14:
                        /* 1110 xxxx  10xx xxxx  10xx xxxx */
                        count += 3;
                        if (count > utflen) {
                            throw new UTFDataFormatException(
                                    "malformed input: partial character at end");
                        }
                        char2 = (int) bytearr[count - 2];
                        char3 = (int) bytearr[count - 1];
                        if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                            throw new UTFDataFormatException(
                                    "malformed input around byte " + (count - 1));
                        }
                        chararr[chararr_count++] = (char) (((c & 0x0F) << 12)
                                | ((char2 & 0x3F) << 6)
                                | (char3 & 0x3F));
                        break;
                    default:
                        /* 10xx xxxx,  1111 xxxx */
                        throw new UTFDataFormatException(
                                "malformed input around byte " + count);
                }
            } catch (UTFDataFormatException ex) {
                throw new RuntimeException(ex);
            }
        }
        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararr_count);
    }

    public byte getObjectDataFormatVersion() {
        return objectDataFormatVersion;
    }

    public void setObjectDataFormatVersion(byte objectDataFormatVersion) {
        this.objectDataFormatVersion = objectDataFormatVersion;
    }

    public void append(ByteArrayDataBuffer db, int position, int length) {
        ensureSpace(this.position + length);
        System.arraycopy(db.data, position, data, this.position, length);
        this.position += length;
    }

    @Override
    public String toString() {
        return "ByteArrayDataBuffer{" +
                "position=" + position +
                ", positionStart=" + positionStart +
                ", readOnly=" + readOnly +
                ", objectDataFormatVersion=" + objectDataFormatVersion +
                ", externalData=" + externalData +
                ", used=" + used +
                ", data=" + DatatypeConverter.printHexBinary(data) +
                '}';
    }
}
