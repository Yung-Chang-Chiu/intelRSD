/*
 * Copyright (c) 2019 Intel Corporation
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

package com.intel.rsd.nodecomposer.composition.assembly.tasks;

import com.hazelcast.spring.context.SpringAware;
import com.intel.rsd.nodecomposer.business.EntityOperationException;
import com.intel.rsd.nodecomposer.business.redfish.services.actions.SecureEraseInvoker;
import com.intel.rsd.nodecomposer.business.services.redfish.odataid.ODataId;
import com.intel.rsd.nodecomposer.externalservices.resources.redfish.ProcessorResource;
import com.intel.rsd.nodecomposer.persistence.dao.GenericDao;
import com.intel.rsd.nodecomposer.persistence.redfish.Processor;
import com.intel.rsd.nodecomposer.utils.measures.TimeMeasured;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.Serializable;

import static javax.transaction.Transactional.TxType.REQUIRES_NEW;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Slf4j
@SpringAware
@Component
@Scope(SCOPE_PROTOTYPE)
public class SecureEraseProcessorTask extends NodeTask implements Serializable {
    private static final long serialVersionUID = 6556202327630490023L;

    @Autowired
    private transient GenericDao genericDao;
    @Autowired
    private transient SecureEraseInvoker<Processor, ProcessorResource> secureEraseInvoker;

    private ODataId processorOdataid;

    public SecureEraseProcessorTask setProcessorOdataid(ODataId processorOdataid) {
        this.processorOdataid = processorOdataid;
        return this;
    }

    @Override
    @Transactional(REQUIRES_NEW)
    @TimeMeasured(tag = "[AssemblyTask]")
    public void run() {
        Processor processor = genericDao.find(Processor.class, processorOdataid);
        try {
            secureEraseInvoker.secureErase(processor);
        } catch (EntityOperationException e) {
            log.error("SecureErase on processor action failed: " + e.getMessage(), e);
            throw new RuntimeException("SecureErase on processor action failed: ", e);
        }
    }
}
