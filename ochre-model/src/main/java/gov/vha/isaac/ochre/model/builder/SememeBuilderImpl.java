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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY_STATE_SET KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.model.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

import gov.vha.isaac.ochre.api.DataTarget;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IdentifiedComponentBuilder;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuildListenerI;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.task.OptionalWaitTask;
import gov.vha.isaac.ochre.api.util.UuidFactory;
import gov.vha.isaac.ochre.api.util.UuidT5Generator;
import gov.vha.isaac.ochre.model.sememe.SememeChronologyImpl;
import gov.vha.isaac.ochre.model.sememe.version.ComponentNidSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.DescriptionSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.DynamicSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.LogicGraphSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.LongSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.SememeVersionImpl;
import gov.vha.isaac.ochre.model.sememe.version.StringSememeImpl;
import javafx.concurrent.Task;

/**
 *
 * @author kec
 * @param <C>
 */
public class SememeBuilderImpl<C extends SememeChronology<? extends SememeVersion<?>>> extends ComponentBuilder<C> implements SememeBuilder<C> {
	private static final Logger LOG = LogManager.getLogger(SememeBuilderImpl.class);
    IdentifiedComponentBuilder referencedComponentBuilder;
    int referencedComponentNid = Integer.MAX_VALUE;
    
    int assemblageConceptSequence;
    SememeType sememeType;
    Object[] parameters;

    public SememeBuilderImpl(IdentifiedComponentBuilder referencedComponentBuilder, 
            int assemblageConceptSequence, 
            SememeType sememeType, Object... paramaters) {
        this.referencedComponentBuilder = referencedComponentBuilder;
        this.assemblageConceptSequence = assemblageConceptSequence;
        this.sememeType = sememeType;
        this.parameters = paramaters;
    }
    public SememeBuilderImpl(int referencedComponentNid, 
            int assemblageConceptSequence, 
            SememeType sememeType, Object... paramaters) {
        this.referencedComponentNid = referencedComponentNid;
        this.assemblageConceptSequence = assemblageConceptSequence;
        this.sememeType = sememeType;
        this.parameters = paramaters;
    }
    
    @Override
    public OptionalWaitTask<C> build(EditCoordinate editCoordinate, 
            ChangeCheckerMode changeCheckerMode,
            List<ObjectChronology<? extends StampedVersion>> builtObjects) throws IllegalStateException {
        
        List<SememeBuildListenerI> sememeBuildListeners = LookupService.get().getAllServices(SememeBuildListenerI.class);
        for (SememeBuildListenerI listener : sememeBuildListeners) {
        	if (listener != null) {
                if (listener.isEnabled()) {
                    //LOG.info("Calling " + listener.getListenerName() + ".applyBefore(...)");
                    try {
                        listener.applyBefore(editCoordinate, changeCheckerMode, builtObjects);
                    } catch (RuntimeException e) {
                        LOG.error("FAILED running " + listener.getListenerName() + ".applyBefore(...): ", e);
                    }
                } else {
                    LOG.info("NOT calling " + listener.getListenerName() + ".applyBefore(...) because listener has been disabled");
                }
            }
        }

        if (referencedComponentNid == Integer.MAX_VALUE) {
            referencedComponentNid = Get.identifierService().getNidForUuids(referencedComponentBuilder.getUuids());
        }
        SememeVersion<?> version;
        SememeChronologyImpl sememeChronicle;
        int sememeNid = Get.identifierService().getNidForUuids(this.getUuids());
        if (Get.sememeService().hasSememe(sememeNid)) {
            sememeChronicle = (SememeChronologyImpl)Get.sememeService().getSememe(sememeNid);
            
            if (sememeChronicle.getSememeType() != sememeType 
                    || !sememeChronicle.getPrimordialUuid().equals(getPrimordialUuid())
                    || sememeChronicle.getAssemblageSequence() != assemblageConceptSequence
                    || sememeChronicle.getReferencedComponentNid() != referencedComponentNid) {
                throw new RuntimeException("Builder is being used to attempt a mis-matched edit of an existing sememe!");
            }
        }
        else {
            sememeChronicle = new SememeChronologyImpl(sememeType, 
                getPrimordialUuid(), 
                sememeNid, 
                assemblageConceptSequence, 
                referencedComponentNid, 
                Get.identifierService().getSememeSequenceForUuids(this.getUuids()));
        }
        sememeChronicle.setAdditionalUuids(additionalUuids);
        switch (sememeType) {
            case COMPONENT_NID:
                ComponentNidSememeImpl cnsi = (ComponentNidSememeImpl) 
                        sememeChronicle.createMutableVersion(ComponentNidSememeImpl.class, state, editCoordinate);
                cnsi.setComponentNid((Integer) parameters[0]);
                version = cnsi;
                break;
            case LONG:
                LongSememeImpl lsi = (LongSememeImpl) 
                        sememeChronicle.createMutableVersion(LongSememeImpl.class, state, editCoordinate);
                lsi.setLongValue((Long) parameters[0]);
                version = lsi;
                break;
            case LOGIC_GRAPH:
                LogicGraphSememeImpl lgsi = (LogicGraphSememeImpl) 
                        sememeChronicle.createMutableVersion(LogicGraphSememeImpl.class, state, editCoordinate);
                lgsi.setGraphData(((LogicalExpression) parameters[0]).getData(DataTarget.INTERNAL));
                version = lgsi;
                break;
            case MEMBER:
                SememeVersionImpl svi = (SememeVersionImpl)
                        sememeChronicle.createMutableVersion(SememeVersionImpl.class, state, editCoordinate);
                version = svi;
                break;
            case STRING:
                StringSememeImpl ssi = (StringSememeImpl)
                    sememeChronicle.createMutableVersion(StringSememeImpl.class, state, editCoordinate);
                ssi.setString((String) parameters[0]);
                version = ssi;
                break;
            case DESCRIPTION: {
                DescriptionSememeImpl dsi = (DescriptionSememeImpl)
                    sememeChronicle.createMutableVersion(DescriptionSememeImpl.class, state, editCoordinate);
                dsi.setCaseSignificanceConceptSequence((Integer) parameters[0]);
                dsi.setDescriptionTypeConceptSequence((Integer) parameters[1]);
                dsi.setLanguageConceptSequence((Integer) parameters[2]);
                dsi.setText((String) parameters[3]);
                version = dsi;
                break;
            }
            case DYNAMIC: {
                DynamicSememeImpl dsi = (DynamicSememeImpl)sememeChronicle.createMutableVersion(DynamicSememeImpl.class, state, editCoordinate);
                if (parameters != null && parameters.length > 0) {
                    //See notes in SememeBuilderProvider - this casting / wrapping nonesense it to work around Java being stupid.
                    dsi.setData(((AtomicReference<DynamicSememeData[]>)parameters[0]).get());
                }
                version = dsi;
                //TODO DAN this needs to fire the validator!
                break;
            }	
            default:
                throw new UnsupportedOperationException("Can't handle: " + sememeType);
        }
        
        Task<Void> primaryNested;
        if (changeCheckerMode == ChangeCheckerMode.ACTIVE) {
            primaryNested = Get.commitService().addUncommitted(sememeChronicle);
        } else {
            primaryNested = Get.commitService().addUncommittedNoChecks(sememeChronicle);
        }
        ArrayList<OptionalWaitTask<?>> nested = new ArrayList<>();
        sememeBuilders.forEach((builder) -> nested.add(builder.build(editCoordinate, changeCheckerMode, builtObjects)));
        builtObjects.add(sememeChronicle);
        for (SememeBuildListenerI listener : sememeBuildListeners) {
            if (listener != null) {
                if (listener.isEnabled()) {
                    //LOG.info("Calling " + listener.getListenerName() + ".applyAfter(...)");
                    try {
                        listener.applyAfter(editCoordinate, changeCheckerMode, version, builtObjects);
                    } catch (RuntimeException e) {
                        LOG.error("FAILED running " + listener.getListenerName() + ".applyAfter(...): ", e);
                    }
                } else {
                    LOG.info("NOT calling " + listener.getListenerName() + ".applyAfter(...) because listener has been disabled");
                }
            }
        }
        return new OptionalWaitTask<C>(primaryNested, (C)sememeChronicle, nested);
    }

    @Override
    public C build(int stampSequence,
        List<ObjectChronology<? extends StampedVersion>> builtObjects) throws IllegalStateException {
        if (referencedComponentNid == Integer.MAX_VALUE) {
            referencedComponentNid = Get.identifierService().getNidForUuids(referencedComponentBuilder.getUuids());
        }
        
        List<SememeBuildListenerI> sememeBuildListeners = LookupService.get().getAllServices(SememeBuildListenerI.class);
        for (SememeBuildListenerI listener : sememeBuildListeners) {
            if (listener != null) {
                if (listener.isEnabled()) {
                    //LOG.info("Calling " + listener.getListenerName() + ".applyBefore(...)");
                    try {
                        listener.applyBefore(stampSequence, builtObjects);
                    } catch (RuntimeException e) {
                        LOG.error("FAILED running " + listener.getListenerName() + ".applyBefore(...): ", e);
                    }
                } else {
                    LOG.info("NOT calling " + listener.getListenerName() + ".applyBefore(...) because listener has been disabled");
                }
            }
        }
        SememeVersion<?> version;
        SememeChronologyImpl sememeChronicle;
        int sememeNid = Get.identifierService().getNidForUuids(this.getUuids());
        if (Get.sememeService().hasSememe(sememeNid)) {
            sememeChronicle = (SememeChronologyImpl)Get.sememeService().getSememe(sememeNid);
            
            if (sememeChronicle.getSememeType() != sememeType 
                    || !sememeChronicle.getPrimordialUuid().equals(getPrimordialUuid())
                    || sememeChronicle.getAssemblageSequence() != assemblageConceptSequence
                    || sememeChronicle.getReferencedComponentNid() != referencedComponentNid) {
                throw new RuntimeException("Builder is being used to attempt a mis-matched edit of an existing sememe!");
            }
        }
        else {
            sememeChronicle = new SememeChronologyImpl(sememeType, 
                getPrimordialUuid(), 
                sememeNid, 
                assemblageConceptSequence, 
                referencedComponentNid, 
                Get.identifierService().getSememeSequenceForUuids(this.getUuids()));
        }
        
        sememeChronicle.setAdditionalUuids(additionalUuids);
        switch (sememeType) {
            case COMPONENT_NID:
                ComponentNidSememeImpl cnsi = (ComponentNidSememeImpl) 
                        sememeChronicle.createMutableVersion(ComponentNidSememeImpl.class, stampSequence);
                cnsi.setComponentNid((Integer) parameters[0]);
                version = cnsi;
                break;
            case LONG:
                LongSememeImpl lsi = (LongSememeImpl) 
                        sememeChronicle.createMutableVersion(LongSememeImpl.class, stampSequence);
                lsi.setLongValue((Long) parameters[0]);
                version = lsi;
                break;
            case LOGIC_GRAPH:
                LogicGraphSememeImpl lgsi = (LogicGraphSememeImpl) 
                        sememeChronicle.createMutableVersion(LogicGraphSememeImpl.class, stampSequence);
                lgsi.setGraphData(((LogicalExpression) parameters[0]).getData(DataTarget.INTERNAL));
                version = lgsi;
                break;
            case MEMBER:
                SememeVersionImpl svi = (SememeVersionImpl)
                        sememeChronicle.createMutableVersion(SememeVersionImpl.class, stampSequence);
                version = svi;
                break;
            case STRING:
                StringSememeImpl ssi = (StringSememeImpl)
                    sememeChronicle.createMutableVersion(StringSememeImpl.class, stampSequence);
                ssi.setString((String) parameters[0]);
                version = ssi;
                break;
            case DESCRIPTION: {
                DescriptionSememeImpl dsi = (DescriptionSememeImpl)
                    sememeChronicle.createMutableVersion(DescriptionSememeImpl.class, stampSequence);
                dsi.setCaseSignificanceConceptSequence((Integer) parameters[0]);
                dsi.setDescriptionTypeConceptSequence((Integer) parameters[1]);
                dsi.setLanguageConceptSequence((Integer) parameters[2]);
                dsi.setText((String) parameters[3]);
                version = dsi;
                break;
            }
            case DYNAMIC: {
                DynamicSememeImpl dsi = (DynamicSememeImpl)sememeChronicle.createMutableVersion(DynamicSememeImpl.class, stampSequence);
                if (parameters != null && parameters.length > 0) {
                    //See notes in SememeBuilderProvider - this casting / wrapping nonesense it to work around Java being stupid.
                    dsi.setData(((AtomicReference<DynamicSememeData[]>)parameters[0]).get());
                }
                //TODO Dan this needs to fire the validator!
                version = dsi;
                break;
            }	
            default:
                throw new UnsupportedOperationException("Can't handle: " + sememeType);
        }
        sememeBuilders.forEach((builder) -> builder.build(stampSequence, builtObjects));
        builtObjects.add(sememeChronicle);
        for (SememeBuildListenerI listener : sememeBuildListeners) {
            if (listener != null) {
                if (listener.isEnabled()) {
                    //LOG.info("Calling " + listener.getListenerName() + ".applyAfter(...)");
                    try {
                        listener.applyAfter(stampSequence, version, builtObjects);
                    } catch (RuntimeException e) {
                        LOG.error("FAILED running " + listener.getListenerName() + ".applyAfter(...): ", e);
                    }
                } else {
                    LOG.info("NOT calling " + listener.getListenerName() + ".applyAfter(...) because listener has been disabled");
                }
            }
        }
        return (C) sememeChronicle;
    }

    @Override
    public IdentifiedComponentBuilder<C> setT5Uuid() {
        if (isPrimordialUuidSet() && getPrimordialUuid().version() == 4) {
        	throw new RuntimeException("Attempting to set Type 5 UUID where the UUID was previously set to random");
        }

        if (isPrimordialUuidSet()) {
            int assemblageNid = Get.identifierService().getConceptNid(assemblageConceptSequence);
            UUID assemblageUuid = Get.identifierService().getUuidPrimordialForNid(assemblageNid).get();

            UUID refCompUuid = null;
            if (referencedComponentBuilder != null) {
                refCompUuid = referencedComponentBuilder.getPrimordialUuid();
            } else {
                refCompUuid = Get.identifierService().getUuidPrimordialForNid(referencedComponentNid).get();
            }

            if (sememeType == SememeType.LOGIC_GRAPH) {
                setPrimordialUuid(UuidFactory.getUuidForLogicGraphSememe(UuidT5Generator.PATH_ID_FROM_FS_DESC, assemblageUuid, refCompUuid, parameters));
            } else if (sememeType == SememeType.MEMBER) {
                setPrimordialUuid(UuidFactory.getUuidForMemberSememe(UuidT5Generator.PATH_ID_FROM_FS_DESC, assemblageUuid, refCompUuid));
            } else if (sememeType == SememeType.DYNAMIC) {
                setPrimordialUuid(UuidFactory.getUuidForDynamicSememe(UuidT5Generator.PATH_ID_FROM_FS_DESC, assemblageUuid, refCompUuid, parameters));
            } else if (sememeType == SememeType.COMPONENT_NID) {
                UUID componentUuid = Get.identifierService().getUuidPrimordialForNid((Integer)parameters[0]).get();
                setPrimordialUuid(UuidFactory.getUuidForComponentNidSememe(UuidT5Generator.PATH_ID_FROM_FS_DESC, assemblageUuid, refCompUuid, componentUuid));
            } else if (sememeType == SememeType.DESCRIPTION) {
                setPrimordialUuid(UuidFactory.getUuidForDescriptionSememe(UuidT5Generator.PATH_ID_FROM_FS_DESC, assemblageUuid, refCompUuid, parameters));
            } else if (sememeType == SememeType.STRING) {
                setPrimordialUuid(UuidFactory.getUuidForStringSememe(UuidT5Generator.PATH_ID_FROM_FS_DESC, assemblageUuid, refCompUuid, parameters));
            }
        }
        
        return this;
    }
}
