/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

 /*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.analysis.csharp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.lucene.document.Field;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.opensolaris.opengrok.analysis.AnalyzerGuru.string_ft_nstored_nanalyzed_norms;
import org.opensolaris.opengrok.analysis.Ctags;
import org.opensolaris.opengrok.analysis.FileAnalyzer;
import org.opensolaris.opengrok.analysis.Scopes;
import org.opensolaris.opengrok.analysis.Scopes.Scope;
import org.opensolaris.opengrok.analysis.StreamSource;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.search.QueryBuilder;
import org.opensolaris.opengrok.util.TestRepository;

/**
 *
 * @author Tomas Kotal
 */
public class CSharpAnalyzerFactoryTest {

    FileAnalyzer analyzer;
    private final String ctagsProperty = "org.opensolaris.opengrok.analysis.Ctags";
    private static Ctags ctags;
    private static TestRepository repository;

    public CSharpAnalyzerFactoryTest() {
        CSharpAnalyzerFactory analFact = new CSharpAnalyzerFactory();
        this.analyzer = analFact.getAnalyzer();
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        env.setCtags(System.getProperty(ctagsProperty, "ctags"));
        if (env.validateExuberantCtags()) {
            this.analyzer.setCtags(new Ctags());
        }
    }

    private static StreamSource getStreamSource(final String fname) {
        return new StreamSource() {
            @Override
            public InputStream getStream() throws IOException {
                return new FileInputStream(fname);
            }
        };
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        ctags = new Ctags();
        ctags.setBinary(RuntimeEnvironment.getInstance().getCtags());

        repository = new TestRepository();
        repository.create(CSharpAnalyzerFactoryTest.class.getResourceAsStream(
                "/org/opensolaris/opengrok/index/source.zip"));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ctags.close();
        ctags = null;
    }

    /**
     * Test of writeXref method, of class CSharpAnalyzerFactory.
     */
    @Test
    public void testScopeAnalyzer() throws Exception {
        String path = repository.getSourceRoot() + "/csharp/Sample.cs";
        File f = new File(path);
        if (!(f.canRead() && f.isFile())) {
            fail("csharp testfile " + f + " not found");
        }

        Document doc = new Document();
        doc.add(new Field(QueryBuilder.FULLPATH, path,
                string_ft_nstored_nanalyzed_norms));
        StringWriter xrefOut = new StringWriter();
        analyzer.setCtags(ctags);
        analyzer.setScopesEnabled(true);
        analyzer.analyze(doc, getStreamSource(path), xrefOut);

        IndexableField scopesField = doc.getField(QueryBuilder.SCOPES);
        assertNotNull(scopesField);
        Scopes scopes = Scopes.deserialize(scopesField.binaryValue().bytes);
        Scope globalScope = scopes.getScope(-1);
        assertEquals(4, scopes.size()); //TODO 5

        for (int i = 0; i < 41; ++i) {
            if (i >= 10 && i <= 10) {
                assertEquals("M1", scopes.getScope(i).getName());
                assertEquals("MyNamespace.TopClass", scopes.getScope(i).getNamespace());
            } else if (i >= 12 && i <= 14) {
                assertEquals("M2", scopes.getScope(i).getName());
                assertEquals("MyNamespace.TopClass", scopes.getScope(i).getNamespace());
            } else if (i >= 19 && i <= 25) {
                assertEquals("M3", scopes.getScope(i).getName());
                assertEquals("MyNamespace.TopClass", scopes.getScope(i).getNamespace());
//TODO add support for generic classes                
//            } else if (i >= 28 && i <= 30) { 
//                assertEquals("M4", scopes.getScope(i).name);
//                assertEquals("MyNamespace.TopClass", scopes.getScope(i).namespace);
            } else if (i >= 34 && i <= 36) {
                assertEquals("M5", scopes.getScope(i).getName());
                assertEquals("MyNamespace.TopClass.InnerClass", scopes.getScope(i).getNamespace());
            } else {
                assertEquals(scopes.getScope(i), globalScope);
                assertNull(scopes.getScope(i).getNamespace());
            }

        }
    }

}
