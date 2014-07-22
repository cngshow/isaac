package org.ihtsdo.otf.tcc.model.cc.component;

import org.ihtsdo.otf.tcc.api.refex.RefexType;
import org.ihtsdo.otf.tcc.model.cc.refex.RefexMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_array_of_bytearray.ArrayOfByteArrayMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_array_of_bytearray.ArrayOfByteArraySerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_boolean.BooleanMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_boolean.BooleanSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_int.IntMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_int.IntSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_long.LongMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_long.LongSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_membership.MembershipMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_membership.MembershipSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid.NidMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid.NidSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_boolean.NidBooleanMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_boolean.NidBooleanSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_float.NidFloatMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_float.NidFloatSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_int.NidIntMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_int.NidIntSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_long.NidLongMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_long.NidLongSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid.NidNidMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid.NidNidSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid.NidNidNidMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid.NidNidNidSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid_float.NidNidNidFloatMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid_float.NidNidNidFloatSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid_int.NidNidNidIntMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid_int.NidNidNidIntSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid_long.NidNidNidLongMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid_long.NidNidNidLongSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid_string.NidNidNidStringMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_nid_string.NidNidNidStringSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_string.NidNidStringMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_nid_string.NidNidStringSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_string.NidStringMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_nid_string.NidStringSerializer;
import org.ihtsdo.otf.tcc.model.cc.refex.type_string.StringMember;
import org.ihtsdo.otf.tcc.model.cc.refex.type_string.StringSerializer;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.RefexDynamicMember;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.RefexDynamicSerializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by kec on 7/13/14.
 */
public class ConceptComponentSerializer {

    public static void serialize(DataOutput output, ConceptComponent cc) throws IOException {
        assert cc.nid != 0;
        assert cc.primordialStamp != 0 && cc.primordialStamp != Integer.MAX_VALUE : "Processing nid: " + cc.nid;
        output.writeInt(cc.nid);
        output.writeInt(cc.enclosingConceptNid);
        output.writeLong(cc.primordialMsb);
        output.writeLong(cc.primordialLsb);
        output.writeInt(cc.primordialStamp);

        // Additional UUIDs

        if (cc.additionalUuidParts != null) {
            output.writeShort(cc.additionalUuidParts.length);
            for (long uuidPart : cc.additionalUuidParts) {
                output.writeLong(uuidPart);
            }
        } else {
            output.writeShort(0);
        }

        // static refexes
        if (cc.annotations != null) {
            output.writeInt(cc.annotations.size());
            for (Object obj : cc.annotations) {
                RefexMember rx = (RefexMember) obj;
                rx.getRefexType().writeType(output);
                switch (rx.getRefexType()) {
                    case ARRAY_BYTEARRAY:
                        ArrayOfByteArraySerializer.get().serialize(output, (ArrayOfByteArrayMember) rx);
                        break;
                    case BOOLEAN:
                        BooleanSerializer.get().serialize(output, (BooleanMember) rx);
                        break;
                    case CID:
                        NidSerializer.get().serialize(output, (NidMember) rx);
                        break;
                    case CID_BOOLEAN:
                        NidBooleanSerializer.get().serialize(output, (NidBooleanMember) rx);
                        break;
                    case CID_CID:
                        NidNidSerializer.get().serialize(output, (NidNidMember) rx);
                        break;
                    case CID_CID_CID:
                        NidNidNidSerializer.get().serialize(output, (NidNidNidMember) rx);
                        break;
                    case CID_CID_CID_FLOAT:
                        NidNidNidFloatSerializer.get().serialize(output, (NidNidNidFloatMember) rx);
                        break;
                    case CID_CID_CID_INT:
                        NidNidNidIntSerializer.get().serialize(output, (NidNidNidIntMember) rx);
                        break;
                    case CID_CID_CID_LONG:
                        NidNidNidLongSerializer.get().serialize(output, (NidNidNidLongMember) rx);
                        break;
                    case CID_CID_CID_STRING:
                        NidNidNidStringSerializer.get().serialize(output, (NidNidNidStringMember) rx);
                        break;
                    case CID_CID_STR:
                        NidNidStringSerializer.get().serialize(output, (NidNidStringMember) rx);
                        break;
                    case CID_FLOAT:
                        NidFloatSerializer.get().serialize(output, (NidFloatMember) rx);
                        break;
                    case CID_INT:
                        NidIntSerializer.get().serialize(output, (NidIntMember) rx);
                        break;
                    case CID_LONG:
                        NidLongSerializer.get().serialize(output, (NidLongMember) rx);
                        break;
                    case CID_STR:
                        NidStringSerializer.get().serialize(output, (NidStringMember) rx);
                        break;
                    case INT:
                        IntSerializer.get().serialize(output, (IntMember) rx);
                        break;
                    case LONG:
                        LongSerializer.get().serialize(output, (LongMember) rx);
                        break;
                    case MEMBER:
                        MembershipSerializer.get().serialize(output, (MembershipMember) rx);
                    case STR:
                        StringSerializer.get().serialize(output, (StringMember) rx);
                        break;
                    default:
                        throw new RuntimeException("Can't handle type: " + rx.getRefexType());

                }

            }
        } else {
            output.writeInt(0);
        }

        // dynamic refexes

        if (cc.annotationsDynamic != null) {
            output.writeInt(cc.annotationsDynamic.size());
            for (Object obj : cc.annotationsDynamic) {
                RefexDynamicMember member = (RefexDynamicMember) obj;
                RefexDynamicSerializer.get().serialize(output, member);
            }
        } else {
            output.writeInt(0);
        }
    }

    public static void deserialize(DataInput input, ConceptComponent cc) throws IOException {
        assert cc.nid != 0;
        assert cc.primordialStamp != 0 && cc.primordialStamp != Integer.MAX_VALUE : "Processing nid: " + cc.nid;
        cc.nid = input.readInt();
        cc.enclosingConceptNid = input.readInt();
        cc.primordialMsb = input.readLong();
        cc.primordialLsb = input.readLong();
        cc.primordialStamp = input.readInt();

        // Additional UUIDs

        short additionalUuidPartCount = input.readShort();
        if (additionalUuidPartCount > 0) {
            long[] additionalUuidParts = new long[additionalUuidPartCount];
            for (int i = 0; i > additionalUuidPartCount; i++) {
                additionalUuidParts[i] = input.readLong();
            }
            cc.additionalUuidParts = additionalUuidParts;
        }

        // static refexes
        int staticRefexCount = input.readInt();
        if (staticRefexCount > 0) {
            for (int i = 0; i < staticRefexCount; i++) {
                RefexType rxType = RefexType.readType(input);
                RefexMember member;
                switch (rxType) {
                    case ARRAY_BYTEARRAY:
                        member = ArrayOfByteArraySerializer.get().deserialize(input, new ArrayOfByteArrayMember());
                        break;
                    case BOOLEAN:
                    case CID:
                    case CID_BOOLEAN:
                    case CID_CID:
                    case CID_CID_CID:
                    case CID_CID_CID_FLOAT:
                    case CID_CID_CID_INT:
                    case CID_CID_CID_LONG:
                    case CID_CID_CID_STRING:
                    case CID_CID_STR:
                    case CID_FLOAT:
                    case CID_INT:
                    case CID_LONG:
                    case CID_STR:
                    case INT:
                    case LONG:
                    case MEMBER:
                    case STR:
                    default:
                        throw new RuntimeException("Can't handle type: " + rxType);

                }
                cc.addAnnotation(member);
            }

        }

        // dynamic refexes
        int dynamicRefexCount = input.readInt();
        if (dynamicRefexCount > 0) {
            for (int i = 0; i < dynamicRefexCount; i++) {
                RefexDynamicMember member = new RefexDynamicMember();
                RefexDynamicSerializer.get().deserialize(input, member);
                cc.addDynamicAnnotation(member);
            }
        }
    }
}
