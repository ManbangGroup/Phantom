/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wlqq.mavenversion;

import com.wlqq.mavenversion.expr.ExpressionParser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Stack;


/**
 * <p>
 * Generic implementation of version comparison.
 * </p>
 *
 * Features:
 * <ul>
 * <li>mixing of '<code>-</code>' (hyphen) and '<code>.</code>' (dot) separators,</li>
 * <li>transition between characters and digits also constitutes a separator:
 *     <code>1.0alpha1 =&gt; [1, 0, alpha, 1]</code></li>
 * <li>unlimited number of version components,</li>
 * <li>version components in the text can be digits or strings,</li>
 * <li>strings are checked for well-known qualifiers and the qualifier ordering is used for version ordering.
 *     Well-known qualifiers (case insensitive) are:<ul>
 *     <li><code>alpha</code> or <code>a</code></li>
 *     <li><code>beta</code> or <code>b</code></li>
 *     <li><code>milestone</code> or <code>m</code></li>
 *     <li><code>rc</code> or <code>cr</code></li>
 *     <li><code>snapshot</code></li>
 *     <li><code>(the empty string)</code> or <code>ga</code> or <code>final</code></li>
 *     <li><code>sp</code></li>
 *     </ul>
 *     Unknown qualifiers are considered after known qualifiers, with lexical order (always case insensitive),
 *   </li>
 * <li>a hyphen usually precedes a qualifier, and is always less important than something preceded with a dot.</li>
 * </ul>
 *
 * @see <a href="https://cwiki.apache.org/confluence/display/MAVENOLD/Versioning">"Versioning" on Maven Wiki</a>
 */
// 该文件主要拷贝自 maven 项目的 https://github.com/apache/maven/blob/master/maven-artifact/src/main/java/org/apache/maven/artifact/versioning/ComparableVersion.java。
// 为方便以后获取源的更新，忽略 checkstyle 检查
@SuppressWarnings({"checkstyle:MemberName", "checkstyle:ConstantName", "checkstyle:EmptyForIteratorPad"})
public class Version implements Comparable<Version> {
    private String value;

    private String canonical;

    private ListItem items;

    /**
     * Creates a new instance of {@code Version} as a
     * result of parsing the specified version string.
     *
     * @param version the version string to parse
     * @return a new instance of the {@code Version} class
     */
    public Version(String version) {
        parseVersion(version);
    }

    private static Item parseItem(boolean isDigit, String buf) {
        return isDigit ? new IntegerItem(buf) : new StringItem(buf, false);
    }

    /**
     * Checks if this version satisfies the specified SemVer Expression string.
     * <p>
     *     Supported expression:
     *     <ul>
     *         <li>
     *             equal e.g. 1.2.3
     *         </li>
     *         <li>
     *             greater or equal e.g. >=1.2.3
     *         </li>
     *     </ul>
     * </p>
     *
     * @param expr the SemVer Expression string
     * @return {@code true} if this version satisfies the specified
     * SemVer Expression or {@code false} otherwise
     */
    public boolean satisfies(String expr) {
        try {
            return ExpressionParser.INSTANCE.parse(expr).interpret(this);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private final void parseVersion(String version) {
        this.value = version;

        items = new ListItem();

        version = version.toLowerCase(Locale.ENGLISH);

        ListItem list = items;

        Stack<Item> stack = new Stack<Item>();
        stack.push(list);

        boolean isDigit = false;

        int startIndex = 0;

        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);

            if (c == '.') {
                if (i == startIndex) {
                    list.add(IntegerItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;
            } else if (c == '-') {
                if (i == startIndex) {
                    list.add(IntegerItem.ZERO);
                } else {
                    list.add(parseItem(isDigit, version.substring(startIndex, i)));
                }
                startIndex = i + 1;

                if (isDigit) {
                    list.normalize(); // 1.0-* = 1-*

                    if ((i + 1 < version.length()) && Character.isDigit(version.charAt(i + 1))) {
                        // new ListItem only if previous were digits and new char is a digit,
                        // ie need to differentiate only 1.1 from 1-1
                        list.add(list = new ListItem());

                        stack.push(list);
                    }
                }
            } else if (Character.isDigit(c)) {
                if (!isDigit && i > startIndex) {
                    list.add(new StringItem(version.substring(startIndex, i), true));
                    startIndex = i;
                }

                isDigit = true;
            } else {
                if (isDigit && i > startIndex) {
                    list.add(parseItem(true, version.substring(startIndex, i)));
                    startIndex = i;
                }

                isDigit = false;
            }
        }

        if (version.length() > startIndex) {
            list.add(parseItem(isDigit, version.substring(startIndex)));
        }

        while (!stack.isEmpty()) {
            list = (ListItem) stack.pop();
            list.normalize();
        }

        canonical = items.toString();
    }

    public int compareTo(Version o) {
        return items.compareTo(o.items);
    }

    public String toString() {
        return value;
    }

    public boolean equals(Object o) {
        return (o instanceof Version) && canonical.equals(((Version) o).canonical);
    }

    public int hashCode() {
        return canonical.hashCode();
    }

    /**
     * Checks if this version is greater than the other version.
     *
     * @param other the other version to compare to
     * @return {@code true} if this version is greater than the other version
     * or {@code false} otherwise
     * @see #compareTo(Version other)
     */
    public boolean greaterThan(Version other) {
        return compareTo(other) > 0;
    }

    /**
     * Checks if this version is greater than or equal to the other version.
     *
     * @param other the other version to compare to
     * @return {@code true} if this version is greater than or equal
     * to the other version or {@code false} otherwise
     * @see #compareTo(Version other)
     */
    public boolean greaterThanOrEqualTo(Version other) {
        return compareTo(other) >= 0;
    }

    /**
     * Checks if this version is less than the other version.
     *
     * @param other the other version to compare to
     * @return {@code true} if this version is less than the other version
     * or {@code false} otherwise
     * @see #compareTo(Version other)
     */
    public boolean lessThan(Version other) {
        return compareTo(other) < 0;
    }

    /**
     * Checks if this version is less than or equal to the other version.
     *
     * @param other the other version to compare to
     * @return {@code true} if this version is less than or equal
     * to the other version or {@code false} otherwise
     * @see #compareTo(Version other)
     */
    public boolean lessThanOrEqualTo(Version other) {
        return compareTo(other) <= 0;
    }

    private interface Item {
        final int INTEGER_ITEM = 0;
        final int STRING_ITEM = 1;
        final int LIST_ITEM = 2;

        int compareTo(Item item);

        int getType();

        boolean isNull();
    }

    /**
     * Represents a numeric item in the version item list.
     */
    private static class IntegerItem
            implements Item {
        public static final IntegerItem ZERO = new IntegerItem();
        private static final BigInteger BigInteger_ZERO = new BigInteger("0");
        private final BigInteger value;

        private IntegerItem() {
            this.value = BigInteger_ZERO;
        }

        public IntegerItem(String str) {
            this.value = new BigInteger(str);
        }

        public int getType() {
            return INTEGER_ITEM;
        }

        public boolean isNull() {
            return BigInteger_ZERO.equals(value);
        }

        public int compareTo(Item item) {
            if (item == null) {
                return BigInteger_ZERO.equals(value) ? 0 : 1; // 1.0 == 1, 1.1 > 1
            }

            switch (item.getType()) {
                case INTEGER_ITEM:
                    return value.compareTo(((IntegerItem) item).value);

                case STRING_ITEM:
                    return 1; // 1.1 > 1-sp

                case LIST_ITEM:
                    return 1; // 1.1 > 1-1

                default:
                    throw new RuntimeException("invalid item: " + item.getClass());
            }
        }

        public String toString() {
            return value.toString();
        }
    }

    /**
     * Represents a string in the version item list, usually a qualifier.
     */
    private static class StringItem
            implements Item {
        private static final String[] QUALIFIERS = {"alpha", "beta", "milestone", "rc", "snapshot", "", "sp"};

        private static final List<String> _QUALIFIERS = Arrays.asList(QUALIFIERS);

        private static final Properties ALIASES = new Properties();
        /**
         * A comparable value for the empty-string qualifier. This one is used to determine if a given qualifier makes
         * the version older than one without a qualifier, or more recent.
         */
        private static final String RELEASE_VERSION_INDEX = String.valueOf(_QUALIFIERS.indexOf(""));

        static {
            ALIASES.put("ga", "");
            ALIASES.put("final", "");
            ALIASES.put("cr", "rc");
        }

        private String value;

        public StringItem(String value, boolean followedByDigit) {
            if (followedByDigit && value.length() == 1) {
                // a1 = alpha-1, b1 = beta-1, m1 = milestone-1
                switch (value.charAt(0)) {
                    case 'a':
                        value = "alpha";
                        break;
                    case 'b':
                        value = "beta";
                        break;
                    case 'm':
                        value = "milestone";
                        break;
                }
            }
            this.value = ALIASES.getProperty(value, value);
        }

        /**
         * Returns a comparable value for a qualifier.
         *
         * This method both takes into account the ordering of known qualifiers as well as lexical ordering for unknown
         * qualifiers.
         *
         * just returning an Integer with the index here is faster, but requires a lot of if/then/else to check for -1
         * or QUALIFIERS.size and then resort to lexical ordering. Most comparisons are decided by the first character,
         * so this is still fast. If more characters are needed then it requires a lexical sort anyway.
         *
         * @return an equivalent value that can be used with lexical comparison
         */
        public static String comparableQualifier(String qualifier) {
            int i = _QUALIFIERS.indexOf(qualifier);

            return i == -1 ? _QUALIFIERS.size() + "-" + qualifier : String.valueOf(i);
        }

        public int getType() {
            return STRING_ITEM;
        }

        public boolean isNull() {
            return (comparableQualifier(value).compareTo(RELEASE_VERSION_INDEX) == 0);
        }

        public int compareTo(Item item) {
            if (item == null) {
                // 1-rc < 1, 1-ga > 1
                return comparableQualifier(value).compareTo(RELEASE_VERSION_INDEX);
            }
            switch (item.getType()) {
                case INTEGER_ITEM:
                    return -1; // 1.any < 1.1 ?

                case STRING_ITEM:
                    return comparableQualifier(value).compareTo(comparableQualifier(((StringItem) item).value));

                case LIST_ITEM:
                    return -1; // 1.any < 1-1

                default:
                    throw new RuntimeException("invalid item: " + item.getClass());
            }
        }

        public String toString() {
            return value;
        }
    }

    /**
     * Represents a version list item. This class is used both for the global item list and for sub-lists (which start
     * with '-(number)' in the version specification).
     */
    private static class ListItem
            extends ArrayList<Item>
            implements Item {
        public int getType() {
            return LIST_ITEM;
        }

        public boolean isNull() {
            return (size() == 0);
        }

        void normalize() {
            for (ListIterator<Item> iterator = listIterator(size()); iterator.hasPrevious(); ) {
                Item item = iterator.previous();
                if (item.isNull()) {
                    iterator.remove(); // remove null trailing items: 0, "", empty list
                } else {
                    break;
                }
            }
        }

        public int compareTo(Item item) {
            if (item == null) {
                if (size() == 0) {
                    return 0; // 1-0 = 1- (normalize) = 1
                }
                Item first = get(0);
                return first.compareTo(null);
            }
            switch (item.getType()) {
                case INTEGER_ITEM:
                    return -1; // 1-1 < 1.0.x

                case STRING_ITEM:
                    return 1; // 1-1 > 1-sp

                case LIST_ITEM:
                    Iterator<Item> left = iterator();
                    Iterator<Item> right = ((ListItem) item).iterator();

                    while (left.hasNext() || right.hasNext()) {
                        Item l = left.hasNext() ? left.next() : null;
                        Item r = right.hasNext() ? right.next() : null;

                        // if this is shorter, then invert the compare and mul with -1
                        int result = l == null ? -1 * r.compareTo(l) : l.compareTo(r);

                        if (result != 0) {
                            return result;
                        }
                    }

                    return 0;

                default:
                    throw new RuntimeException("invalid item: " + item.getClass());
            }
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder("(");
            for (Iterator<Item> iter = iterator(); iter.hasNext(); ) {
                buffer.append(iter.next());
                if (iter.hasNext()) {
                    buffer.append(',');
                }
            }
            buffer.append(')');
            return buffer.toString();
        }
    }
}
