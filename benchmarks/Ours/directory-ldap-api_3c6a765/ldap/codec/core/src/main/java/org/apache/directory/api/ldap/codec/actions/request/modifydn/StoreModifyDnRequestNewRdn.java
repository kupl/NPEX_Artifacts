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
package org.apache.directory.api.ldap.codec.actions.request.modifydn;


import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.codec.api.LdapMessageContainer;
import org.apache.directory.api.ldap.codec.api.ResponseCarryingException;
import org.apache.directory.api.ldap.codec.decorators.ModifyDnRequestDecorator;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnResponseImpl;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the ModifyDnRequest new RDN
 * <pre>
 * ModifyDNRequest ::= [APPLICATION 12] SEQUENCE { ...
 *     ...
 *     newrdn  RelativeRDN,
 *     ...
 *
 * RelativeRDN :: LDAPString
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreModifyDnRequestNewRdn extends GrammarAction<LdapMessageContainer<ModifyDnRequestDecorator>>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreModifyDnRequestNewRdn.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new action.
     */
    public StoreModifyDnRequestNewRdn()
    {
        super( "Store ModifyDN request new RDN" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( LdapMessageContainer<ModifyDnRequestDecorator> container ) throws DecoderException
    {
        ModifyDnRequest modifyDnRequest = container.getMessage();

        // Get the Value and store it in the modifyDNRequest
        TLV tlv = container.getCurrentTLV();

        // We have to handle the special case of a 0 length matched
        // newDN
        Rdn newRdn;

        if ( tlv.getLength() == 0 )
        {
            String msg = I18n.err( I18n.ERR_04090 );
            LOG.error( msg );

            ModifyDnResponseImpl response = new ModifyDnResponseImpl( modifyDnRequest.getMessageId() );
            throw new ResponseCarryingException( msg, response, ResultCodeEnum.INVALID_DN_SYNTAX,
                modifyDnRequest.getName(), null );
        }
        else
        {
            byte[] dnBytes = tlv.getValue().getData();
            String dnStr = Strings.utf8ToString( dnBytes );

            try
            {
                Dn dn = new Dn( dnStr );
                newRdn = dn.getRdn( dn.size() - 1 );
            }
            catch ( LdapInvalidDnException ine )
            {
                String msg = "Invalid new Rdn given : " + dnStr + " (" + Strings.dumpBytes( dnBytes )
                    + ") is invalid";
                LOG.error( "{} : {}", msg, ine.getMessage() );

                ModifyDnResponseImpl response = new ModifyDnResponseImpl( modifyDnRequest.getMessageId() );
                throw new ResponseCarryingException( msg, response, ResultCodeEnum.INVALID_DN_SYNTAX,
                    modifyDnRequest.getName(), ine );
            }

            modifyDnRequest.setNewRdn( newRdn );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Modifying with new Rdn {}", newRdn );
        }
    }
}
