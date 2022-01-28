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

package org.apache.shardingsphere.infra.executor.sql.resourced.jdbc.executor;

import org.apache.shardingsphere.infra.executor.kernel.ExecutorKernel;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class SQLExecutorTest {
    
    @Test
    public void assertExecute() throws SQLException {
        ExecutorKernel kernel = mock(ExecutorKernel.class);
        when(kernel.execute(anyCollection(), any(), any(), anyBoolean())).thenReturn(Collections.singletonList("test"));
        SQLExecutor sqlExecutor = new SQLExecutor(kernel, false);
        List<?> actual1 = sqlExecutor.execute(Collections.emptyList(), null);
        assertThat(actual1, is(Collections.singletonList("test")));
        List<?> actual2 = sqlExecutor.execute(Collections.emptyList(), null, null);
        assertThat(actual2, is(Collections.singletonList("test")));
    }
    
    @Test
    public void assertExecuteSQLException() {
        try {
            ExecutorKernel kernel = mock(ExecutorKernel.class);
            when(kernel.execute(anyCollection(), any(), any(), anyBoolean())).thenThrow(new SQLException("TestSQLException"));
            SQLExecutor sqlExecutor = new SQLExecutor(kernel, false);
            sqlExecutor.execute(Collections.emptyList(), null);
        } catch (final SQLException ex) {
            assertThat(ex.getMessage(), is("TestSQLException"));
        }
    }
    
    @Test
    public void assertExecuteNotThrownSQLException() throws SQLException {
        ExecutorKernel kernel = mock(ExecutorKernel.class);
        when(kernel.execute(anyCollection(), any(), any(), anyBoolean())).thenThrow(new SQLException("TestSQLException"));
        SQLExecutor sqlExecutor = new SQLExecutor(kernel, false);
        ExecutorExceptionHandler.setExceptionThrown(false);
        List<?> actual = sqlExecutor.execute(Collections.emptyList(), null);
        assertThat(actual, is(Collections.emptyList()));
    }
}
