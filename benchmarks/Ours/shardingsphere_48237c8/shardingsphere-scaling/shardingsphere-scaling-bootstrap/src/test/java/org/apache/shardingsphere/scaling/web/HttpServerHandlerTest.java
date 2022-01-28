/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.scaling.web;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.apache.shardingsphere.orchestration.core.common.yaml.config.YamlDataSourceConfiguration;
import org.apache.shardingsphere.scaling.core.config.ScalingConfiguration;
import org.apache.shardingsphere.scaling.core.config.ScalingContext;
import org.apache.shardingsphere.scaling.core.config.ServerConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public final class HttpServerHandlerTest {
    
    private static final Gson GSON = new Gson();
    
    @Mock
    private ChannelHandlerContext channelHandlerContext;
    
    private FullHttpRequest fullHttpRequest;
    
    private HttpServerHandler httpServerHandler;
    
    private ScalingConfiguration scalingConfiguration;
    
    @Before
    public void setUp() {
        initConfig("/config.json");
        ScalingContext.getInstance().init(new ServerConfiguration());
        httpServerHandler = new HttpServerHandler();
    }
    
    @Test
    public void assertChannelReadStartSuccess() {
        startScalingJob();
        ArgumentCaptor<FullHttpResponse> argumentCaptor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(channelHandlerContext).writeAndFlush(argumentCaptor.capture());
        FullHttpResponse fullHttpResponse = argumentCaptor.getValue();
        assertTrue(fullHttpResponse.content().toString(CharsetUtil.UTF_8).contains("{\"success\":true"));
    }
    
    private void startScalingJob() {
        scalingConfiguration.getRuleConfiguration().setSourceDatasource("ds_0: !!" + YamlDataSourceConfiguration.class.getName() + "\n  "
                + "dataSourceClassName: com.zaxxer.hikari.HikariDataSource\n  props:\n    "
                + "jdbcUrl: jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MySQL\n    username: root\n    password: 'password'\n    connectionTimeout: 30000\n    "
                + "idleTimeout: 60000\n    maxLifetime: 1800000\n    maxPoolSize: 50\n    minPoolSize: 1\n    maintenanceIntervalMilliseconds: 30000\n    readOnly: false\n");
        scalingConfiguration.getRuleConfiguration().getDestinationDataSources().setUrl("jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MySQL");
        scalingConfiguration.getRuleConfiguration().getDestinationDataSources().setName("root");
        scalingConfiguration.getRuleConfiguration().getDestinationDataSources().setPassword("password");
        ByteBuf byteBuf = Unpooled.copiedBuffer(GSON.toJson(scalingConfiguration), CharsetUtil.UTF_8);
        fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/scaling/job/start", byteBuf);
        httpServerHandler.channelRead0(channelHandlerContext, fullHttpRequest);
    }
    
    @Test
    public void assertChannelReadProgressFail() {
        fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/scaling/job/progress/9");
        httpServerHandler.channelRead0(channelHandlerContext, fullHttpRequest);
        ArgumentCaptor<FullHttpResponse> argumentCaptor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(channelHandlerContext).writeAndFlush(argumentCaptor.capture());
        FullHttpResponse fullHttpResponse = argumentCaptor.getValue();
        assertTrue(fullHttpResponse.content().toString(CharsetUtil.UTF_8).contains("Can't find scaling job id 9"));
    }
    
    @Test
    public void assertChannelReadProgressSuccess() {
        startScalingJob();
        fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/scaling/job/progress/1");
        httpServerHandler.channelRead0(channelHandlerContext, fullHttpRequest);
        ArgumentCaptor<FullHttpResponse> argumentCaptor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(channelHandlerContext, times(2)).writeAndFlush(argumentCaptor.capture());
        FullHttpResponse fullHttpResponse = argumentCaptor.getValue();
        assertTrue(fullHttpResponse.content().toString(CharsetUtil.UTF_8).contains("{\"success\":true"));
    }
    
    @Test
    public void assertChannelReadStop() {
        Map<String, Integer> map = new HashMap<>();
        map.put("id", 1);
        ByteBuf byteBuf = Unpooled.copiedBuffer(GSON.toJson(map), CharsetUtil.UTF_8);
        fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/scaling/job/stop", byteBuf);
        httpServerHandler.channelRead0(channelHandlerContext, fullHttpRequest);
        ArgumentCaptor<FullHttpResponse> argumentCaptor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(channelHandlerContext).writeAndFlush(argumentCaptor.capture());
        FullHttpResponse fullHttpResponse = argumentCaptor.getValue();
        assertTrue(fullHttpResponse.content().toString(CharsetUtil.UTF_8).contains("{\"success\":true"));
    }
    
    @Test
    public void assertChannelReadList() {
        fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/scaling/job/list");
        httpServerHandler.channelRead0(channelHandlerContext, fullHttpRequest);
        ArgumentCaptor<FullHttpResponse> argumentCaptor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(channelHandlerContext).writeAndFlush(argumentCaptor.capture());
        FullHttpResponse fullHttpResponse = argumentCaptor.getValue();
        assertTrue(fullHttpResponse.content().toString(CharsetUtil.UTF_8).contains("{\"success\":true"));
    }
    
    @Test
    public void assertChannelReadUnsupportedUrl() {
        fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, "/scaling/1");
        httpServerHandler.channelRead0(channelHandlerContext, fullHttpRequest);
        ArgumentCaptor<FullHttpResponse> argumentCaptor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(channelHandlerContext).writeAndFlush(argumentCaptor.capture());
        FullHttpResponse fullHttpResponse = argumentCaptor.getValue();
        assertTrue(fullHttpResponse.content().toString(CharsetUtil.UTF_8).contains("Not support request!"));
    }
    
    @Test
    public void assertChannelReadUnsupportedMethod() {
        fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, "/scaling/job/stop");
        httpServerHandler.channelRead0(channelHandlerContext, fullHttpRequest);
        ArgumentCaptor<FullHttpResponse> argumentCaptor = ArgumentCaptor.forClass(FullHttpResponse.class);
        verify(channelHandlerContext).writeAndFlush(argumentCaptor.capture());
        FullHttpResponse fullHttpResponse = argumentCaptor.getValue();
        assertTrue(fullHttpResponse.content().toString(CharsetUtil.UTF_8).contains("Not support request!"));
    }
    
    @Test
    public void assertExceptionCaught() {
        Throwable throwable = mock(Throwable.class);
        httpServerHandler.exceptionCaught(channelHandlerContext, throwable);
        verify(channelHandlerContext).close();
    }
    
    private void initConfig(final String configFile) {
        InputStream fileInputStream = HttpServerHandlerTest.class.getResourceAsStream(configFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        scalingConfiguration = GSON.fromJson(inputStreamReader, ScalingConfiguration.class);
    }
}
