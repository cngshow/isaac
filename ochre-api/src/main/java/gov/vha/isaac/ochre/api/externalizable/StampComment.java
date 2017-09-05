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

/**
 *
 * @author kec
 */
public class StampComment implements OchreExternalizable {
    
    private String comment;
    private int stampSequence;

    public StampComment(String comment, int stampSequence) {
        this.comment = comment;
        this.stampSequence = stampSequence;
    }

    public StampComment(ByteArrayDataBuffer in) {
        byte version = in.getByte();
        if (version == getDataFormatVersion()) {
            stampSequence = StampUniversal.get(in).getStampSequence();
            comment = in.readUTF();
        } else {
            throw new UnsupportedOperationException("Can't handle version: " + version);
        }
    }

    @Override
    public void putExternal(ByteArrayDataBuffer out) {
        out.putByte(getDataFormatVersion());
        StampUniversal.get(stampSequence).writeExternal(out);
        out.putUTF(comment);
    }

    @Override
    public byte getDataFormatVersion() {
        return 0;
    }

    @Override
    public OchreExternalizableObjectType getOchreObjectType() {
        return OchreExternalizableObjectType.STAMP_COMMENT;
    }
    
    public String getComment() {
        return comment;
    }

    public int getStampSequence() {
        return stampSequence;
    }

    @Override
    public String toString() {
        return "StampComment{" +
                "comment='" + comment + '\'' +
                ", stampSequence=" + stampSequence +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final StampComment other = (StampComment) obj;
        return this.getComment().equals(other.getComment()) && this.getStampSequence() == other.getStampSequence();
    }
}
