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

package org.apache.shardingsphere.sql.parser.sql.segment.dml;

import lombok.Getter;
import lombok.Setter;
import org.apache.shardingsphere.sql.parser.sql.segment.SQLSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.table.SimpleTableSegment;

import java.util.Collection;
import java.util.LinkedList;

@Getter
@Setter
public final class JoinedTableSegment implements SQLSegment {
    
    private int startIndex;
    
    private int stopIndex;
    
    private TableFactorSegment tableFactor;
    
    private JoinSpecificationSegment joinSpecification;
    
    /**
     * get table.
     * @return tableSegment.
     */
    public Collection<SimpleTableSegment> getSimpleTableSegments() {
        Collection<SimpleTableSegment> tables = new LinkedList<>();
        if (null != tableFactor) {
            tables.addAll(tableFactor.getSimpleTableSegments());
        }
        if (null != joinSpecification) {
            tables.addAll(joinSpecification.getSimpleTableSegments());
        }
        return tables;
    }
}
