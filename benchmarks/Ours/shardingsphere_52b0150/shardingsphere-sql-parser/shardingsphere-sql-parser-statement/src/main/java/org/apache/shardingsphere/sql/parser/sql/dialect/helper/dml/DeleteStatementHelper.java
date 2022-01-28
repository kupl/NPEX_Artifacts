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

package org.apache.shardingsphere.sql.parser.sql.dialect.helper.dml;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.order.OrderBySegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.pagination.limit.LimitSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dml.DeleteStatement;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.mysql.MySQLStatement;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.mysql.dml.MySQLDeleteStatement;

import java.util.Optional;

/**
 * DeleteStatement helper class for different dialect SQLStatements.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DeleteStatementHelper {

    /**
     * Get OrderBySegment.
     *
     * @param deleteStatement DeleteStatement
     * @return OrderBySegment
     */
    public static Optional<OrderBySegment> getOrderBySegment(final DeleteStatement deleteStatement) {
        if (deleteStatement instanceof MySQLStatement) {
            return ((MySQLDeleteStatement) deleteStatement).getOrderBy();
        }
        return Optional.empty();
    }

    /**
     * Get LimitSegment.
     *
     * @param deleteStatement DeleteStatement
     * @return LimitSegment
     */
    public static Optional<LimitSegment> getLimitSegment(final DeleteStatement deleteStatement) {
        if (deleteStatement instanceof MySQLStatement) {
            return ((MySQLDeleteStatement) deleteStatement).getLimit();
        }
        return Optional.empty();
    }
}
