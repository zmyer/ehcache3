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

import org.ehcache.clustered.common.PoolAllocation;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.Settings;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.service.monitoring.registry.provider.AliasBindingManagementProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Named("ServerStoreSettings")
@RequiredContext({@Named("consumerId"), @Named("type"), @Named("alias")})
class ServerStoreSettingsManagementProvider extends AliasBindingManagementProvider<ServerStoreBinding> {

  private final String clusterTierManagerIdentifier;

  ServerStoreSettingsManagementProvider(String clusterTierManagerIdentifier) {
    super(ServerStoreBinding.class);
    this.clusterTierManagerIdentifier = clusterTierManagerIdentifier;
  }

  @Override
  public Collection<Descriptor> getDescriptors() {
    Collection<Descriptor> descriptors = new ArrayList<>(super.getDescriptors());
    descriptors.add(new Settings()
      .set("type", getCapabilityName())
      .set("clusterTierManager", clusterTierManagerIdentifier)
      .set("time", System.currentTimeMillis()));
    return descriptors;
  }

  @Override
  protected ExposedServerStoreBinding internalWrap(Context context, ServerStoreBinding managedObject) {
    return new ExposedServerStoreBinding(context, managedObject);
  }

  private static class ExposedServerStoreBinding extends ExposedAliasBinding<ServerStoreBinding> {

    ExposedServerStoreBinding(Context context, ServerStoreBinding binding) {
      super(context.with("type", "ServerStore"), binding);
    }

    @Override
    public Collection<? extends Descriptor> getDescriptors() {
      return Collections.singleton(getSettings());
    }

    Settings getSettings() {
      // names taken from ServerStoreConfiguration.isCompatible()
      PoolAllocation poolAllocation = getBinding().getValue().getStoreConfiguration().getPoolAllocation();
      Settings settings = new Settings(getContext())
        .set("resourcePoolType", poolAllocation.getClass().getSimpleName().toLowerCase())
        .set("allocatedMemoryAtTime", getBinding().getValue().getAllocatedMemory())
        .set("tableCapacityAtTime", getBinding().getValue().getTableCapacity())
        .set("vitalMemoryAtTime", getBinding().getValue().getVitalMemory())
        .set("longSizeAtTime", getBinding().getValue().getSize())
        .set("dataAllocatedMemoryAtTime", getBinding().getValue().getDataAllocatedMemory())
        .set("dataOccupiedMemoryAtTime", getBinding().getValue().getDataOccupiedMemory())
        .set("dataSizeAtTime", getBinding().getValue().getDataSize())
        .set("dataVitalMemoryAtTime", getBinding().getValue().getDataVitalMemory());
      if (poolAllocation instanceof PoolAllocation.DedicatedPoolAllocation) {
        settings.set("resourcePoolDedicatedResourceName", ((PoolAllocation.DedicatedPoolAllocation) poolAllocation).getResourceName());
        settings.set("resourcePoolDedicatedSize", ((PoolAllocation.DedicatedPoolAllocation) poolAllocation).getSize());
      } else if (poolAllocation instanceof PoolAllocation.SharedPoolAllocation) {
        settings.set("resourcePoolSharedPoolName", ((PoolAllocation.SharedPoolAllocation) poolAllocation).getResourcePoolName());
      }
      return settings;
    }
  }

}
