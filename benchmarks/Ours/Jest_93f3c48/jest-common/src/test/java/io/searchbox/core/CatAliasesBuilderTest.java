package io.searchbox.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Bartosz Polnik
 */
public class CatAliasesBuilderTest {
    @Test
    public void shouldSetApplicationJsonHeader() {
        Cat cat = new Cat.AliasesBuilder().build();
        assertEquals("application/json", cat.getHeader("content-type"));
    }

    @Test
    public void shouldGenerateValidUriWhenIndexNotGiven() {
        Cat cat = new Cat.AliasesBuilder().build();
        assertEquals("_cat/aliases/_all", cat.getURI());
    }

    @Test
    public void shouldGenerateValidUriWhenIndexGiven() {
        Cat cat = new Cat.AliasesBuilder().addIndex("testIndex").build();
        assertEquals("_cat/aliases/testIndex", cat.getURI());
    }

    @Test
    public void shouldGenerateValidUriWhenParameterGiven() {
        Cat cat = new Cat.AliasesBuilder().setParameter("v", "true").build();
        assertEquals("_cat/aliases/_all?v=true", cat.getURI());
    }
}
