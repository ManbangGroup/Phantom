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

package com.wlqq.phantom.library.pool;

import java.io.Serializable;


public class FixedActivity implements Serializable {
    public String proxyActivity;
    public String pluginActivity;

    FixedActivity(String proxyActivity, String pluginActivity) {
        this.proxyActivity = proxyActivity;
        this.pluginActivity = pluginActivity;
    }

    static FixedActivity parseFormString(String fixedActivity) {
        int index = fixedActivity.indexOf('@');
        return new FixedActivity(fixedActivity.substring(0, index),
                fixedActivity.substring(index + 1, fixedActivity.length()));
    }

    @Override
    public int hashCode() {
        return pluginActivity.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FixedActivity that = (FixedActivity) o;

        return pluginActivity.equals(that.pluginActivity);
    }

    @Override
    public String toString() {
        return proxyActivity + "@" + pluginActivity;
    }
}
