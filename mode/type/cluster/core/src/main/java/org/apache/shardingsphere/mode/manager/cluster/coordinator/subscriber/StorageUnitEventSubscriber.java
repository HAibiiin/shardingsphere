/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.mode.manager.cluster.coordinator.subscriber;

import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.util.eventbus.EventSubscriber;
import org.apache.shardingsphere.mode.event.dispatch.datasource.unit.AlterStorageUnitEvent;
import org.apache.shardingsphere.mode.event.dispatch.datasource.unit.RegisterStorageUnitEvent;
import org.apache.shardingsphere.mode.event.dispatch.datasource.unit.UnregisterStorageUnitEvent;
import org.apache.shardingsphere.mode.manager.ContextManager;

/**
 * Storage unit event subscriber.
 */
@RequiredArgsConstructor
public final class StorageUnitEventSubscriber implements EventSubscriber {
    
    private final ContextManager contextManager;
    
    /**
     * Renew for register storage unit.
     *
     * @param event register storage unit event
     */
    @Subscribe
    public synchronized void renew(final RegisterStorageUnitEvent event) {
        if (!event.getActiveVersion().equals(contextManager.getPersistServiceFacade().getMetaDataPersistService().getMetaDataVersionPersistService()
                .getActiveVersionByFullPath(event.getActiveVersionKey()))) {
            return;
        }
        contextManager.getMetaDataContextManager().getConfigurationManager().registerStorageUnit(event.getDatabaseName(),
                contextManager.getPersistServiceFacade().getMetaDataPersistService().getDataSourceUnitService().load(event.getDatabaseName(), event.getStorageUnitName()));
    }
    
    /**
     * Renew for alter storage unit.
     *
     * @param event register storage unit event
     */
    @Subscribe
    public synchronized void renew(final AlterStorageUnitEvent event) {
        if (!event.getActiveVersion().equals(contextManager.getPersistServiceFacade().getMetaDataPersistService().getMetaDataVersionPersistService()
                .getActiveVersionByFullPath(event.getActiveVersionKey()))) {
            return;
        }
        contextManager.getMetaDataContextManager().getConfigurationManager().alterStorageUnit(
                event.getDatabaseName(), contextManager.getPersistServiceFacade().getMetaDataPersistService().getDataSourceUnitService().load(event.getDatabaseName(), event.getStorageUnitName()));
    }
    
    /**
     * Renew for unregister storage unit.
     *
     * @param event register storage unit event
     */
    @Subscribe
    public synchronized void renew(final UnregisterStorageUnitEvent event) {
        if (!contextManager.getMetaDataContexts().getMetaData().containsDatabase(event.getDatabaseName())) {
            return;
        }
        contextManager.getMetaDataContextManager().getConfigurationManager().unregisterStorageUnit(event.getDatabaseName(), event.getStorageUnitName());
    }
}
