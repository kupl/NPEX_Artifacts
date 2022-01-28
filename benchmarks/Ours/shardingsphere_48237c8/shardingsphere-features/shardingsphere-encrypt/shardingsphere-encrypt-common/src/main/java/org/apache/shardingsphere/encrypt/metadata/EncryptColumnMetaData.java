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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.shardingsphere.sql.parser.binder.metadata.column.ColumnMetaData;

/**
 * Column meta data for encrypt.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class EncryptColumnMetaData extends ColumnMetaData {
    
    private final String cipherColumnName;
    
    private final String plainColumnName;
    
    private final String assistedQueryColumnName;
    
    public EncryptColumnMetaData(final String name, final int dataType, final String dataTypeName, final boolean primaryKey,
                                 final String cipherColumnName, final String plainColumnName, final String assistedQueryColumnName) {
        super(name, dataType, dataTypeName, primaryKey, false, false);
        this.cipherColumnName = cipherColumnName;
        this.plainColumnName = plainColumnName;
        this.assistedQueryColumnName = assistedQueryColumnName;
    }
}
