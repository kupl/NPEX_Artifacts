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

import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class MVELRuleFactoryTest {

    @Test
    public void testRuleCreation() throws Exception {
        // given
        File adultRuleDescriptor = new File("src/test/resources/adult-rule.yml");

        // when
        MVELRule adultRule = MVELRuleFactory.createRuleFrom(adultRuleDescriptor);

        // then
        assertThat(adultRule.getName()).isEqualTo("adult rule");
        assertThat(adultRule.getDescription()).isEqualTo("when age is greater then 18, then mark as adult");
        assertThat(adultRule.getPriority()).isEqualTo(1);
    }

    @Test
    public void testRuleCreationFromFileReader() throws Exception{
        // given
        Reader adultRuleDescriptorAsReader = new FileReader("src/test/resources/adult-rule.yml");

        // when
        MVELRule adultRule = MVELRuleFactory.createRuleFrom(adultRuleDescriptorAsReader);

        // then
        assertThat(adultRule.getName()).isEqualTo("adult rule");
        assertThat(adultRule.getDescription()).isEqualTo("when age is greater then 18, then mark as adult");
        assertThat(adultRule.getPriority()).isEqualTo(1);
    }

    @Test
    public void testRuleCreationFromStringReader() throws Exception{
        // given
        Reader adultRuleDescriptorAsReader = new StringReader(new String(Files.readAllBytes(Paths.get("src/test/resources/adult-rule.yml"))));

        // when
        MVELRule adultRule = MVELRuleFactory.createRuleFrom(adultRuleDescriptorAsReader);

        // then
        assertThat(adultRule.getName()).isEqualTo("adult rule");
        assertThat(adultRule.getDescription()).isEqualTo("when age is greater then 18, then mark as adult");
        assertThat(adultRule.getPriority()).isEqualTo(1);
    }
}