/*
 * The MIT License
 *
 * Copyright 2012-2015 Zafar Khaja <zafarkhaja@gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.wlqq.mavenversion.expr;


import com.wlqq.mavenversion.Version;

class GreaterOrEqual implements Expression {

    /**
     * The parsed version, the right-hand operand
     * of the "greater than or equal to" operator.
     */
    private final Version mParsedVersion;

    /**
     * Constructs a {@code GreaterOrEqual} expression with the parsed version.
     *
     * @param parsedVersion the parsed version
     */
    GreaterOrEqual(Version parsedVersion) {
        this.mParsedVersion = parsedVersion;
    }

    /**
     * Checks if the current version is greater
     * than or equal to the parsed version.
     *
     * @param version the version to compare to, the left-hand operand
     *                of the "greater than or equal to" operator
     * @return {@code true} if the version is greater than or equal
     *         to the parsed version or {@code false} otherwise
     */
    @Override
    public boolean interpret(Version version) {
        return version.compareTo(mParsedVersion) >= 0;
    }
}
