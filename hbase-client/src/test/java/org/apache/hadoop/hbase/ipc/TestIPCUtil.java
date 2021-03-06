/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.ipc;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.hadoop.hbase.HBaseClassTestRule;
import org.apache.hadoop.hbase.exceptions.ClientExceptionsUtil;
import org.apache.hadoop.hbase.exceptions.TimeoutIOException;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({ ClientTests.class, SmallTests.class })
public class TestIPCUtil {

  @ClassRule
  public static final HBaseClassTestRule CLASS_RULE =
    HBaseClassTestRule.forClass(TestIPCUtil.class);

  private static Throwable create(Class<? extends Throwable> clazz) throws InstantiationException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    try {
      Constructor<? extends Throwable> c = clazz.getDeclaredConstructor();
      c.setAccessible(true);
      return c.newInstance();
    } catch (NoSuchMethodException e) {
      // fall through
    }

    try {
      Constructor<? extends Throwable> c = clazz.getDeclaredConstructor(String.class);
      c.setAccessible(true);
      return c.newInstance("error");
    } catch (NoSuchMethodException e) {
      // fall through
    }

    try {
      Constructor<? extends Throwable> c = clazz.getDeclaredConstructor(Throwable.class);
      c.setAccessible(true);
      return c.newInstance(new Exception("error"));
    } catch (NoSuchMethodException e) {
      // fall through
    }

    try {
      Constructor<? extends Throwable> c =
        clazz.getDeclaredConstructor(String.class, Throwable.class);
      c.setAccessible(true);
      return c.newInstance("error", new Exception("error"));
    } catch (NoSuchMethodException e) {
      // fall through
    }

    Constructor<? extends Throwable> c =
      clazz.getDeclaredConstructor(Throwable.class, Throwable.class);
    c.setAccessible(true);
    return c.newInstance(new Exception("error"), "error");
  }

  /**
   * See HBASE-21862, it is very important to keep the original exception type for connection
   * exceptions.
   */
  @Test
  public void testWrapConnectionException() throws Exception {
    List<Throwable> exceptions = new ArrayList<>();
    for (Class<? extends Throwable> clazz : ClientExceptionsUtil.getConnectionExceptionTypes()) {
      exceptions.add(create(clazz));
    }
    InetSocketAddress addr = InetSocketAddress.createUnresolved("127.0.0.1", 12345);
    for (Throwable exception : exceptions) {
      if (exception instanceof TimeoutException) {
        assertThat(IPCUtil.wrapException(addr, exception), instanceOf(TimeoutIOException.class));
      } else {
        assertThat(IPCUtil.wrapException(addr, exception), instanceOf(exception.getClass()));
      }
    }
  }
}
