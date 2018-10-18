/*
 * Copyright (C) 2017-2018 Manbang Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wlqq.phantom.library.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


public final class IoUtils {
    public static final String DEFAULT_CHARSET = "utf-8";

    private IoUtils() {
        // prevent initialization
    }

    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "has another method with encoding arguments")
    @NonNull
    public static List<String> readLines(InputStream input) throws IOException {
        InputStreamReader reader = new InputStreamReader(input);
        return readLines(reader);
    }

    @NonNull
    public static List<String> readLines(InputStream input, String encoding) throws IOException {
        if (encoding == null) {
            return readLines(input);
        } else {
            InputStreamReader reader = new InputStreamReader(input, encoding);
            return readLines(reader);
        }
    }

    @NonNull
    public static List<String> readLines(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        ArrayList<String> list = new ArrayList<>();

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            list.add(line);
        }

        return list;
    }
}
