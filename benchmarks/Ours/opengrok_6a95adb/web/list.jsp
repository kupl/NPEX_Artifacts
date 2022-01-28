<%--
$Id$

CDDL HEADER START

The contents of this file are subject to the terms of the
Common Development and Distribution License (the "License").
You may not use this file except in compliance with the License.

See LICENSE.txt included in this distribution for the specific
language governing permissions and limitations under the License.

When distributing Covered Code, include this CDDL HEADER in each
file and include the License file at LICENSE.txt.
If applicable, add the following below this CDDL HEADER, with the
fields enclosed by brackets "[]" replaced with your own identifying
information: Portions Copyright [yyyy] [name of copyright owner]

CDDL HEADER END

Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.

Portions Copyright 2011 Jens Elkner.

--%>
<%@page errorPage="error.jsp" import="
java.io.BufferedInputStream,
java.io.BufferedReader,
java.io.FileInputStream,
java.io.FileReader,
java.io.InputStream,
java.io.InputStreamReader,
java.io.Reader,
java.net.URLEncoder,
java.util.ArrayList,
java.util.Arrays,
java.util.List,
java.util.Set,
java.util.logging.Level,
java.util.zip.GZIPInputStream,
javax.servlet.http.HttpServletResponse,

org.opensolaris.opengrok.analysis.AnalyzerGuru,
org.opensolaris.opengrok.analysis.Definitions,
org.opensolaris.opengrok.analysis.FileAnalyzer.Genre,
org.opensolaris.opengrok.analysis.FileAnalyzerFactory,
org.opensolaris.opengrok.history.Annotation,
org.opensolaris.opengrok.index.IndexDatabase,
org.opensolaris.opengrok.util.IOUtils,
org.opensolaris.opengrok.web.DirectoryListing"
%><%
{
    // need to set it here since requesting parameters
    if (request.getCharacterEncoding() == null) {
        request.setCharacterEncoding("UTF-8");
    }

    PageConfig cfg = PageConfig.get(request);
    cfg.checkSourceRootExistence();

    Annotation annotation = cfg.getAnnotation();
    if (annotation != null) {
        int r = annotation.getWidestRevision();
        int a = annotation.getWidestAuthor();
        cfg.addHeaderData("<style type=\"text/css\">"
            + ".blame .r { width: " + (r == 0 ? 6 : Math.ceil(r * 0.7)) + "em; } "
            + ".blame .a { width: " + (a == 0 ? 6 : Math.ceil(a * 0.7)) + "em; } "
            + "</style>");
    }
}
%><%@include

file="mast.jsp"

%><script type="text/javascript">/* <![CDATA[ */
document.pageReady.push(function() { pageReadyList();});
/* ]]> */</script>
<%
/* ---------------------- list.jsp start --------------------- */
{
    PageConfig cfg = PageConfig.get(request);
    String rev = cfg.getRequestedRevision();

    String navigateWindowEnabled = cfg.getProject() != null
                ? Boolean.toString(cfg.getProject().isNavigateWindowEnabled())
                : "false";
    File resourceFile = cfg.getResourceFile();
    String path = cfg.getPath();
    String basename = resourceFile.getName();
    String rawPath = request.getContextPath() + Prefix.DOWNLOAD_P + path;
    Reader r = null;
    if (cfg.isDir()) {
        // valid resource is requested
        // mast.jsp assures, that resourceFile is valid and not /
        // see cfg.resourceNotAvailable()
        Project activeProject = Project.getProject(resourceFile);
        String cookieValue = cfg.getRequestedProjectsAsString();
        if (activeProject != null) {
            Set<String>  projects = cfg.getRequestedProjects();
            if (!projects.contains(activeProject.getName())) {
                projects.add(activeProject.getName());
                // update cookie
                cookieValue = cookieValue.length() == 0
                    ? activeProject.getName()
                    : activeProject.getName() + ',' + cookieValue;
                Cookie cookie = new Cookie(PageConfig.OPEN_GROK_PROJECT, URLEncoder.encode(cookieValue, "utf-8"));
                // TODO hmmm, projects.jspf doesn't set a path
                cookie.setPath(request.getContextPath() + '/');
                response.addCookie(cookie);
            }
        }
        // requesting a directory listing
        DirectoryListing dl = new DirectoryListing(cfg.getEftarReader());
        List<String> files = cfg.getResourceFileList();
        if (!files.isEmpty()) {
            List<String> readMes = dl.listTo(
                    Util.URIEncodePath(request.getContextPath()),
                    resourceFile, out, path, files);
            File[] catfiles = cfg.findDataFiles(readMes);
            for (int i=0; i < catfiles.length; i++) {
                if (catfiles[i] == null) {
                    continue;
                }
%>
<%
    if (readMes.get(i).toLowerCase().endsWith(".md")) {
    %><div id="src<%=i%>" data-markdown>
        <div class="markdown-heading">
            <h3><%= readMes.get(i) %></h3>
        </div>
        <div class="markdown-content"
             data-markdown-download="<%= request.getContextPath() + Prefix.DOWNLOAD_P + Util.URIEncodePath(cfg.getPath() + readMes.get(i)) %>">
        </div>
        <pre data-markdown-original><%
            Util.dump(out, catfiles[i], catfiles[i].getName().endsWith(".gz"));
        %></pre>
    </div>
<% } else { %>
    <h3><%= readMes.get(i) %></h3>
    <div id="src<%=i%>">
        <pre><%
            Util.dump(out, catfiles[i], catfiles[i].getName().endsWith(".gz"));
        %></pre>
    </div>
<%
    }

            }
        }
    } else if (cfg.annotate()) {
            // annotate
            BufferedInputStream bin =
                new BufferedInputStream(new FileInputStream(resourceFile));
            try {
                FileAnalyzerFactory a = AnalyzerGuru.find(basename);
                Genre g = AnalyzerGuru.getGenre(a);
                if (g == null) {
                    a = AnalyzerGuru.find(bin);
                    g = AnalyzerGuru.getGenre(a);
                }
                if (g == Genre.IMAGE) {
%>
<div id="src">
    <img src="<%= rawPath %>"/>
</div><%
                } else if ( g == Genre.HTML) {
                    r = new InputStreamReader(bin);
                    Util.dump(out, r);
                } else if (g == Genre.PLAIN) {
%>
<div id="src" data-navigate-window-enabled="<%= navigateWindowEnabled %>">
    <pre><%
                    // We're generating xref for the latest revision, so we can
                    // find the definitions in the index.
                    Definitions defs = IndexDatabase.getDefinitions(resourceFile);
                    Annotation annotation = cfg.getAnnotation();
                    r = IOUtils.createBOMStrippedReader(bin);
                    AnalyzerGuru.writeXref(a, r, out, defs, annotation,
                        Project.getProject(resourceFile));
    %></pre>
</div><%
                } else {
%>
Click <a href="<%= rawPath %>">download <%= basename %></a><%
                }
            } finally {
                if (r != null) {
                    try { r.close(); bin = null; }
                    catch (Exception e) { /* ignore */ }
                }
                if (bin != null) {
                    try { bin.close(); }
                    catch (Exception e) { /* ignore */ }
                }
            }
    } else if (rev.length() != 0) {
        // requesting a revision
        if (cfg.isLatestRevision(rev)) {
            File xrefFile = cfg.findDataFile();
            if (xrefFile != null) {
%>
<div id="src" data-navigate-window-enabled="<%= navigateWindowEnabled %>">
    <pre><%
                Util.dump(out, xrefFile, xrefFile.getName().endsWith(".gz"));
    %></pre>
</div><%
            }
        } else {
            // requesting a previous revision
            FileAnalyzerFactory a = AnalyzerGuru.find(basename);
            Genre g = AnalyzerGuru.getGenre(a);
            String error = null;
            if (g == Genre.PLAIN|| g == Genre.HTML || g == null) {
                InputStream in = null;
                try {
                    in = HistoryGuru.getInstance()
                        .getRevision(resourceFile.getParent(), basename, rev);
                } catch (Exception e) {
                    // fall through to error message
                    error = e.getMessage();
                }
                if (in != null) {
                    try {
                        if (g == null) {
                            a = AnalyzerGuru.find(in);
                            g = AnalyzerGuru.getGenre(a);
                        }
                        if (g == Genre.DATA || g == Genre.XREFABLE
                            || g == null)
                        {
    %>
    <div id="src">
    Binary file [Click <a href="<%= rawPath %>?r=<%= Util.URIEncode(rev) %>">here</a> to download]
    </div><%
                        } else {
    %>
    <div id="src">
        <pre><%
                            if (g == Genre.PLAIN) {
                                // We don't have any way to get definitions
                                // for old revisions currently.
                                Definitions defs = null;
                                Annotation annotation = cfg.getAnnotation();
                                //not needed yet
                                //annotation.writeTooltipMap(out);
                                r = IOUtils.createBOMStrippedReader(in);
                                AnalyzerGuru.writeXref(a, r, out, defs,
                                    annotation, Project.getProject(resourceFile));
                            } else if (g == Genre.IMAGE) {
        %></pre>
        <img src="<%= rawPath %>?r=<%= Util.URIEncode(rev) %>"/>
        <pre><%
                            } else if (g == Genre.HTML) {
                                r = new InputStreamReader(in);
                                Util.dump(out, r);
                            } else {
        %> Click <a href="<%= rawPath %>?r=<%= Util.URIEncode(rev) %>">download <%= basename %></a><%
                            }
                        }
                    } catch (IOException e) {
                        error = e.getMessage();
                    } finally {
                        if (r != null) {
                            try { r.close(); in = null;}
                            catch (Exception e) { /* ignore */ }
                        }
                        if (in != null) {
                            try { in.close(); }
                            catch (Exception e) { /* ignore */ }
                        }
                    }
        %></pre>
    </div><%
                } else {
    %>
    <h3 class="error">Error reading file</h3><%
                    if (error != null) {
    %>
    <p class="error"><%= error %></p><%
                    }
                }
            } else if (g == Genre.IMAGE) {
    %>
    <div id="src">
        <img src="<%= rawPath %>?r=<%= Util.URIEncode(rev) %>"/>
    </div><%
            } else {
    %>
    <div id="src">
    Binary file [Click <a href="<%= rawPath %>?r=<%= Util.URIEncode(rev) %>">here</a> to download]
    </div><%
            }
        }
    } else {
        // requesting cross referenced file
        File xrefFile = null;

        // Get the latest revision and redirect so that the revision number
        // appears in the URL.
        String location = cfg.getLatestRevisionLocation();
        if (location != null) {
            response.sendRedirect(location);
            return;
        } else {
            xrefFile = cfg.findDataFile();
        }

        if (xrefFile != null) {
%>
<div id="src" data-navigate-window-enabled="<%= navigateWindowEnabled %>">
    <pre><%
            Util.dump(out, xrefFile, xrefFile.getName().endsWith(".gz"));
    %></pre>
</div><%
        }
    }
}
/* ---------------------- list.jsp end --------------------- */
%><%@

include file="foot.jspf"

%>
