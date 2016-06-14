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

package org.ehcache.clustered.client.internal.store;

import org.ehcache.clustered.client.internal.store.operations.Result;
import org.ehcache.clustered.common.store.Chain;

import java.util.Collections;
import java.util.Map;

/**
 * Represents the result of a {@link Chain} resolution.
 * Implementors would be wrappers over the compacted chain and the resolved operations.
 * A resolver may or may not have resolved all the different keys in a chain.
 *
 * @param <K> the Key type
 */
public interface ResolvedChain<K, V> {

  Chain getCompactedChain();

  Result<V> getResolvedResult(K key);

  /**
   * Represents the {@link ResolvedChain} result of a resolver that resolves
   * all the keys in a {@link Chain}
   */
  class Impl<K, V> implements ResolvedChain<K, V> {

    private final Chain compactedChain;
    private final Map<K, Result<V>> resolvedOperations;

    public Impl(Chain compactedChain, Map<K, Result<V>> resolvedOperations) {
      this.compactedChain = compactedChain;
      this.resolvedOperations = resolvedOperations;
    }

    public Impl(Chain compactedChain, K key, Result<V> result) {
      this(compactedChain, Collections.singletonMap(key, result));
    }

    public Chain getCompactedChain() {
      return this.compactedChain;
    }

    public Result<V> getResolvedResult(K key) {
      return resolvedOperations.get(key);
    }
  }
}
