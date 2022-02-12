/**
 * The MIT License
 *
 *  Copyright (c) 2017, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.jeasy.rules.mvel;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
class MVELRuleDefinitionReader {

    private Yaml yaml = new Yaml();

    MVELRuleDefinition read(File descriptor) throws FileNotFoundException {
        return read(new FileReader(descriptor));
    }

    MVELRuleDefinition read(String descriptor) {
        return read(new StringReader(descriptor));
    }

    private MVELRuleDefinition read(Reader reader) {
        Object object = yaml.load(reader);
        Map<String, Object> map = (Map<String, Object>) object;
        return createRuleDefinitionFrom(map);
    }

private static org.jeasy.rules.mvel.MVELRuleDefinition createRuleDefinitionFrom(java.util.Map<java.lang.String, java.lang.Object> map) {
    org.jeasy.rules.mvel.MVELRuleDefinition ruleDefinition = new org.jeasy.rules.mvel.MVELRuleDefinition();
    ruleDefinition.setName(((java.lang.String) (map.get("name"))));
    ruleDefinition.setDescription(((java.lang.String) (map.get("description"))));
    /* NPEX_PATCH_BEGINS */
    if (((java.lang.Integer) (map.get("priority"))) == null) {
        return null;
    }
    ruleDefinition.setPriority(((java.lang.Integer) (map.get("priority"))));
    ruleDefinition.setCondition(((java.lang.String) (map.get("condition"))));
    ruleDefinition.setActions(((java.util.List<java.lang.String>) (map.get("actions"))));
    return ruleDefinition;
}
}
