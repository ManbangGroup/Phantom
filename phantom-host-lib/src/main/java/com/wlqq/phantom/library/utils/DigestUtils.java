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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public final class DigestUtils {
    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'
    };

    private DigestUtils() {
        throw new AssertionError();
    }

    @NonNull
    private static String toHexString(@NonNull byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_DIGITS[(b & 0xf0) >> 4]);
            sb.append(HEX_DIGITS[b & 0x0f]);
        }
        return sb.toString();
    }

    @Nullable
    private static byte[] md5(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(bytes);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            // should not happen
            return null;
        }
    }

    @Nullable
    public static String md5Hex(byte[] bytes) {
        final byte[] md5 = md5(bytes);
        return md5 == null ? null : toHexString(md5);
    }
}
