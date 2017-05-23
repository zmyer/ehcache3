/*
 * Copyright Terracotta, Inc.
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

package org.ehcache.clustered.server.management;

import org.ehcache.clustered.common.ServerSideConfiguration;
import org.ehcache.clustered.server.ServerSideServerStore;
import org.ehcache.clustered.server.state.EhcacheStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.service.monitoring.ActiveEntityMonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistryConfiguration;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.management.service.monitoring.PassiveEntityMonitoringServiceConfiguration;
import org.terracotta.monitoring.IMonitoringProducer;

import java.util.concurrent.CompletableFuture;

import static org.ehcache.clustered.server.management.Notification.EHCACHE_SERVER_STORE_ATTACHED;
import static org.ehcache.clustered.server.management.Notification.EHCACHE_SERVER_STORE_CLIENT_RECONNECTED;
import static org.ehcache.clustered.server.management.Notification.EHCACHE_SERVER_STORE_CREATED;
import static org.ehcache.clustered.server.management.Notification.EHCACHE_SERVER_STORE_RELEASED;

public class ClusterTierManagement {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTierManagement.class);

  private final ConsumerManagementRegistry managementRegistry;
  private final EhcacheStateService ehcacheStateService;
  private final String storeIdentifier;

  public ClusterTierManagement(ServiceRegistry services, EhcacheStateService ehcacheStateService, boolean active, String storeIdentifier, String clusterTierManagerIdentifier) {
    this.ehcacheStateService = ehcacheStateService;
    this.storeIdentifier = storeIdentifier;

    // create an entity monitoring service that allows this entity to push some management information into voltron monitoring service
    EntityMonitoringService entityMonitoringService;
    if (active) {
      entityMonitoringService = services.getService(new ActiveEntityMonitoringServiceConfiguration());
    } else {
      IMonitoringProducer monitoringProducer = services.getService(new BasicServiceConfiguration<>(IMonitoringProducer.class));
      entityMonitoringService = monitoringProducer == null ? null : services.getService(new PassiveEntityMonitoringServiceConfiguration(monitoringProducer));
    }

    // create a management registry for this entity to handle exposed objects and stats
    // if mnm-server distribution is on the classpath
    managementRegistry = entityMonitoringService == null ? null : services.getService(new ConsumerManagementRegistryConfiguration(entityMonitoringService));

    if (managementRegistry != null) {

      if (active) {
        // expose settings about attached stores
        managementRegistry.addManagementProvider(new ClusterTierStateSettingsManagementProvider());
      }

      // expose settings about server stores
      managementRegistry.addManagementProvider(new ServerStoreSettingsManagementProvider(clusterTierManagerIdentifier));
      // expose settings about pools
      managementRegistry.addManagementProvider(new PoolSettingsManagementProvider());

      // expose stats about server stores
      managementRegistry.addManagementProvider(new ServerStoreStatisticsManagementProvider());
      // expose stats about pools
      managementRegistry.addManagementProvider(new PoolStatisticsManagementProvider(ehcacheStateService));
    }
  }

  // the goal of the following code is to send the management metadata from the entity into the monitoring tree AFTER the entity creation
  public void init() {
    if (managementRegistry != null) {
      LOGGER.trace("init({})", storeIdentifier);
      ServerSideServerStore serverStore = ehcacheStateService.getStore(storeIdentifier);
      ServerStoreBinding serverStoreBinding = new ServerStoreBinding(storeIdentifier, serverStore);
      CompletableFuture<Void> r1 = managementRegistry.register(serverStoreBinding);
      ServerSideConfiguration.Pool pool = ehcacheStateService.getDedicatedResourcePool(storeIdentifier);
      CompletableFuture<Void> allOf;
      if (pool != null) {
        allOf = CompletableFuture.allOf(r1, managementRegistry.register(new PoolBinding(storeIdentifier, pool, PoolBinding.AllocationType.DEDICATED)));
      } else {
        allOf = r1;
      }
      allOf.thenRun(() -> {
          managementRegistry.refresh();
          managementRegistry.pushServerEntityNotification(serverStoreBinding, EHCACHE_SERVER_STORE_CREATED.name());
          });
    }
  }

  public void clientConnected(ClientDescriptor clientDescriptor, ClusterTierClientState clientState) {
    if (managementRegistry != null) {
      LOGGER.trace("clientConnected({})", clientDescriptor);
      managementRegistry.registerAndRefresh(new ClusterTierClientStateBinding(clientDescriptor, clientState));
    }
  }


  public void clientDisconnected(ClientDescriptor clientDescriptor, ClusterTierClientState clientState) {
    if (managementRegistry != null) {
      LOGGER.trace("clientDisconnected({})", clientDescriptor);
      ClusterTierClientStateBinding clientStateBinding = new ClusterTierClientStateBinding(clientDescriptor, clientState);
      managementRegistry.pushServerEntityNotification(clientStateBinding, EHCACHE_SERVER_STORE_RELEASED.name());
      managementRegistry.unregisterAndRefresh(clientStateBinding);
    }
  }

  public void clientReconnected(ClientDescriptor clientDescriptor, ClusterTierClientState clientState) {
    if (managementRegistry != null) {
      LOGGER.trace("clientReconnected({})", clientDescriptor);
      managementRegistry.pushServerEntityNotification(new ClusterTierClientStateBinding(clientDescriptor, clientState), EHCACHE_SERVER_STORE_CLIENT_RECONNECTED.name());
    }
  }

  public void clientValidated(ClientDescriptor clientDescriptor, ClusterTierClientState clientState) {
    if (managementRegistry != null) {
      LOGGER.trace("clientValidated({})", clientDescriptor);
      ClusterTierClientStateBinding clientStateBinding = new ClusterTierClientStateBinding(clientDescriptor, clientState);
      managementRegistry.unregister(clientStateBinding);
      managementRegistry.registerAndRefresh(clientStateBinding).thenRun(() ->
        managementRegistry.pushServerEntityNotification(clientStateBinding, EHCACHE_SERVER_STORE_ATTACHED.name()));
    }
  }
}
