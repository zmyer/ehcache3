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
package org.ehcache.internal.store.heap.bytesized;

import org.ehcache.config.EvictionVeto;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Expirations;
import org.ehcache.expiry.Expiry;
import org.ehcache.internal.TimeSource;
import org.ehcache.internal.sizeof.DefaultSizeOfEngine;
import org.ehcache.spi.cache.Store;
import org.ehcache.spi.serialization.Serializer;

import java.io.Serializable;

import static org.ehcache.config.ResourcePoolsBuilder.newResourcePoolsBuilder;

public class OnHeapStoreEvictionTest extends org.ehcache.internal.store.heap.OnHeapStoreEvictionTest {

  protected <K, V> OnHeapStoreForTests<K, V> newStore(final TimeSource timeSource,
      final EvictionVeto<? super K, ? super V> veto) {
    return new OnHeapStoreForTests<K, V>(new Store.Configuration<K, V>() {
      @SuppressWarnings("unchecked")
      @Override
      public Class<K> getKeyType() {
        return (Class<K>) String.class;
      }

      @SuppressWarnings("unchecked")
      @Override
      public Class<V> getValueType() {
        return (Class<V>) Serializable.class;
      }

      @Override
      public EvictionVeto<? super K, ? super V> getEvictionVeto() {
        return veto;
      }

      @Override
      public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
      }

      @Override
      public Expiry<? super K, ? super V> getExpiry() {
        return Expirations.noExpiration();
      }

      @Override
      public ResourcePools getResourcePools() {
        return newResourcePoolsBuilder().heap(500, MemoryUnit.B).build();
      }

      @Override
      public Serializer<K> getKeySerializer() {
        throw new AssertionError();
      }

      @Override
      public Serializer<V> getValueSerializer() {
        throw new AssertionError();
      }
    }, timeSource, new DefaultSizeOfEngine(Long.MAX_VALUE, Long.MAX_VALUE));
  }

}