/*
 * Copyright (c) 2015-2019 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.rsd.nodecomposer.composition.allocation.strategy;

import com.intel.rsd.nodecomposer.business.Violations;
import com.intel.rsd.nodecomposer.business.services.redfish.requests.RequestedNode;
import com.intel.rsd.nodecomposer.composition.allocation.AllocationStrategy;
import com.intel.rsd.nodecomposer.composition.allocation.strategy.matcher.NewDriveAllocationContextDescriber;
import com.intel.rsd.nodecomposer.composition.allocation.validation.NewRemoteDriveValidator;
import com.intel.rsd.nodecomposer.composition.assembly.IscsiAssemblyTasksProvider;
import com.intel.rsd.nodecomposer.composition.assembly.tasks.InitiatorEndpointAssemblyTaskFactory;
import com.intel.rsd.nodecomposer.composition.assembly.tasks.NewVolumeTaskFactory;
import com.intel.rsd.nodecomposer.composition.assembly.tasks.NodeTask;
import com.intel.rsd.nodecomposer.composition.assembly.tasks.ZoneTaskFactory;
import com.intel.rsd.nodecomposer.discovery.external.partial.EndpointObtainer;
import com.intel.rsd.nodecomposer.persistence.dao.GenericDao;
import com.intel.rsd.nodecomposer.persistence.redfish.ComposedNode;
import com.intel.rsd.nodecomposer.persistence.redfish.ComputerSystem;
import com.intel.rsd.nodecomposer.persistence.redfish.StoragePool;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.intel.rsd.nodecomposer.types.Protocol.ISCSI;
import static javax.transaction.Transactional.TxType.MANDATORY;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_PROTOTYPE)
@Transactional(MANDATORY)
@SuppressWarnings({"checkstyle:ClassFanOutComplexity"})
public class NewRemoteDriveAllocationStrategy implements AllocationStrategy {
    private final NewRemoteDriveValidator validator;
    private final NewVolumeTaskFactory newVolumeTaskFactory;
    private final InitiatorEndpointAssemblyTaskFactory initiatorEndpointAssemblyTaskFactory;
    private final ZoneTaskFactory zoneTaskFactory;
    private final EndpointObtainer endpointObtainer;
    private final IscsiAssemblyTasksProvider iscsiAssemblyTasksProvider;
    private final GenericDao genericDao;
    private final DelegatingDescriber driveDescriptorFinder;

    @Setter
    private RequestedNode.RemoteDrive drive;

    @Getter
    private List<NodeTask> tasks = new ArrayList<>();

    @Autowired
    @SuppressWarnings({"checkstyle:ParameterNumber"})
    public NewRemoteDriveAllocationStrategy(NewRemoteDriveValidator validator, NewVolumeTaskFactory newVolumeTaskFactory,
                                            InitiatorEndpointAssemblyTaskFactory initiatorEndpointAssemblyTaskFactory,
                                            ZoneTaskFactory zoneTaskFactory, EndpointObtainer endpointObtainer,
                                            IscsiAssemblyTasksProvider iscsiAssemblyTasksProvider, GenericDao genericDao,
                                            DelegatingDescriber driveDescriptorFinder) {
        this.validator = validator;
        this.newVolumeTaskFactory = newVolumeTaskFactory;
        this.initiatorEndpointAssemblyTaskFactory = initiatorEndpointAssemblyTaskFactory;
        this.zoneTaskFactory = zoneTaskFactory;
        this.endpointObtainer = endpointObtainer;
        this.iscsiAssemblyTasksProvider = iscsiAssemblyTasksProvider;
        this.genericDao = genericDao;
        this.driveDescriptorFinder = driveDescriptorFinder;
    }

    @Override
    public Violations validate() {
        return validator.validate(drive);
    }

    @Override
    public void allocate(ComposedNode composedNode) throws ResourceFinderException {
        val descriptor = initialize();
        StoragePool storagePool = genericDao.find(StoragePool.class, descriptor.getStoragePoolODataId());
        reserveSpaceOnLogicalVolumeGroup(composedNode, storagePool, descriptor.getCapacity());

        descriptor.setStorageServiceODataId(storagePool.getStorageService().getUri());
        tasks.add(newVolumeTaskFactory.createTask(descriptor));

        ComputerSystem computerSystem = composedNode.getComputerSystem();
        if (endpointObtainer.getInitiatorEndpoint(computerSystem, storagePool.getStorageService()) == null) {
            tasks.add(initiatorEndpointAssemblyTaskFactory.create(storagePool.getStorageService().getFabric().getUri()));
        }

        if (computerSystem.hasNetworkInterfaceWithNetworkDeviceFunction() && ISCSI.equals(descriptor.getProtocol())) {
            tasks.addAll(iscsiAssemblyTasksProvider.createTasks());
        }
        tasks.add(zoneTaskFactory.create());
    }

    private RemoteDriveAllocationContextDescriptor initialize() throws ResourceFinderException {
        return driveDescriptorFinder.describe(drive);
    }

    private void reserveSpaceOnLogicalVolumeGroup(ComposedNode composedNode, StoragePool storagePool, BigDecimal capacity) {
        composedNode.setRemoteDriveCapacityGib(capacity);
        storagePool.addComposedNode(composedNode);
    }

    @Component
    @Scope(SCOPE_SINGLETON)
    static class DelegatingDescriber implements RemoteDriveAllocationContextDescriber {
        private final NewDriveAllocationContextDescriber newDriveAllocationContextDescriber;
        private final MasterBasedNewDriveAllocationContextDescriber masterBasedNewDriveAllocationContextDescriber;

        @Autowired
        DelegatingDescriber(NewDriveAllocationContextDescriber newDriveAllocationContextDescriber,
                            MasterBasedNewDriveAllocationContextDescriber masterBasedNewDriveAllocationContextDescriber) {
            this.newDriveAllocationContextDescriber = newDriveAllocationContextDescriber;
            this.masterBasedNewDriveAllocationContextDescriber = masterBasedNewDriveAllocationContextDescriber;
        }

        @Override
        @Transactional(MANDATORY)
        public RemoteDriveAllocationContextDescriptor describe(RequestedNode.RemoteDrive remoteDrive) throws ResourceFinderException {
            if (remoteDrive.getMaster() != null) {
                return masterBasedNewDriveAllocationContextDescriber.describe(remoteDrive);
            }

            return newDriveAllocationContextDescriber.describe(remoteDrive);
        }
    }
}
