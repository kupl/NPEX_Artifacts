/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.hyphenation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.xml.sax.InputSource;

import org.apache.commons.io.IOUtils;

/**
 * This tree structure stores the hyphenation patterns in an efficient
 * way for fast lookup. It provides the provides the method to
 * hyphenate a word.
 *
 * This work was originally authored by Carlos Villegas cav@uniscope.co.jp
 */
public class HyphenationTree extends TernaryTree implements PatternConsumer {

    private static final long serialVersionUID = -7842107987915665573L;

    /**
     * value space: stores the interletter values
     */
    protected ByteVector vspace;

    /**
     * This map stores hyphenation exceptions
     */
    protected HashMap stoplist;

    /**
     * This map stores the character classes
     */
    protected TernaryTree classmap;

    /**
     * Temporary map to store interletter values on pattern loading.
     */
    private transient TernaryTree ivalues;

    /** Default constructor. */
    public HyphenationTree() {
        stoplist = new HashMap(23);    // usually a small table
        classmap = new TernaryTree();
        vspace = new ByteVector();
        vspace.alloc(1);    // this reserves index 0, which we don't use
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
    }

    /**
     * Packs the values by storing them in 4 bits, two values into a byte
     * Values range is from 0 to 9. We use zero as terminator,
     * so we'll add 1 to the value.
     * @param values a string of digits from '0' to '9' representing the
     * interletter values.
     * @return the index into the vspace array where the packed values
     * are stored.
     */
    protected int packValues(String values) {
        int i;
        int n = values.length();
        int m = (n & 1) == 1 ? (n >> 1) + 2 : (n >> 1) + 1;
        int offset = vspace.alloc(m);
        byte[] va = vspace.getArray();
        for (i = 0; i < n; i++) {
            int j = i >> 1;
            byte v = (byte)((values.charAt(i) - '0' + 1) & 0x0f);
            if ((i & 1) == 1) {
                va[j + offset] = (byte)(va[j + offset] | v);
            } else {
                va[j + offset] = (byte)(v << 4);    // big endian
            }
        }
        va[m - 1 + offset] = 0;    // terminator
        return offset;
    }

    /**
     * Unpack values.
     * @param k an integer
     * @return a string
     */
    protected String unpackValues(int k) {
        StringBuffer buf = new StringBuffer();
        byte v = vspace.get(k++);
        while (v != 0) {
            char c = (char)((v >>> 4) - 1 + '0');
            buf.append(c);
            c = (char)(v & 0x0f);
            if (c == 0) {
                break;
            }
            c = (char)(c - 1 + '0');
            buf.append(c);
            v = vspace.get(k++);
        }
        return buf.toString();
    }

    /**
     * Read hyphenation patterns from an XML file.
     * @param filename the filename
     * @throws HyphenationException In case the parsing fails
     */
    public void loadPatterns(String filename) throws HyphenationException {
        File f = new File(filename);
        try {
            InputSource src = new InputSource(f.toURI().toURL().toExternalForm());
            loadPatterns(src);
        } catch (MalformedURLException e) {
            throw new HyphenationException("Error converting the File '" + f + "' to a URL: "
                    + e.getMessage());
        }
    }

    /**
     * Read hyphenation patterns from an XML file.
     * @param source the InputSource for the file
     * @throws HyphenationException In case the parsing fails
     */
    public void loadPatterns(InputSource source) throws HyphenationException {
        PatternParser pp = new PatternParser(this);
        ivalues = new TernaryTree();

        pp.parse(source);

        // patterns/values should be now in the tree
        // let's optimize a bit
        trimToSize();
        vspace.trimToSize();
        classmap.trimToSize();

        // get rid of the auxiliary map
        ivalues = null;
    }

    /**
     * Find pattern.
     * @param pat a pattern
     * @return a string
     */
    public String findPattern(String pat) {
        int k = super.find(pat);
        if (k >= 0) {
            return unpackValues(k);
        }
        return "";
    }

    /**
     * String compare, returns 0 if equal or
     * t is a substring of s.
     * @param s first character array
     * @param si starting index into first array
     * @param t second character array
     * @param ti starting index into second array
     * @return an integer
     */
    protected int hstrcmp(char[] s, int si, char[] t, int ti) {
        for (; s[si] == t[ti]; si++, ti++) {
            if (s[si] == 0) {
                return 0;
            }
        }
        if (t[ti] == 0) {
            return 0;
        }
        return s[si] - t[ti];
    }

    /**
     * Get values.
     * @param k an integer
     * @return a byte array
     */
    protected byte[] getValues(int k) {
        StringBuffer buf = new StringBuffer();
        byte v = vspace.get(k++);
        while (v != 0) {
            char c = (char)((v >>> 4) - 1);
            buf.append(c);
            c = (char)(v & 0x0f);
            if (c == 0) {
                break;
            }
            c = (char)(c - 1);
            buf.append(c);
            v = vspace.get(k++);
        }
        byte[] res = new byte[buf.length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = (byte)buf.charAt(i);
        }
        return res;
    }

    /**
     * <p>Search for all possible partial matches of word starting
     * at index an update interletter values. In other words, it
     * does something like:</p>
     * <code>
     * for(i=0; i&lt;patterns.length; i++) {
     * if ( word.substring(index).startsWidth(patterns[i]) )
     * update_interletter_values(patterns[i]);
     * }
     * </code>
     * <p>But it is done in an efficient way since the patterns are
     * stored in a ternary tree. In fact, this is the whole purpose
     * of having the tree: doing this search without having to test
     * every single pattern. The number of patterns for languages
     * such as English range from 4000 to 10000. Thus, doing thousands
     * of string comparisons for each word to hyphenate would be
     * really slow without the tree. The tradeoff is memory, but
     * using a ternary tree instead of a trie, almost halves the
     * the memory used by Lout or TeX. It's also faster than using
     * a hash table</p>
     * @param word null terminated word to match
     * @param index start index from word
     * @param il interletter values array to update
     */
    protected void searchPatterns(char[] word, int index, byte[] il) {
        byte[] values;
        int i = index;
        char p;
        char q;
        char sp = word[i];
        p = root;

        while (p > 0 && p < sc.length) {
            if (sc[p] == 0xFFFF) {
                if (hstrcmp(word, i, kv.getArray(), lo[p]) == 0) {
                    values = getValues(eq[p]);    // data pointer is in eq[]
                    int j = index;
                    for (byte value : values) {
                        if (j < il.length && value > il[j]) {
                            il[j] = value;
                        }
                        j++;
                    }
                }
                return;
            }
            int d = sp - sc[p];
            if (d == 0) {
                if (sp == 0) {
                    break;
                }
                sp = word[++i];
                p = eq[p];
                q = p;

                // look for a pattern ending at this position by searching for
                // the null char ( splitchar == 0 )
                while (q > 0 && q < sc.length) {
                    if (sc[q] == 0xFFFF) {        // stop at compressed branch
                        break;
                    }
                    if (sc[q] == 0) {
                        values = getValues(eq[q]);
                        int j = index;
                        for (byte value : values) {
                            if (j < il.length && value > il[j]) {
                                il[j] = value;
                            }
                            j++;
                        }
                        break;
                    } else {
                        q = lo[q];

                        /**
                         * actually the code should be:
                         * q = sc[q] < 0 ? hi[q] : lo[q];
                         * but java chars are unsigned
                         */
                    }
                }
            } else {
                p = d < 0 ? lo[p] : hi[p];
            }
        }
    }

    /**
     * Hyphenate word and return a Hyphenation object.
     * @param word the word to be hyphenated
     * @param remainCharCount Minimum number of characters allowed
     * before the hyphenation point.
     * @param pushCharCount Minimum number of characters allowed after
     * the hyphenation point.
     * @return a {@link Hyphenation Hyphenation} object representing
     * the hyphenated word or null if word is not hyphenated.
     */
    public Hyphenation hyphenate(String word, int remainCharCount,
                                 int pushCharCount) {
        char[] w = word.toCharArray();
        if (isMultiPartWord(w, w.length)) {
            List<char[]> words = splitOnNonCharacters(w);
            return new Hyphenation(new String(w),
                    getHyphPointsForWords(words, remainCharCount, pushCharCount));
        } else {
            return hyphenate(w, 0, w.length, remainCharCount, pushCharCount);
        }
    }

    private boolean isMultiPartWord(char[] w, int len) {
        int wordParts = 0;
        for (int i = 0; i < len; i++) {
            char[] c = new char[2];
            c[0] = w[i];
            int nc = classmap.find(c, 0);
            if (nc > 0) {
                if (wordParts > 1) {
                    return true;
                }
                wordParts = 1;
            } else {
                if (wordParts == 1) {
                    wordParts++;
                }
            }
        }
        return false;
    }

    private List<char[]> splitOnNonCharacters(char[] word) {
        List<Integer> breakPoints = getNonLetterBreaks(word);
        if (breakPoints.size() == 0) {
            return Collections.emptyList();
        }
        List<char[]> words = new ArrayList<char[]>();
        for (int ibreak = 0; ibreak < breakPoints.size(); ibreak++) {
            char[] newWord = getWordFromCharArray(word, ((ibreak == 0)
                    ? 0 : breakPoints.get(ibreak - 1)), breakPoints.get(ibreak));
            words.add(newWord);
        }
        if (word.length - breakPoints.get(breakPoints.size() - 1) - 1 > 1) {
            char[] newWord = getWordFromCharArray(word, breakPoints.get(breakPoints.size() - 1),
                    word.length);
            words.add(newWord);
        }
        return words;
    }

    private List<Integer> getNonLetterBreaks(char[] word) {
        char[] c = new char[2];
        List<Integer> breakPoints = new ArrayList<Integer>();
        boolean foundLetter = false;
        for (int i = 0; i < word.length; i++) {
            c[0] = word[i];
            if (classmap.find(c, 0) < 0) {
                if (foundLetter) {
                    breakPoints.add(i);
                }
            } else {
                foundLetter = true;
            }
        }
        return breakPoints;
    }

    private char[] getWordFromCharArray(char[] word, int startIndex, int endIndex) {
        char[] newWord = new char[endIndex - ((startIndex == 0) ? startIndex : startIndex + 1)];
        int iChar = 0;
        for (int i = (startIndex == 0) ? 0 : startIndex + 1; i < endIndex; i++) {
            newWord[iChar++] = word[i];
        }
        return newWord;
    }

    private int[] getHyphPointsForWords(List<char[]> nonLetterWords, int remainCharCount,
            int pushCharCount) {
        int[] breaks = new int[0];
        for (int iNonLetterWord = 0; iNonLetterWord < nonLetterWords.size(); iNonLetterWord++) {
            char[] nonLetterWord = nonLetterWords.get(iNonLetterWord);
            Hyphenation curHyph = hyphenate(nonLetterWord, 0, nonLetterWord.length,
                    (iNonLetterWord == 0) ? remainCharCount : 1,
                    (iNonLetterWord == nonLetterWords.size() - 1) ? pushCharCount : 1);
            if (curHyph == null) {
                continue;
            }
            int[] combined = new int[breaks.length + curHyph.getHyphenationPoints().length];
            int[] hyphPoints = curHyph.getHyphenationPoints();
            int foreWordsSize = calcForeWordsSize(nonLetterWords, iNonLetterWord);
            for (int i = 0; i < hyphPoints.length; i++) {
                hyphPoints[i] += foreWordsSize;
            }
            System.arraycopy(breaks, 0, combined, 0, breaks.length);
            System.arraycopy(hyphPoints, 0, combined, breaks.length, hyphPoints.length);
            breaks = combined;
        }
        return breaks;
    }

    private int calcForeWordsSize(List<char[]> nonLetterWords, int iNonLetterWord) {
        int result = 0;
        for (int i = 0; i < iNonLetterWord; i++) {
            result += nonLetterWords.get(i).length + 1;
        }
        return result;
    }

    /**
     * w = "****nnllllllnnn*****",
     * where n is a non-letter, l is a letter,
     * all n may be absent, the first n is at offset,
     * the first l is at offset + iIgnoreAtBeginning;
     * word = ".llllll.'\0'***",
     * where all l in w are copied into word.
     * In the first part of the routine len = w.length,
     * in the second part of the routine len = word.length.
     * Three indices are used:
     * index(w), the index in w,
     * index(word), the index in word,
     * letterindex(word), the index in the letter part of word.
     * The following relations exist:
     * index(w) = offset + i - 1
     * index(word) = i - iIgnoreAtBeginning
     * letterindex(word) = index(word) - 1
     * (see first loop).
     * It follows that:
     * index(w) - index(word) = offset - 1 + iIgnoreAtBeginning
     * index(w) = letterindex(word) + offset + iIgnoreAtBeginning
     */

    /**
     * Hyphenate word and return an array of hyphenation points.
     * @param w char array that contains the word
     * @param offset Offset to first character in word
     * @param len Length of word
     * @param remainCharCount Minimum number of characters allowed
     * before the hyphenation point.
     * @param pushCharCount Minimum number of characters allowed after
     * the hyphenation point.
     * @return a {@link Hyphenation Hyphenation} object representing
     * the hyphenated word or null if word is not hyphenated.
     */
    public Hyphenation hyphenate(char[] w, int offset, int len,
                                 int remainCharCount, int pushCharCount) {
        int i;
        char[] word = new char[len + 3];

        // normalize word
        char[] c = new char[2];
        int iIgnoreAtBeginning = 0;
        int iLength = len;
        boolean bEndOfLetters = false;
        for (i = 1; i <= len; i++) {
            c[0] = w[offset + i - 1];
            int nc = classmap.find(c, 0);
            if (nc < 0) {    // found a non-letter character ...
                if (i == (1 + iIgnoreAtBeginning)) {
                    // ... before any letter character
                    iIgnoreAtBeginning++;
                } else {
                    // ... after a letter character
                    bEndOfLetters = true;
                }
                iLength--;
            } else {
                if (!bEndOfLetters) {
                    word[i - iIgnoreAtBeginning] = (char)nc;
                } else {
                    return null;
                }
            }
        }

        len = iLength;
        if (len < (remainCharCount + pushCharCount)) {
            // word is too short to be hyphenated
            return null;
        }
        int[] result = new int[len + 1];
        int k = 0;

        // check exception list first
        String sw = new String(word, 1, len);
        if (stoplist.containsKey(sw)) {
            // assume only simple hyphens (Hyphen.pre="-", Hyphen.post = Hyphen.no = null)
            ArrayList hw = (ArrayList)stoplist.get(sw);
            int j = 0;
            for (i = 0; i < hw.size(); i++) {
                Object o = hw.get(i);
                // j = index(sw) = letterindex(word)?
                // result[k] = corresponding index(w)
                if (o instanceof String) {
                    j += ((String)o).length();
                    if (j >= remainCharCount && j < (len - pushCharCount)) {
                        result[k++] = j + iIgnoreAtBeginning;
                    }
                }
            }
        } else {
            // use algorithm to get hyphenation points
            word[0] = '.';                    // word start marker
            word[len + 1] = '.';              // word end marker
            word[len + 2] = 0;                // null terminated
            byte[] il = new byte[len + 3];    // initialized to zero
            for (i = 0; i < len + 1; i++) {
                searchPatterns(word, i, il);
            }

            // hyphenation points are located where interletter value is odd
            // i is letterindex(word),
            // i + 1 is index(word),
            // result[k] = corresponding index(w)
            for (i = 0; i < len; i++) {
                if (((il[i + 1] & 1) == 1) && i >= remainCharCount
                        && i <= (len - pushCharCount)) {
                    result[k++] = i + iIgnoreAtBeginning;
                }
            }
        }


        if (k > 0) {
            // trim result array
            int[] res = new int[k];
            System.arraycopy(result, 0, res, 0, k);
            return new Hyphenation(new String(w, offset, len), res);
        } else {
            return null;
        }
    }

    /**
     * Add a character class to the tree. It is used by
     * {@link PatternParser PatternParser} as callback to
     * add character classes. Character classes define the
     * valid word characters for hyphenation. If a word contains
     * a character not defined in any of the classes, it is not hyphenated.
     * It also defines a way to normalize the characters in order
     * to compare them with the stored patterns. Usually pattern
     * files use only lower case characters, in this case a class
     * for letter 'a', for example, should be defined as "aA", the first
     * character being the normalization char.
     * @param chargroup a character class (group)
     */
    public void addClass(String chargroup) {
        if (chargroup.length() > 0) {
            char equivChar = chargroup.charAt(0);
            char[] key = new char[2];
            key[1] = 0;
            for (int i = 0; i < chargroup.length(); i++) {
                key[0] = chargroup.charAt(i);
                classmap.insert(key, 0, equivChar);
            }
        }
    }

    /**
     * Add an exception to the tree. It is used by
     * {@link PatternParser PatternParser} class as callback to
     * store the hyphenation exceptions.
     * @param word normalized word
     * @param hyphenatedword a vector of alternating strings and
     * {@link Hyphen hyphen} objects.
     */
    public void addException(String word, ArrayList hyphenatedword) {
        stoplist.put(word, hyphenatedword);
    }

    /**
     * Add a pattern to the tree. Mainly, to be used by
     * {@link PatternParser PatternParser} class as callback to
     * add a pattern to the tree.
     * @param pattern the hyphenation pattern
     * @param ivalue interletter weight values indicating the
     * desirability and priority of hyphenating at a given point
     * within the pattern. It should contain only digit characters.
     * (i.e. '0' to '9').
     */
    public void addPattern(String pattern, String ivalue) {
        int k = ivalues.find(ivalue);
        if (k <= 0) {
            k = packValues(ivalue);
            ivalues.insert(ivalue, (char)k);
        }
        insert(pattern, (char)k);
    }

    /**
     * Print statistics.
     */
    public void printStats() {
        System.out.println("Value space size = "
                           + Integer.toString(vspace.length()));
        super.printStats();

    }

    /**
     * Main entry point for this hyphenation utility application.
     * @param argv array of command linee arguments
     * @throws Exception in case an exception is raised but not caught
     */
    public static void main(String[] argv) throws Exception {
        HyphenationTree ht = null;
        int minCharCount = 2;
        BufferedReader in
            = new BufferedReader(new java.io.InputStreamReader(System.in));
        while (true) {
            System.out.print("l:\tload patterns from XML\n"
                    + "L:\tload patterns from serialized object\n"
                    + "s:\tset minimum character count\n"
                    + "w:\twrite hyphenation tree to object file\n"
                    + "h:\thyphenate\n"
                    + "f:\tfind pattern\n"
                    + "b:\tbenchmark\n"
                    + "q:\tquit\n\n"
                    + "Command:");
            String token = in.readLine();
            if (token == null) {
                break;
            }
            token = token.trim();
            if (token.equals("f")) {
                System.out.print("Pattern: ");
                token = in.readLine();
                if (token == null) {
                    break;
                }
                token = token.trim();
                System.out.println("Values: " + ht.findPattern(token));
            } else if (token.equals("s")) {
                System.out.print("Minimun value: ");
                token = in.readLine();
                if (token == null) {
                    break;
                }
                token = token.trim();
                minCharCount = Integer.parseInt(token);
            } else if (token.equals("l")) {
                ht = new HyphenationTree();
                System.out.print("XML file name: ");
                token = in.readLine();
                if (token == null) {
                    break;
                }
                token = token.trim();
                ht.loadPatterns(token);
            } else if (token.equals("L")) {
                ObjectInputStream ois = null;
                System.out.print("Object file name: ");
                token = in.readLine();
                if (token == null) {
                    break;
                }
                token = token.trim();
                try {
                    ois = new ObjectInputStream(new FileInputStream(token));
                    ht = (HyphenationTree)ois.readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (ois != null) {
                        try {
                            ois.close();
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                }
            } else if (token.equals("w")) {
                System.out.print("Object file name: ");
                token = in.readLine();
                if (token == null) {
                    break;
                }
                token = token.trim();
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(new FileOutputStream(token));
                    oos.writeObject(ht);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (oos != null) {
                        try {
                            oos.flush();
                        } catch (IOException e) {
                            //ignore
                        }
                        try {
                            oos.close();
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                }
            } else if (token.equals("h")) {
                System.out.print("Word: ");
                token = in.readLine();
                if (token == null) {
                    break;
                }
                token = token.trim();
                System.out.print("Hyphenation points: ");
                System.out.println(ht.hyphenate(token, minCharCount,
                                                minCharCount));
            } else if (token.equals("b")) {
                if (ht == null) {
                    System.out.println("No patterns have been loaded.");
                    break;
                }
                System.out.print("Word list filename: ");
                token = in.readLine();
                if (token == null) {
                    break;
                }
                token = token.trim();
                long starttime = 0;
                int counter = 0;
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(token));
                    String line;

                    starttime = System.currentTimeMillis();
                    while ((line = reader.readLine()) != null) {
                        // System.out.print("\nline: ");
                        Hyphenation hyp = ht.hyphenate(line, minCharCount,
                                                       minCharCount);
                        if (hyp != null) {
                            String hword = hyp.toString();
                            // System.out.println(line);
                            // System.out.println(hword);
                        } else {
                            // System.out.println("No hyphenation");
                        }
                        counter++;
                    }
                } catch (Exception ioe) {
                    System.out.println("Exception " + ioe);
                    ioe.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(reader);
                }
                long endtime = System.currentTimeMillis();
                long result = endtime - starttime;
                System.out.println(counter + " words in " + result
                                   + " Milliseconds hyphenated");

            } else if (token.equals("q")) {
                break;
            }
        }

    }

}
