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

package com.wlqq.phantom.communication;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;


public class PhantomServiceManagerTest {
    @Rule
    public ExpectedException mExceptions = ExpectedException.none();

    @BeforeClass
    public static void classSetup() {
        PhantomServiceManager.init("com.wlqq", "5.8.1", 5080100, null, 0);
    }

    @Before
    public void setup() throws Exception {
        PhantomServiceManager.registerService(new RemoteService1());
        PhantomServiceManager.registerService(new RemoteService2());
    }

    @After
    public void tearDown() throws Exception {
        PhantomServiceManager.unregisterAllService();
    }

    @Test
    public void registerService_shouldThrowException() throws Exception {
        mExceptions.expect(IllegalArgumentException.class);
        mExceptions.expectMessage("service class must has PhantomService annotation");

        PhantomServiceManager.registerService(new RemoteServiceMissingAnnotation());
    }

    @Test
    public void getService_CanGetServiceInfo() throws Exception {
        final IService service1 = PhantomServiceManager.getService("remote_service_1");
        Assert.assertEquals("remote_service_1", service1.getServiceName());
        Assert.assertEquals(2, service1.getServiceVersion());
    }

    @Test
    public void getServices_CanGetByCategory() throws Exception {
        final List<IService> services = PhantomServiceManager.getServices(
                PhantomServiceManager.getHostPackage());
        Assert.assertEquals(2, services.size());
    }

    @Test
    public void callRemoteService_isCorrect() throws Exception {
        {
            final IService service1 = PhantomServiceManager.getService("remote_service_1");
            Assert.assertEquals("remote_method_1", service1.call("remote_method_1"));

            String message1 = "hahaha";
            Assert.assertEquals("remote_method_2 reply " + message1,
                    service1.call("remote_method_2", message1));
        }

        {
            final IService service2 = PhantomServiceManager.getService("remote_service_2");
            Assert.assertEquals("remote_method_1", service2.call("remote_method_1"));

            String message2 = "hahaha";
            Assert.assertEquals("remote_method_2 reply " + message2,
                    service2.call("remote_method_2", message2));
        }
    }

    @Test
    public void callRemoteServiceAsInterface_isCorrect() throws Exception {
        {
            final RemoteServiceProxy service1 = PhantomServiceManager.getService(
                    "remote_service_1", RemoteServiceProxy.class);
            Assert.assertEquals("remote_method_1", service1.remote_method_1());

            String message1 = "hahaha";
            Assert.assertEquals("remote_method_2 reply " + message1,
                    service1.remote_method_2(message1));
        }

        {
            final RemoteServiceProxy service2 = PhantomServiceManager.getService(
                    "remote_service_2",
                    RemoteServiceProxy.class);
            Assert.assertEquals("remote_method_1", service2.remote_method_1());

            String message2 = "hahaha";
            Assert.assertEquals("remote_method_2 reply " + message2,
                    service2.remote_method_2(message2));
        }
    }

    private interface RemoteServiceProxy {
        String remote_method_1();

        String remote_method_2(String message);
    }

    @PhantomService(name = "remote_service_1", version = 2)
    private static class RemoteService1 {

        @RemoteMethod(name = "remote_method_1")
        public String remoteMethod1() {
            return "remote_method_1";
        }

        @RemoteMethod(name = "remote_method_2")
        public String remoteMethod2(String message) {
            return "remote_method_2 reply " + message;
        }
    }

    @PhantomService(name = "remote_service_2", version = 3)
    private static class RemoteService2 {

        @RemoteMethod(name = "remote_method_1")
        public String remoteMethod1() {
            return "remote_method_1";
        }

        @RemoteMethod(name = "remote_method_2")
        public String remoteMethod2(String message) {
            return "remote_method_2 reply " + message;
        }
    }

    // 没有使用 PhantomService 注解的服务类，注册会抛出异常
    private static class RemoteServiceMissingAnnotation {
        @RemoteMethod(name = "remote_method_1")
        public String remoteMethod1() {
            return "remote_method_1";
        }

        @RemoteMethod(name = "remote_method_2")
        public String remoteMethod2(String message) {
            return "remote_method_2 reply " + message;
        }
    }
}

