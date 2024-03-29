// CHECKSTYLE_OFF: Copyrighted to ASF
/*
 * Copyright 1999-2004 The Apache Software Foundation
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
// CHECKSTYLE_ON
package com.google.gwt.emultest.java.util;

import java.util.Map;

/** A default implementation of {@link java.util.Map.Entry} */
@SuppressWarnings("rawtypes")
class DefaultMapEntry implements Map.Entry {

  private Object key;
  private Object value;

  /** Constructs a new <Code>DefaultMapEntry</Code> with a null key and null value. */
  public DefaultMapEntry() {}

  /**
   * Constructs a new <Code>DefaultMapEntry</Code> with the given key and given value.
   *
   * @param key the key for the entry, may be null
   * @param value the value for the entyr, may be null
   */
  public DefaultMapEntry(Object key, Object value) {
    this.key = key;
    this.value = value;
  }

  /** Implemented per API documentation of {@link java.util.Map.Entry#equals(Object)} */
  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o == this) {
      return true;
    }

    if (!(o instanceof Map.Entry)) {
      return false;
    }
    Map.Entry e2 = (Map.Entry) o;
    return ((getKey() == null ? e2.getKey() == null : getKey().equals(e2.getKey()))
        && (getValue() == null ? e2.getValue() == null : getValue().equals(e2.getValue())));
  }

  /** Implemented per API documentation of {@link java.util.Map.Entry#hashCode()} */
  @Override
  public int hashCode() {
    return ((getKey() == null ? 0 : getKey().hashCode())
        ^ (getValue() == null ? 0 : getValue().hashCode()));
  }

  // Map.Entry interface
  // -------------------------------------------------------------------------

  /**
   * Returns the key.
   *
   * @return the key
   */
  @Override
  public Object getKey() {
    return key;
  }

  /**
   * Returns the value.
   *
   * @return the value
   */
  @Override
  public Object getValue() {
    return value;
  }

  // Properties
  // -------------------------------------------------------------------------

  /**
   * Sets the key. This method does not modify any map.
   *
   * @param key the new key
   */
  public void setKey(Object key) {
    this.key = key;
  }

  /**
   * Note that this method only sets the local reference inside this object and does not modify the
   * original Map.
   *
   * @return the old value of the value
   * @param value the new value
   */
  @Override
  public Object setValue(Object value) {
    Object previous = this.value;
    this.value = value;
    return previous;
  }
}
