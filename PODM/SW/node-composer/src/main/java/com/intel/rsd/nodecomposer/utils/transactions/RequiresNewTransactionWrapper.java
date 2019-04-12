/*
 * Copyright (c) 2017-2019 Intel Corporation
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

package com.intel.rsd.nodecomposer.utils.transactions;

import com.intel.rsd.nodecomposer.utils.retry.RetryOnRollback;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.REQUIRES_NEW;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
public class RequiresNewTransactionWrapper {

    @Transactional(REQUIRES_NEW)
    public <E extends Exception, R> R run(Task<R, E> executable) throws E {
        return executable.execute();
    }

    @Transactional(REQUIRES_NEW)
    @RetryOnRollback(15)
    public <E extends Exception> void runInRetryableTransaction(VoidTask<E> executable) throws E {
        executable.execute();
    }

    public interface Task<R, E extends Exception> {
        R execute() throws E;
    }

    public interface VoidTask<E extends Exception> {
        void execute() throws E;
    }
}