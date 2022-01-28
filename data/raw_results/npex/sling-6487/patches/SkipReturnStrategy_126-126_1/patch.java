/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.scripting.thymeleaf.internal;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.api.resource.ScriptingResourceResolverProvider;
import org.apache.sling.scripting.thymeleaf.TemplateModeProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.templateresource.ITemplateResource;

@Component(
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Sling Resource TemplateResolver for Sling Scripting Thymeleaf",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = SlingResourceTemplateResolverConfiguration.class
)
public class SlingResourceTemplateResolver implements ITemplateResolver {

    @Reference(
        cardinality = ReferenceCardinality.MANDATORY,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
        bind = "setTemplateModeProvider",
        unbind = "unsetTemplateModeProvider"
    )
    private volatile TemplateModeProvider templateModeProvider;

    @Reference(
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ScriptingResourceResolverProvider scriptingResourceResolverProvider;

    private SlingResourceTemplateResolverConfiguration configuration;

//    private final Logger logger = LoggerFactory.getLogger(SlingResourceTemplateResolver.class);

    public SlingResourceTemplateResolver() {
    }

    public void setTemplateModeProvider(final TemplateModeProvider templateModeProvider) {
//        logger.debug("setting template mode provider: {}", templateModeProvider);
    }

    public void unsetTemplateModeProvider(final TemplateModeProvider templateModeProvider) {
//        logger.debug("unsetting template mode provider: {}", templateModeProvider);
    }

    @Activate
    private void activate(final SlingResourceTemplateResolverConfiguration configuration) {
//        logger.debug("activating");
        this.configuration = configuration;
    }

    @Modified
    private void modified(final SlingResourceTemplateResolverConfiguration configuration) {
//        logger.debug("modifying");
        this.configuration = configuration;
    }

    @Deactivate
    private void deactivate() {
//        logger.debug("deactivating");
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public Integer getOrder() {
        return configuration.order();
    }

    public TemplateMode fakeProvideTemplateMode() {
    	return null;
    }
    @Override
public org.thymeleaf.templateresolver.TemplateResolution resolveTemplate(final org.thymeleaf.IEngineConfiguration engineConfiguration, final java.lang.String ownerTemplate, final java.lang.String template, final java.util.Map<java.lang.String, java.lang.Object> templateResolutionAttributes) {
    // logger.debug("resolving template '{}'", template);
    final org.apache.sling.api.resource.Resource resource = null;
    final org.thymeleaf.templateresource.ITemplateResource templateResource = new org.apache.sling.scripting.thymeleaf.internal.SlingTemplateResource(resource);
    final boolean templateResourceExistenceVerified = false;
    final org.thymeleaf.templatemode.TemplateMode templateMode = fakeProvideTemplateMode();// templateModeProvider.provideTemplateMode(resource);

    /* NPEX_PATCH_BEGINS */
    if (templateMode == null) {
        return null;
    }
    // logger.debug("using template mode {} for template '{}'", templateMode, template);
    final boolean useDecoupledLogic = templateMode.isMarkup() && configuration.useDecoupledLogic();
    final org.thymeleaf.cache.ICacheEntryValidity validity = org.thymeleaf.cache.NonCacheableCacheEntryValidity.INSTANCE;
    return new org.thymeleaf.templateresolver.TemplateResolution(templateResource, templateResourceExistenceVerified, templateMode, useDecoupledLogic, validity);
}

}
