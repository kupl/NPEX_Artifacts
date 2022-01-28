/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.api.ldap.model.schema.registries.helper;


import java.util.List;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.DitStructureRule;
import org.apache.directory.api.ldap.model.schema.registries.Registries;


/**
 * An helper class used to store all the methods associated with an DitStructureRule
 * in relation with the Registries and SchemaManager.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class DitStructureRuleHelper
{
    private DitStructureRuleHelper()
    {
    }


    /**
     * Inject the DitContentRule into the registries, updating the references to
     * other SchemaObject
     *
     * @param ditStructureRule The DitStructureRule to add to the Registries
     * @param errors The errors we got while adding the DitContentRule to the Registries
     * @param registries The Registries
     * @throws LdapException If the addition failed
     */
    public static void addToRegistries( DitStructureRule ditStructureRule, List<Throwable> errors, Registries registries )
        throws LdapException
    {
        if ( registries != null )
        {
            try
            {
                ditStructureRule.unlock();

                // NOT YET IMPLEMENTED
            }
            finally
            {
                ditStructureRule.lock();
            }
        }
    }
}
