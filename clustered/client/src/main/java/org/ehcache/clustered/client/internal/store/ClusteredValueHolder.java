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

import org.ehcache.core.spi.store.AbstractValueHolder;

import java.util.concurrent.TimeUnit;

public class ClusteredValueHolder<V> extends AbstractValueHolder<V> {

  public static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

  private final V value;

  public ClusteredValueHolder(V value) {
    super(0, 0);
    if(value == null) {
      throw new NullPointerException("Value can not be null");
    }
    this.value = value;
  }

  @Override
  protected TimeUnit nativeTimeUnit() {
    return TIME_UNIT;
  }

  @Override
  public V value() {
    return value;
  }
}
