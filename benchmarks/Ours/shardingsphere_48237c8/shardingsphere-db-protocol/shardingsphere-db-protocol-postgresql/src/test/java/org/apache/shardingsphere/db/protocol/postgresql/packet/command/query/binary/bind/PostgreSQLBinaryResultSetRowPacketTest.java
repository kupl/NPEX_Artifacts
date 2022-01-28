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

package org.apache.shardingsphere.db.protocol.postgresql.packet.command.query.binary.bind;

import org.apache.shardingsphere.db.protocol.postgresql.constant.PostgreSQLColumnType;
import org.apache.shardingsphere.db.protocol.postgresql.packet.command.PostgreSQLCommandPacketType;
import org.apache.shardingsphere.db.protocol.postgresql.payload.PostgreSQLPacketPayload;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PostgreSQLBinaryResultSetRowPacketTest {
    
    @Mock
    private PostgreSQLPacketPayload payload;
    
    @Test
    public void assertWriteStringData() {
        PostgreSQLBinaryResultSetRowPacket rowPacket = new PostgreSQLBinaryResultSetRowPacket(Arrays.asList("value", "b"),
                Arrays.asList(PostgreSQLColumnType.POSTGRESQL_TYPE_VARCHAR, PostgreSQLColumnType.POSTGRESQL_TYPE_VARCHAR));
        assertThat(rowPacket.getData().size(), is(2));
        rowPacket.write(payload);
        verify(payload).writeInt2(2);
        verify(payload).writeInt4(5);
        verify(payload).writeStringEOF("value");
        verify(payload).writeInt4(1);
        verify(payload).writeStringEOF("b");
    }
    
    @Test
    public void assertWriteIntData() {
        PostgreSQLBinaryResultSetRowPacket rowPacket = new PostgreSQLBinaryResultSetRowPacket(Arrays.asList(10),
                Arrays.asList(PostgreSQLColumnType.POSTGRESQL_TYPE_INT4));
        assertThat(rowPacket.getData().size(), is(1));
        rowPacket.write(payload);
        verify(payload).writeInt2(1);
        verify(payload).writeInt4(4);
        verify(payload).writeInt4(10);
    }
    
    @Test
    public void assertGetMessageType() {
        PostgreSQLBinaryResultSetRowPacket rowPacket = new PostgreSQLBinaryResultSetRowPacket(null, null);
        assertThat(rowPacket.getMessageType(), is(PostgreSQLCommandPacketType.DATA_ROW.getValue()));
    }
    
}
