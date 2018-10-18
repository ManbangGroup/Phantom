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

package com.wlqq.phantom.plugin.component;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.wlqq.phantom.plugin.component.fragment.ActivityFragment;
import com.wlqq.phantom.plugin.component.fragment.BroadcastFragment;
import com.wlqq.phantom.plugin.component.fragment.ServiceFragment;

public class MainActivity extends FragmentActivity {

    public static final String ACTION_BROADCAST_MSG = "com.phantom.plugin.component.action.BROADCAST_MSG";

    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(new ComponentFragmentPagerAdapter(getSupportFragmentManager()));
    }

    class ComponentFragmentPagerAdapter extends FragmentPagerAdapter {

        public ComponentFragmentPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ActivityFragment();
                case 1:
                    return new ServiceFragment();
                case 2:
                    return new BroadcastFragment();
                default:
                    // should not reach here
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Activity";
                case 1:
                    return "Service";
                case 2:
                    return "Broadcast";
                default:
                    // should not reach here
                    return "";
            }
        }
    }
}
