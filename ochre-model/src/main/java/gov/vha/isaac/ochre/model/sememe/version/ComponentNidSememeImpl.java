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
package gov.vha.isaac.ochre.model.sememe.version;

import gov.vha.isaac.ochre.api.component.sememe.version.MutableComponentNidSememe;
import gov.vha.isaac.ochre.model.DataBuffer;
import gov.vha.isaac.ochre.model.sememe.SememeChronologyImpl;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;

/**
 * Used for description dialect preferences
 *
 * @author kec
 */
public class ComponentNidSememeImpl<T extends ComponentNidSememeImpl<T>> extends SememeVersionImpl<T> implements MutableComponentNidSememe<T> {

    int componentNid = Integer.MAX_VALUE;

    public ComponentNidSememeImpl(SememeChronologyImpl<T> container, 
            int stampSequence, short versionSequence) {
        super(container, 
                stampSequence, versionSequence);
    }

    public ComponentNidSememeImpl(SememeChronologyImpl<T> container, 
            int stampSequence, short versionSequence, DataBuffer data) {
        super(container, 
                stampSequence, versionSequence);
        this.componentNid = data.getInt();
    }

    @Override
    protected void writeVersionData(DataBuffer data) {
        super.writeVersionData(data);
        data.putInt(componentNid);
    }

    @Override
    public SememeType getSememeType() {
        return SememeType.COMPONENT_NID;
    }

    @Override
    public int getComponentNid() {
        return componentNid;
    }

    @Override
    public void setComponentNid(int componentNid) {
        if (this.componentNid != Integer.MAX_VALUE) {
            checkUncommitted();
        }
        this.componentNid = componentNid;
    }

}
