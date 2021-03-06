package org.nutz.mvc.impl;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.nutz.Nutz;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.Ioc2;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.json.Json;
import org.nutz.json.JsonFormat;
import org.nutz.lang.Encoding;
import org.nutz.lang.Lang;
import org.nutz.lang.Mirror;
import org.nutz.lang.Stopwatch;
import org.nutz.lang.Strings;
import org.nutz.lang.util.Context;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.ActionChainMaker;
import org.nutz.mvc.ActionInfo;
import org.nutz.mvc.Loading;
import org.nutz.mvc.LoadingException;
import org.nutz.mvc.MessageLoader;
import org.nutz.mvc.Mvcs;
import org.nutz.mvc.NutConfig;
import org.nutz.mvc.SessionProvider;
import org.nutz.mvc.Setup;
import org.nutz.mvc.UrlMapping;
import org.nutz.mvc.ViewMaker;
import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.ChainBy;
import org.nutz.mvc.annotation.IocBy;
import org.nutz.mvc.annotation.Localization;
import org.nutz.mvc.annotation.SessionBy;
import org.nutz.mvc.annotation.SetupBy;
import org.nutz.mvc.annotation.UrlMappingBy;
import org.nutz.mvc.annotation.Views;
import org.nutz.mvc.view.DefaultViewMaker;

public class NutLoading implements Loading {

    private static final Log log = Logs.get();

    public UrlMapping load(NutConfig config) {
        if (log.isInfoEnabled()) {
            log.infof("Nutz Version : %s ", Nutz.version());
            log.infof("Nutz.Mvc[%s] is initializing ...", config.getAppName());
        }
        if (log.isDebugEnabled()) {
            Properties sys = System.getProperties();
            log.debug("Web Container Information:");
            log.debugf(" - Default Charset : %s", Encoding.defaultEncoding());
            log.debugf(" - Current . path  : %s", new File(".").getAbsolutePath());
            log.debugf(" - Java Version    : %s", sys.get("java.version"));
            log.debugf(" - File separator  : %s", sys.get("file.separator"));
            log.debugf(" - Timezone        : %s", sys.get("user.timezone"));
            log.debugf(" - OS              : %s %s", sys.get("os.name"), sys.get("os.arch"));
            log.debugf(" - ServerInfo      : %s", config.getServletContext().getServerInfo());
            log.debugf(" - Servlet API     : %d.%d",
                       config.getServletContext().getMajorVersion(),
                       config.getServletContext().getMinorVersion());
            if (config.getServletContext().getMajorVersion() > 2
                || config.getServletContext().getMinorVersion() > 4)
                log.debugf(" - ContextPath     : %s", config.getServletContext().getContextPath());
            log.debugf(" - context.tempdir : %s", config.getAttribute("javax.servlet.context.tempdir"));
            log.debugf(" - MainModule      : %s", config.getMainModule().getName());
        }
        /*
         * ???????????????
         */
        UrlMapping mapping;
        Ioc ioc = null;

        /*
         * ????????????
         */
        Stopwatch sw = Stopwatch.begin();

        try {

            /*
             * ??????????????????????????????????????????????????????????????? MainModule ???
             */
            Class<?> mainModule = config.getMainModule();

            /*
             * ???????????????
             */
            createContext(config);

            /*
             * ?????? Ioc ???????????????????????????
             */
            ioc = createIoc(config, mainModule);

            /*
             * ??????UrlMapping
             */
            mapping = evalUrlMapping(config, mainModule, ioc);

            /*
             * ????????????????????????
             */
            evalLocalization(config, mainModule);

            // ?????????SessionProvider
            createSessionProvider(config, mainModule);

            /*
             * ????????????????????? Setup
             */
            evalSetup(config, mainModule);
        }
        catch (Exception e) {
            if (log.isErrorEnabled())
                log.error("Error happend during start serivce!", e);
            if (ioc != null) {
                log.error("try to depose ioc");
                try {
                    ioc.depose();
                }
                catch (Throwable e2) {
                    log.error("error when depose ioc", e);
                }
            }
            throw Lang.wrapThrow(e, LoadingException.class);
        }

        // ~ Done ^_^
        sw.stop();
        if (log.isInfoEnabled())
            log.infof("Nutz.Mvc[%s] is up in %sms", config.getAppName(), sw.getDuration());

        return mapping;

    }

    protected UrlMapping evalUrlMapping(NutConfig config, Class<?> mainModule, Ioc ioc)
            throws Exception {
        /*
         * @ TODO ????????????????????????????????????????????????????????????Loadings???????????????????????????,
         * ???????????????????????????,????????????????????????MVC??????????????????,???????????????UrlMapping?????????
         */

        /*
         * ?????? UrlMapping
         */
        UrlMapping mapping = createUrlMapping(config);
        if (log.isInfoEnabled())
            log.infof("Build URL mapping by %s ...", mapping.getClass().getName());

        /*
         * ??????????????????
         */
        ViewMaker[] makers = createViewMakers(mainModule, ioc);

        /*
         * ?????????????????????
         */
        ActionChainMaker maker = createChainMaker(config, mainModule);

        /*
         * ??????????????????????????????
         */
        ActionInfo mainInfo = Loadings.createInfo(mainModule);

        /*
         * ??????????????????????????????
         */
        // TODO ????????????Set???? ???List????????????????
        Set<Class<?>> modules = Loadings.scanModules(ioc, mainModule);

        if (modules.isEmpty()) {
            if (log.isWarnEnabled())
                log.warn("None module classes found!!!");
        }

        int atMethods = 0;
        /*
         * ????????????????????????
         */
        for (Class<?> module : modules) {
            ActionInfo moduleInfo = Loadings.createInfo(module).mergeWith(mainInfo);
            for (Method method : module.getMethods()) {
                /*
                 * public ??????????????? @At ????????????????????????????????????????????????Bridge???????????????????????????
                 */
                if (!Modifier.isPublic(method.getModifiers()) || method.isBridge()
                    || Mirror.getAnnotationDeep(method, At.class) == null
                    || method.getDeclaringClass() != module)
                    continue;
                // ??????????????????
                ActionInfo info = Loadings.createInfo(method).mergeWith(moduleInfo);
                info.setViewMakers(makers);
                mapping.add(maker, info, config);
                atMethods++;
            }

            // ??????pathMap
            if (null != moduleInfo.getPathMap()) {
                for (Entry<String, String> en : moduleInfo.getPathMap().entrySet()) {
                    config.getAtMap().add(en.getKey(), en.getValue());
                }
            }
        }

        if (atMethods == 0) {
            if (log.isWarnEnabled())
                log.warn("None @At found in any modules class!!");
        } else {
            log.infof("Found %d module methods", atMethods);
        }
        
        config.setUrlMapping(mapping);
        config.setActionChainMaker(maker);
        config.setViewMakers(makers);

        return mapping;
    }

    protected void createContext(NutConfig config) {
        // ?????????????????????????????????????????????????????????????????????
        // ??????????????? Filter ??? Adaptor ???????????? ${app.root} ???????????????
        Context context = Lang.context();
        String appRoot = config.getAppRoot();
        context.set("app.root", appRoot);

        if (log.isDebugEnabled()) {
            log.debugf(">> app.root = %s", appRoot);
        }

        // ??????????????????
        for (Entry<String, String> entry : System.getenv().entrySet())
            context.set("env." + entry.getKey(), entry.getValue());
        // ??????????????????
        for (Entry<Object, Object> entry : System.getProperties().entrySet())
            context.set("sys." + entry.getKey(), entry.getValue());

        if (log.isTraceEnabled()) {
            log.tracef(">>\nCONTEXT %s", Json.toJson(context, JsonFormat.nice()));
        }
        config.getServletContext().setAttribute(Loading.CONTEXT_NAME, context);
    }

    protected UrlMapping createUrlMapping(NutConfig config) throws Exception {
        UrlMappingBy umb = config.getMainModule().getAnnotation(UrlMappingBy.class);
        if (umb != null)
            return Loadings.evalObj(config, umb.value(), umb.args());
        return new UrlMappingImpl();
    }

    protected ActionChainMaker createChainMaker(NutConfig config, Class<?> mainModule) {
        ChainBy ann = mainModule.getAnnotation(ChainBy.class);
        ActionChainMaker maker = null == ann ? new NutActionChainMaker(new String[]{})
                                            : Loadings.evalObj(config, ann.type(), ann.args());
        if (log.isDebugEnabled())
            log.debugf("@ChainBy(%s)", maker.getClass().getName());
        return maker;
    }

    protected void evalSetup(NutConfig config, Class<?> mainModule) throws Exception {
        SetupBy sb = mainModule.getAnnotation(SetupBy.class);
        if (null != sb) {
            if (log.isInfoEnabled())
                log.info("Setup application...");
            Setup setup = Loadings.evalObj(config, sb.value(), sb.args());
            config.setAttributeIgnoreNull(Setup.class.getName(), setup);
            setup.init(config);
        } else if (config.getIoc() != null && config.getIoc().has(Setup.IOCNAME)) {
            String[] names = config.getIoc().getNames();
            Arrays.sort(names);
            boolean flag = true;
            for (String name : names) {
                if (name != null && name.startsWith(Setup.IOCNAME)) {
                    if (flag) {
                        flag = false;
                        if (log.isInfoEnabled())
                            log.info("Setup application...");
                    }
                    log.debug("load Setup from Ioc by name=" + Setup.IOCNAME);
                    Setup setup = config.getIoc().get(Setup.class, Setup.IOCNAME);
                    config.setAttributeIgnoreNull(Setup.class.getName(), setup);
                    setup.init(config);
                }
            }
        } else if (Setup.class.isAssignableFrom(mainModule)) { // MainModule??????????????????Setup??????????
        	Setup setup = (Setup)Mirror.me(mainModule).born();
        	config.setAttributeIgnoreNull(Setup.class.getName(), setup);
        	setup.init(config);
        }
    }

    protected void evalLocalization(NutConfig config, Class<?> mainModule) {
        Localization lc = mainModule.getAnnotation(Localization.class);
        if (null != lc) {
            if (log.isDebugEnabled())
                log.debugf("Localization: %s('%s') %s dft<%s>",
                           lc.type().getName(),
                           lc.value(),
                           Strings.isBlank(lc.beanName()) ? "" : "$ioc->" + lc.beanName(),
                           lc.defaultLocalizationKey());

            MessageLoader msgLoader = null;
            // ?????? Ioc ???????????? MessageLoader ...
            if (!Strings.isBlank(lc.beanName())) {
                msgLoader = config.getIoc().get(lc.type(), lc.beanName());
            }
            // ??????????????????
            else {
                msgLoader = Mirror.me(lc.type()).born();
            }
            // ????????????
            Map<String, Map<String, Object>> msgss = msgLoader.load(lc.value());

            // ???????????? Map
            Mvcs.setMessageSet(msgss);

            // ??????????????????????????? ...
            if (!Strings.isBlank(lc.defaultLocalizationKey()))
                Mvcs.setDefaultLocalizationKey(lc.defaultLocalizationKey());

        }
        // ??????????????????
        else if (log.isDebugEnabled()) {
            log.debug("@Localization not define");
        }
    }

    protected ViewMaker[] createViewMakers(Class<?> mainModule, Ioc ioc) throws Exception {
        Views vms = mainModule.getAnnotation(Views.class);
        List<ViewMaker> makers = new ArrayList<ViewMaker>();
        if (null != vms) {
            for (int i = 0; i < vms.value().length; i++) {
                if (vms.value()[i].getAnnotation(IocBean.class) != null && ioc != null) {
                    makers.add(ioc.get(vms.value()[i]));
                } else {
                    makers.add(Mirror.me(vms.value()[i]).born());
                }
            }
        } else {
            if (ioc != null) {
                String[] names = ioc.getNames();
                Arrays.sort(names);
                for (String name : ioc.getNames()) {
                    if (name != null && name.startsWith(ViewMaker.IOCNAME)) {
                        log.debug("add ViewMaker from Ioc by name=" + name);
                        makers.add(ioc.get(ViewMaker.class, name));
                    }
                }
            }
        }
        makers.add(new DefaultViewMaker());// ???????????????????????????

        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (ViewMaker maker : makers) {
                sb.append(maker.getClass().getSimpleName()).append(".class,");
            }
            sb.setLength(sb.length() - 1);
            log.debugf("@Views(%s)", sb);
        }

        return makers.toArray(new ViewMaker[makers.size()]);
    }

    protected Ioc createIoc(NutConfig config, Class<?> mainModule) throws Exception {
        IocBy ib = mainModule.getAnnotation(IocBy.class);
        if (null != ib) {
            if (log.isDebugEnabled())
                log.debugf("@IocBy(type=%s, args=%s,init=%s)",
                           ib.type().getName(),
                           Json.toJson(ib.args()),
                           Json.toJson(ib.init()));

            Ioc ioc = Mirror.me(ib.type()).born().create(config, ib.args());
            // ????????? Ioc2 ???????????????????????? ValueMaker
            if (ioc instanceof Ioc2) {
                ((Ioc2) ioc).addValueProxyMaker(new ServletValueProxyMaker(config.getServletContext()));
            }

            // ??????????????? Ioc ??????????????????????????????
            for (String objName : ib.init()) {
                ioc.get(null, objName);
            }

            // ?????? Ioc ??????
            Mvcs.setIoc(ioc);
            return ioc;
        } else if (log.isInfoEnabled())
            log.info("!!!Your application without @IocBy supporting");
        return null;
    }

    @SuppressWarnings({"all"})
    protected void createSessionProvider(NutConfig config, Class<?> mainModule) throws Exception {
        SessionBy sb = mainModule.getAnnotation(SessionBy.class);
        if (sb != null) {
            SessionProvider sp = null;
            if (sb.args() != null && sb.args().length == 1 && sb.args()[0].startsWith("ioc:"))
                sp = config.getIoc().get(sb.value(), sb.args()[0].substring(4));
            else
                sp = Mirror.me(sb.value()).born((Object[])sb.args());
            if (log.isInfoEnabled())
                log.info("SessionBy --> " + sp);
            config.setSessionProvider(sp);
        }
    }

    public void depose(NutConfig config) {
        if (log.isInfoEnabled())
            log.infof("Nutz.Mvc[%s] is deposing ...", config.getAppName());
        Stopwatch sw = Stopwatch.begin();

        // Firstly, upload the user customized desctroy
        try {
            Setup setup = config.getAttributeAs(Setup.class, Setup.class.getName());
            if (null != setup)
                setup.destroy(config);
        }
        catch (Exception e) {
            throw new LoadingException(e);
        }
        finally {
            SessionProvider sp = config.getSessionProvider();
            if (sp != null)
                sp.notifyStop();
            // If the application has Ioc, depose it
            Ioc ioc = config.getIoc();
            if (null != ioc)
                ioc.depose();
        }

        // Done, print info
        sw.stop();
        if (log.isInfoEnabled())
            log.infof("Nutz.Mvc[%s] is down in %sms", config.getAppName(), sw.getDuration());
    }

}
