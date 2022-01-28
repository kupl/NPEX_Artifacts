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

package org.apache.shardingsphere.encrypt.metadata;

import org.apache.shardingsphere.encrypt.rule.EncryptRule;
import org.apache.shardingsphere.infra.metadata.schema.spi.RuleMetaDataDecorator;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.infra.spi.order.OrderedSPIRegistry;
import org.apache.shardingsphere.sql.parser.binder.metadata.column.ColumnMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.table.TableMetaData;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class EncryptMetaDataDecoratorTest {
    
    static {
        ShardingSphereServiceLoader.register(RuleMetaDataDecorator.class);
    }
    
    @Test
    public void assertDecorate() {
        EncryptRule rule = createEncryptRule();
        EncryptMetaDataDecorator decorator = (EncryptMetaDataDecorator) OrderedSPIRegistry.getRegisteredServices(Collections.singletonList(rule), RuleMetaDataDecorator.class).get(rule);
        TableMetaData actual = decorator.decorate("t_encrypt", createTableMetaData(), rule);
        assertThat(actual.getColumns().size(), is(2));
        assertTrue(actual.getColumns().containsKey("id"));
        assertTrue(actual.getColumns().containsKey("pwd"));
    }
    
    private EncryptRule createEncryptRule() {
        EncryptRule result = mock(EncryptRule.class);
        when(result.getLogicColumnOfCipher("t_encrypt", "pwd_cipher")).thenReturn("pwd");
        when(result.isCipherColumn("t_encrypt", "pwd_cipher")).thenReturn(true);
        when(result.getAssistedQueryAndPlainColumns("t_encrypt")).thenReturn(Collections.singletonList("pwd_plain"));
        return result;
    }
    
    private TableMetaData createTableMetaData() {
        Collection<ColumnMetaData> columns = Arrays.asList(new ColumnMetaData("id", 1, "int", true, true, true), 
                new ColumnMetaData("pwd_cipher", 2, "varchar", false, false, true), new ColumnMetaData("pwd_plain", 2, "varchar", false, false, true));
        return new TableMetaData(columns, Collections.emptyList());
    }
}
