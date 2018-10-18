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


import com.wlqq.mavenversion.ParseException;
import com.wlqq.mavenversion.Parser;
import com.wlqq.mavenversion.Version;

public enum  ExpressionParser implements Parser<Expression> {
    INSTANCE;

    @Override
    public Expression parse(String input) throws ParseException {
        boolean isGte = false;
        String versionText;
        if (input.startsWith(">=")) {
            isGte = true;
            // strip operator
            versionText = input.substring(2);
        } else {
            versionText = input;
        }

        final Version parsedVersion = new Version(versionText);
        if (isGte) {
            return new GreaterOrEqual(parsedVersion);
        } else {
            return new Equal(parsedVersion);
        }
    }
}