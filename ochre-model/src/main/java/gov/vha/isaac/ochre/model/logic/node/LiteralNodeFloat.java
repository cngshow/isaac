package gov.vha.isaac.ochre.model.logic.node;

import gov.vha.isaac.ochre.api.DataTarget;
import gov.vha.isaac.ochre.api.logic.LogicNode;
import gov.vha.isaac.ochre.model.logic.LogicalExpressionOchreImpl;
import gov.vha.isaac.ochre.api.logic.NodeSemantic;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;
import gov.vha.isaac.ochre.api.util.UuidT5Generator;

/**
 * Created by kec on 12/9/14.
 */
public class LiteralNodeFloat extends LiteralNode {

    float literalValue;

    public LiteralNodeFloat(LogicalExpressionOchreImpl logicGraphVersion, DataInputStream dataInputStream) throws IOException {
        super(logicGraphVersion, dataInputStream);
        literalValue = dataInputStream.readFloat();
    }

    public LiteralNodeFloat(LogicalExpressionOchreImpl logicGraphVersion, float literalValue) {
        super(logicGraphVersion);
        this.literalValue = literalValue;
    }

    @Override
    protected void writeNodeData(DataOutput dataOutput, DataTarget dataTarget) throws IOException {
        super.writeData(dataOutput, dataTarget);
        dataOutput.writeFloat(literalValue);
    }

    @Override
    public NodeSemantic getNodeSemantic() {
        return NodeSemantic.LITERAL_FLOAT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        LiteralNodeFloat that = (LiteralNodeFloat) o;

        return Float.compare(that.literalValue, literalValue) == 0;
    }

    @Override
    protected int compareFields(LogicNode o) {
        LiteralNodeFloat that = (LiteralNodeFloat) o;
        return Float.compare(this.literalValue, that.literalValue);
    }

    @Override
    protected UUID initNodeUuid() {
        return UuidT5Generator.get(getNodeSemantic().getSemanticUuid(),
                Float.toString(literalValue));
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (literalValue != +0.0f ? Float.floatToIntBits(literalValue) : 0);
        return result;
    }

    public float getLiteralValue() {
        return literalValue;
    }

    @Override
    public String toString() {
        return toString("");
    }
    
    @Override
    public String toString(String nodeIdSuffix) {
        return "Float literal[" + getNodeIndex() + nodeIdSuffix + "]" + literalValue + super.toString(nodeIdSuffix);
    }
}
