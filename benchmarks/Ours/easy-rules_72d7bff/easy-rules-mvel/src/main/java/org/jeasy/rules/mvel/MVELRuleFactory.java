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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

/**
 * Factory to create {@link MVELRule} instances.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class MVELRuleFactory {

    private static MVELRuleDefinitionReader reader = new MVELRuleDefinitionReader();

    /**
     * Create a new {@link MVELRule} from a rule descriptor.
     *
     * @param ruleDescriptor in yaml format
     * @return a new rule
     * @throws FileNotFoundException if the rule descriptor cannot be found
     */
    public static MVELRule createRuleFrom(File ruleDescriptor) throws FileNotFoundException {
        MVELRuleDefinition ruleDefinition = reader.read(ruleDescriptor);
        return ruleDefinition.create();
    }

    /**
     * Create a new {@link MVELRule} from a Reader.
     *
     * @param ruleDescriptorReader as a Reader
     * @return a new rule
     * @throws IOException if the I/O operation failed
     */
    public static MVELRule createRuleFrom(Reader ruleDescriptorReader) throws IOException {
        StringBuilder ruleDescriptor = new StringBuilder();

        int charValue;
        while ((charValue = ruleDescriptorReader.read()) != -1) {
            ruleDescriptor.append((char) charValue);
        }
        ruleDescriptorReader.close();

        MVELRuleDefinition ruleDefinition = reader.read(ruleDescriptor.toString());
        return ruleDefinition.create();
    }
}
