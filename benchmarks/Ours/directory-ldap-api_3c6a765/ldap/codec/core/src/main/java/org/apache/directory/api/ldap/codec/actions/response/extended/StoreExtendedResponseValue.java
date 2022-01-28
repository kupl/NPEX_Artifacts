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
package org.apache.directory.api.ldap.codec.actions.response.extended;


import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.ldap.codec.api.LdapMessageContainer;
import org.apache.directory.api.ldap.codec.decorators.ExtendedResponseDecorator;
import org.apache.directory.api.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store a Response to an ExtendedResponse
 * <pre>
 * ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
 *     ...
 *     response       [11] OCTET STRING OPTIONAL}
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreExtendedResponseValue extends GrammarAction<LdapMessageContainer<ExtendedResponseDecorator<?>>>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreExtendedResponseValue.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new response action.
     */
    public StoreExtendedResponseValue()
    {
        super( "Store response value" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( LdapMessageContainer<ExtendedResponseDecorator<?>> container ) throws DecoderException
    {
        // We can allocate the ExtendedResponse Object
        ExtendedResponseDecorator<?> extendedResponse = container.getMessage();

        // Get the Value and store it in the ExtendedResponse
        TLV tlv = container.getCurrentTLV();

        // We have to handle the special case of a 0 length matched
        // OID
        if ( tlv.getLength() == 0 )
        {
            extendedResponse.setResponseValue( Strings.EMPTY_BYTES );
        }
        else
        {
            extendedResponse.setResponseValue( tlv.getValue().getData() );
        }

        // We can have an END transition
        container.setGrammarEndAllowed( true );

        if ( IS_DEBUG )
        {
            LOG.debug( "Extended value : {}", extendedResponse.getResponseValue() );
        }
    }
}
