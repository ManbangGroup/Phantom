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

package com.wlqq.mavenversion;


import java.util.Locale;
import java.util.Map;

/**
 * Verify provided lib version satisfies version requirements
 */
public final class VersionVerifier {
    private VersionVerifier() {
        // prevent instantiate
    }

    /**
     * Check lib version satisfies lib version requirements
     *
     * @param versions     e.g. <code>{"junit:junit": "4.12", "com.android.support:support-v4": "25.3.1"}</code>
     * @param requirements e.g. <code>{"junit:junit": "4.12", "com.android.support:support-v4": ">=25"}</code>
     * @return verify result
     * @see Result
     */
    public static Result satisfies(Map<String, String> versions, Map<String, String> requirements) {
        for (Map.Entry<String, String> entry : requirements.entrySet()) {
            final String lib = entry.getKey();
            final String requirement = entry.getValue();

            final String version = versions.get(lib);
            if (version == null) {
                // lib not exist
                return Result.createFailResult(lib, String.format(Locale.ENGLISH, "required lib [%s] not exist", lib));
            }

            if (!new Version(version).satisfies(requirement)) {
                // lib version requirement not satisfied
                return Result.createFailResult(lib, String.format(Locale.ENGLISH,
                                "required lib [%s:%s] not satisfies [%s:%s]", lib, version, lib, requirement));
            }
        }
        return Result.createSuccessResult();
    }

    /**
     * The verify result
     *
     * @see #satisfies(Map, Map)
     */
    public static final class Result {
        /**
         * Whether lib satisfies requirement
         */
        public final boolean success;
        /**
         * The failed lib name, only available when fail; otherwise <code>null</code>
         */
        public final String lib;
        /**
         * The detailed fail message, only available when fail; otherwise <code>null</code>
         */
        public final String message;

        private Result(boolean success, String lib, String message) {
            this.success = success;
            this.lib = lib;
            this.message = message;
        }

        /**
         * Create success result; lib, message would be <code>null</code>
         * @return success result
         */
        public static Result createSuccessResult() {
            return new Result(true, null, null);
        }

        /**
         * Create fail result
         * @param lib which lib requirement not satisfied
         * @param message detail fail message
         * @return fail result
         */
        public static Result createFailResult(String lib, String message) {
            return new Result(false, lib, message);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Result{");
            sb.append("success=").append(success);
            sb.append(", lib='").append(lib).append('\'');
            sb.append(", message='").append(message).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
