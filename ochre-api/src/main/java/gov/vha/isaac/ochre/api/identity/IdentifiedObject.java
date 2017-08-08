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
package gov.vha.isaac.ochre.api.identity;

import java.util.List;
import java.util.UUID;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IdentifiedComponentBuilder;

/**
 *
 * @author kec
 */
public interface IdentifiedObject {

    List<UUID> getUuidList();

    default UUID[] getUuids() {
        return getUuidList().toArray(new UUID[getUuidList().size()]);
    }

    /**
     * If not yet set {@link IdentifiedComponentBuilder#setPrimordialUuid(UUID)}, a randomly generated UUID will be automatically used.
     * Otherwise, return already set value.
     * @param uuid
     * @return the builder for chaining of operations in a fluent pattern.
     */
    default UUID getPrimordialUuid() {
        return getUuidList().get(0);
    }
    
    default int getNid() {
         return Get.identifierService().getNidForUuids(getUuidList());
     }

    default String toUserString() {
        return toString();
    }


}
