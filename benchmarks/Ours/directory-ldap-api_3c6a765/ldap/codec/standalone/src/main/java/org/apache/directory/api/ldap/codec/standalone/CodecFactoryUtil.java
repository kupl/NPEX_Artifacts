/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with apiService work for additional information
 *   regarding copyright ownership.  The ASF licenses apiService file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use apiService file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.api.ldap.codec.standalone;


import java.util.Map;

import org.apache.directory.api.ldap.codec.api.ControlFactory;
import org.apache.directory.api.ldap.codec.api.ExtendedOperationFactory;
import org.apache.directory.api.ldap.codec.api.IntermediateResponseFactory;
import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.codec.controls.cascade.CascadeFactory;
import org.apache.directory.api.ldap.codec.controls.manageDsaIT.ManageDsaITFactory;
import org.apache.directory.api.ldap.codec.controls.proxiedauthz.ProxiedAuthzFactory;
import org.apache.directory.api.ldap.codec.controls.search.entryChange.EntryChangeFactory;
import org.apache.directory.api.ldap.codec.controls.search.pagedSearch.PagedResultsFactory;
import org.apache.directory.api.ldap.codec.controls.search.persistentSearch.PersistentSearchFactory;
import org.apache.directory.api.ldap.codec.controls.search.subentries.SubentriesFactory;
import org.apache.directory.api.ldap.codec.controls.sort.SortRequestFactory;
import org.apache.directory.api.ldap.codec.controls.sort.SortResponseFactory;

import org.apache.directory.api.ldap.extras.controls.ad.AdShowDeleted;
import org.apache.directory.api.ldap.extras.controls.ad.AdPolicyHints;
import org.apache.directory.api.ldap.extras.controls.ad_impl.AdShowDeletedFactory;
import org.apache.directory.api.ldap.extras.controls.ad_impl.AdPolicyHintsFactory;
import org.apache.directory.api.ldap.extras.controls.ad.AdDirSync;
import org.apache.directory.api.ldap.extras.controls.ad_impl.AdDirSyncFactory;
import org.apache.directory.api.ldap.extras.controls.changeNotifications.ChangeNotifications;
import org.apache.directory.api.ldap.extras.controls.changeNotifications_impl.ChangeNotificationsFactory;
import org.apache.directory.api.ldap.extras.controls.permissiveModify.PermissiveModify;
import org.apache.directory.api.ldap.extras.controls.permissiveModify_impl.PermissiveModifyFactory;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicy;
import org.apache.directory.api.ldap.extras.controls.ppolicy_impl.PasswordPolicyFactory;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncDone.SyncDoneValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncRequest.SyncRequestValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncDoneValueFactory;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncRequestValueFactory;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncStateValueFactory;
import org.apache.directory.api.ldap.extras.controls.transaction.TransactionSpecification;
import org.apache.directory.api.ldap.extras.controls.transaction_impl.TransactionSpecificationFactory;
import org.apache.directory.api.ldap.extras.controls.vlv.VirtualListViewRequest;
import org.apache.directory.api.ldap.extras.controls.vlv.VirtualListViewResponse;
import org.apache.directory.api.ldap.extras.controls.vlv_impl.VirtualListViewRequestFactory;
import org.apache.directory.api.ldap.extras.controls.vlv_impl.VirtualListViewResponseFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.cancel.CancelFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.certGeneration.CertGenerationFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.endTransaction.EndTransactionFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.gracefulDisconnect.GracefulDisconnectFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.gracefulShutdown.GracefulShutdownFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.pwdModify.PasswordModifyFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.startTls.StartTlsFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.startTransaction.StartTransactionFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.storedProcedure.StoredProcedureFactory;
import org.apache.directory.api.ldap.extras.extended.ads_impl.whoAmI.WhoAmIFactory;
import org.apache.directory.api.ldap.extras.intermediate.syncrepl_impl.SyncInfoValueFactory;
import org.apache.directory.api.ldap.model.message.controls.Cascade;
import org.apache.directory.api.ldap.model.message.controls.EntryChange;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.api.ldap.model.message.controls.PagedResults;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearch;
import org.apache.directory.api.ldap.model.message.controls.ProxiedAuthz;
import org.apache.directory.api.ldap.model.message.controls.SortRequest;
import org.apache.directory.api.ldap.model.message.controls.SortResponse;
import org.apache.directory.api.ldap.model.message.controls.Subentries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A utility class for adding Codec and extended operation factories.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class CodecFactoryUtil
{
    private static final Logger LOG = LoggerFactory.getLogger( CodecFactoryUtil.class );


    private CodecFactoryUtil()
    {
    }


    /**
     * Loads the Controls implement out of the box in the codec.
     * 
     * @param controlFactories The Control factories to use
     * @param apiService The LDAP Service instance to use
     */
    public static void loadStockControls( Map<String, ControlFactory<?>> controlFactories, LdapApiService apiService )
    {
        // Standard controls
        ControlFactory<Cascade> cascadeFactory = new CascadeFactory( apiService );
        controlFactories.put( cascadeFactory.getOid(), cascadeFactory );
        LOG.info( "Registered pre-bundled control factory: {}", cascadeFactory.getOid() );

        ControlFactory<EntryChange> entryChangeFactory = new EntryChangeFactory( apiService );
        controlFactories.put( entryChangeFactory.getOid(), entryChangeFactory );
        LOG.info( "Registered pre-bundled control factory: {}", entryChangeFactory.getOid() );

        ControlFactory<ManageDsaIT> manageDsaITFactory = new ManageDsaITFactory( apiService );
        controlFactories.put( manageDsaITFactory.getOid(), manageDsaITFactory );
        LOG.info( "Registered pre-bundled control factory: {}", manageDsaITFactory.getOid() );

        ControlFactory<ProxiedAuthz> proxiedAuthzFactory = new ProxiedAuthzFactory( apiService );
        controlFactories.put( proxiedAuthzFactory.getOid(), proxiedAuthzFactory );
        LOG.info( "Registered pre-bundled control factory: {}", proxiedAuthzFactory.getOid() );

        ControlFactory<PagedResults> pagedResultsFactory = new PagedResultsFactory( apiService );
        controlFactories.put( pagedResultsFactory.getOid(), pagedResultsFactory );
        LOG.info( "Registered pre-bundled control factory: {}", pagedResultsFactory.getOid() );

        ControlFactory<PersistentSearch> persistentSearchFactory = new PersistentSearchFactory( apiService );
        controlFactories.put( persistentSearchFactory.getOid(), persistentSearchFactory );
        LOG.info( "Registered pre-bundled control factory: {}", persistentSearchFactory.getOid() );

        ControlFactory<Subentries> subentriesFactory = new SubentriesFactory( apiService );
        controlFactories.put( subentriesFactory.getOid(), subentriesFactory );
        LOG.info( "Registered pre-bundled control factory: {}", subentriesFactory.getOid() );
        
        ControlFactory<SortRequest> sortRequestFactory = new SortRequestFactory( apiService );
        controlFactories.put( sortRequestFactory.getOid(), sortRequestFactory );
        LOG.info( "Registered pre-bundled control factory: {}", sortRequestFactory.getOid() );

        ControlFactory<SortResponse> sortResponseFactory = new SortResponseFactory( apiService );
        controlFactories.put( sortResponseFactory.getOid(), sortResponseFactory );
        LOG.info( "Registered pre-bundled control factory: {}", sortResponseFactory.getOid() );

        // Extra controls
        ControlFactory<AdDirSync> adDirSyncFactory = new AdDirSyncFactory( apiService );
        controlFactories.put( adDirSyncFactory.getOid(), adDirSyncFactory );
        LOG.info( "Registered pre-bundled control factory: {}", adDirSyncFactory.getOid() );
        
        ControlFactory<AdShowDeleted> adShowDeletedFactory = new AdShowDeletedFactory( apiService );
        controlFactories.put( adShowDeletedFactory.getOid(), adShowDeletedFactory );
        LOG.info( "Registered pre-bundled control factory: {}", adShowDeletedFactory.getOid() );
        
        ControlFactory<AdPolicyHints> adPolicyHintsFactory = new AdPolicyHintsFactory( apiService );
        controlFactories.put( adPolicyHintsFactory.getOid(), adPolicyHintsFactory );
        LOG.info( "Registered pre-bundled control factory: {}", adPolicyHintsFactory.getOid() );

        ControlFactory<ChangeNotifications> changeNotificationsFactory = new ChangeNotificationsFactory( apiService );
        controlFactories.put( changeNotificationsFactory.getOid(), changeNotificationsFactory );
        LOG.info( "Registered pre-bundled control factory: {}", changeNotificationsFactory.getOid() );

        ControlFactory<PermissiveModify> permissiveModifyFactory = new PermissiveModifyFactory( apiService );
        controlFactories.put( permissiveModifyFactory.getOid(), permissiveModifyFactory );
        LOG.info( "Registered pre-bundled control factory: {}", permissiveModifyFactory.getOid() );

        ControlFactory<PasswordPolicy> passwordPolicyFactory = new PasswordPolicyFactory( apiService );
        controlFactories.put( passwordPolicyFactory.getOid(), passwordPolicyFactory );
        LOG.info( "Registered pre-bundled control factory: {}", passwordPolicyFactory.getOid() );

        ControlFactory<SyncDoneValue> syncDoneValueFactory = new SyncDoneValueFactory( apiService );
        controlFactories.put( syncDoneValueFactory.getOid(), syncDoneValueFactory );
        LOG.info( "Registered pre-bundled control factory: {}", syncDoneValueFactory.getOid() );

        ControlFactory<SyncRequestValue> syncRequestValueFactory = new SyncRequestValueFactory( apiService );
        controlFactories.put( syncRequestValueFactory.getOid(), syncRequestValueFactory );
        LOG.info( "Registered pre-bundled control factory: {}", syncRequestValueFactory.getOid() );

        ControlFactory<SyncStateValue> syncStateValueFactory = new SyncStateValueFactory( apiService );
        controlFactories.put( syncStateValueFactory.getOid(), syncStateValueFactory );
        LOG.info( "Registered pre-bundled control factory: {}", syncStateValueFactory.getOid() );

        ControlFactory<TransactionSpecification> transactionSpecificationFactory = new TransactionSpecificationFactory( apiService );
        controlFactories.put( transactionSpecificationFactory.getOid(), transactionSpecificationFactory );
        LOG.info( "Registered pre-bundled control factory: {}", transactionSpecificationFactory.getOid() );

        ControlFactory<VirtualListViewRequest> virtualListViewRequestFactory = new VirtualListViewRequestFactory(
            apiService );
        controlFactories.put( virtualListViewRequestFactory.getOid(), virtualListViewRequestFactory );
        LOG.info( "Registered pre-bundled control factory: {}", virtualListViewRequestFactory.getOid() );

        ControlFactory<VirtualListViewResponse> virtualListViewResponseFactory = new VirtualListViewResponseFactory(
            apiService );
        controlFactories.put( virtualListViewResponseFactory.getOid(), virtualListViewResponseFactory );
        LOG.info( "Registered pre-bundled control factory: {}", virtualListViewResponseFactory.getOid() );
    }


    /**
     * Load the standard extended operations :
     * <ul>
     * <li>cancel</li>
     * <li>certGeneration</li>
     * <li>gracefuShutdown</li>
     * <li>storedProcedure</li>
     * <li>gracefulDisconnect</li>
     * <li>passwordModify</li>
     * <li>whoAmI</li>
     * <li>startTls</li>
     * <li>startTransaction</li>
     * </ul>
     * 
     * @param extendendOperationsFactories The map of extended operation factories
     * @param apiService The LdapApiService to use
     */
    public static void loadStockExtendedOperations(
        Map<String, ExtendedOperationFactory> extendendOperationsFactories, LdapApiService apiService )
    {
        CancelFactory cancelFactory = new CancelFactory( apiService );
        extendendOperationsFactories.put( cancelFactory.getOid(), cancelFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", cancelFactory.getOid() );

        CertGenerationFactory certGenerationFactory = new CertGenerationFactory( apiService );
        extendendOperationsFactories.put( certGenerationFactory.getOid(), certGenerationFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", certGenerationFactory.getOid() );

        EndTransactionFactory endTransactionFactory = new EndTransactionFactory( apiService );
        extendendOperationsFactories.put( endTransactionFactory.getOid(), endTransactionFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", endTransactionFactory.getOid() );

        GracefulDisconnectFactory gracefulDisconnectFactory = new GracefulDisconnectFactory( apiService );
        extendendOperationsFactories.put( gracefulDisconnectFactory.getOid(), gracefulDisconnectFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", gracefulDisconnectFactory.getOid() );

        GracefulShutdownFactory gracefulShutdownFactory = new GracefulShutdownFactory( apiService );
        extendendOperationsFactories.put( gracefulShutdownFactory.getOid(), gracefulShutdownFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", gracefulShutdownFactory.getOid() );

        PasswordModifyFactory passwordModifyFactory = new PasswordModifyFactory( apiService );
        extendendOperationsFactories.put( passwordModifyFactory.getOid(), passwordModifyFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", passwordModifyFactory.getOid() );

        StartTlsFactory startTlsFactory = new StartTlsFactory( apiService );
        extendendOperationsFactories.put( startTlsFactory.getOid(), startTlsFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", startTlsFactory.getOid() );

        StartTransactionFactory startTransactionFactory = new StartTransactionFactory( apiService );
        extendendOperationsFactories.put( startTransactionFactory.getOid(), startTransactionFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", startTransactionFactory.getOid() );

        StoredProcedureFactory storedProcedureFactory = new StoredProcedureFactory( apiService );
        extendendOperationsFactories.put( storedProcedureFactory.getOid(), storedProcedureFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", storedProcedureFactory.getOid() );

        WhoAmIFactory whoAmIFactory = new WhoAmIFactory( apiService );
        extendendOperationsFactories.put( whoAmIFactory.getOid(), whoAmIFactory );
        LOG.info( "Registered pre-bundled extended operation factory: {}", whoAmIFactory.getOid() );
    }


    /**
     * Load the standard intermediate responses :
     * <ul>
     * <li>syncInfovalue</li>
     * </ul>
     * 
     * @param intermediateResponseFactories The map of intermediate response factories
     * @param apiService The LdapApiService to use
     */
    public static void loadStockIntermediateResponses(
        Map<String, IntermediateResponseFactory> intermediateResponseFactories, LdapApiService apiService )
    {
        SyncInfoValueFactory syncInfoValueFactory = new SyncInfoValueFactory( apiService );
        intermediateResponseFactories.put( syncInfoValueFactory.getOid(), syncInfoValueFactory );
        LOG.info( "Registered pre-bundled intermediate response factory: {}", syncInfoValueFactory.getOid() );
    }
}
