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

package org.apache.shardingsphere.proxy.frontend.postgresql.auth;

import com.google.common.base.Strings;
import io.netty.channel.ChannelHandlerContext;
import org.apache.shardingsphere.db.protocol.payload.PacketPayload;
import org.apache.shardingsphere.db.protocol.postgresql.constant.PostgreSQLErrorCode;
import org.apache.shardingsphere.db.protocol.postgresql.packet.command.query.binary.BinaryStatementRegistry;
import org.apache.shardingsphere.db.protocol.postgresql.packet.generic.PostgreSQLErrorResponsePacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.generic.PostgreSQLReadyForQueryPacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLAuthenticationMD5PasswordPacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLAuthenticationOKPacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLComStartupPacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLParameterStatusPacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLPasswordMessagePacket;
import org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLRandomGenerator;
import org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLSSLNegativePacket;
import org.apache.shardingsphere.db.protocol.postgresql.payload.PostgreSQLPacketPayload;
import org.apache.shardingsphere.proxy.backend.schema.ProxySchemaContexts;
import org.apache.shardingsphere.proxy.frontend.ConnectionIdGenerator;
import org.apache.shardingsphere.proxy.frontend.engine.AuthenticationEngine;
import org.apache.shardingsphere.proxy.frontend.engine.AuthenticationResult;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Authentication engine for PostgreSQL.
 */
public final class PostgreSQLAuthenticationEngine implements AuthenticationEngine {
    
    private static final int SSL_REQUEST_PAYLOAD_LENGTH = 8;
    
    private static final int SSL_REQUEST_CODE = 80877103;
    
    private static final String USER_NAME_KEYWORD = "user";
    
    private static final String DATABASE_NAME_KEYWORD = "database";
    
    private final AtomicBoolean startupMessageReceived = new AtomicBoolean(false);
    
    private volatile byte[] md5Salt;
    
    private AuthenticationResult currentAuthResult;
    
    @Override
    public int handshake(final ChannelHandlerContext context) {
        int result = ConnectionIdGenerator.getInstance().nextId();
        BinaryStatementRegistry.getInstance().register(result);
        return result;
    }
    
    @Override
public org.apache.shardingsphere.proxy.frontend.engine.AuthenticationResult auth(final io.netty.channel.ChannelHandlerContext context, final org.apache.shardingsphere.db.protocol.payload.PacketPayload payload) {
    if ((org.apache.shardingsphere.proxy.frontend.postgresql.auth.PostgreSQLAuthenticationEngine.SSL_REQUEST_PAYLOAD_LENGTH == payload.getByteBuf().markReaderIndex().readInt()) && (org.apache.shardingsphere.proxy.frontend.postgresql.auth.PostgreSQLAuthenticationEngine.SSL_REQUEST_CODE == payload.getByteBuf().readInt())) {
        context.writeAndFlush(new org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLSSLNegativePacket());
        return org.apache.shardingsphere.proxy.frontend.engine.AuthenticationResult.continued();
    }
    payload.getByteBuf().resetReaderIndex();
    if (!startupMessageReceived.get()) {
        org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLComStartupPacket comStartupPacket = new org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLComStartupPacket(((org.apache.shardingsphere.db.protocol.postgresql.payload.PostgreSQLPacketPayload) (payload)));
        startupMessageReceived.set(true);
        java.lang.String databaseName = comStartupPacket.getParametersMap().get(org.apache.shardingsphere.proxy.frontend.postgresql.auth.PostgreSQLAuthenticationEngine.DATABASE_NAME_KEYWORD);
        if ((!com.google.common.base.Strings.isNullOrEmpty(databaseName)) && (!org.apache.shardingsphere.proxy.backend.schema.ProxySchemaContexts.getInstance().schemaExists(databaseName))) {
            org.apache.shardingsphere.db.protocol.postgresql.packet.generic.PostgreSQLErrorResponsePacket responsePacket = createErrorPacket(org.apache.shardingsphere.db.protocol.postgresql.constant.PostgreSQLErrorCode.INVALID_CATALOG_NAME, java.lang.String.format("database \"%s\" does not exist", databaseName));
            context.writeAndFlush(responsePacket);
            context.close();
            return org.apache.shardingsphere.proxy.frontend.engine.AuthenticationResult.continued();
        }
        java.lang.String username = comStartupPacket.getParametersMap().get(org.apache.shardingsphere.proxy.frontend.postgresql.auth.PostgreSQLAuthenticationEngine.USER_NAME_KEYWORD);
        if ((null == username) || username.isEmpty()) {
            org.apache.shardingsphere.db.protocol.postgresql.packet.generic.PostgreSQLErrorResponsePacket responsePacket = createErrorPacket(org.apache.shardingsphere.db.protocol.postgresql.constant.PostgreSQLErrorCode.SQLSERVER_REJECTED_ESTABLISHMENT_OF_SQLCONNECTION, "user not set in StartupMessage");
            context.writeAndFlush(responsePacket);
            context.close();
            return org.apache.shardingsphere.proxy.frontend.engine.AuthenticationResult.continued();
        }
        md5Salt = org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLRandomGenerator.getInstance().generateRandomBytes(4);
        context.writeAndFlush(new org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLAuthenticationMD5PasswordPacket(md5Salt));
        return org.apache.shardingsphere.proxy.frontend.engine.AuthenticationResult.continued(username, databaseName);
    } else {
        char messageType = ((char) (((org.apache.shardingsphere.db.protocol.postgresql.payload.PostgreSQLPacketPayload) (payload)).readInt1()));
        if ('p' != messageType) {
            org.apache.shardingsphere.db.protocol.postgresql.packet.generic.PostgreSQLErrorResponsePacket responsePacket = createErrorPacket(org.apache.shardingsphere.db.protocol.postgresql.constant.PostgreSQLErrorCode.SQLSERVER_REJECTED_ESTABLISHMENT_OF_SQLCONNECTION, java.lang.String.format("PasswordMessage is expected, message type 'p', but not '%s'", messageType));
            context.writeAndFlush(responsePacket);
            context.close();
            currentAuthResult = org.apache.shardingsphere.proxy.frontend.engine.AuthenticationResult.continued();
            return currentAuthResult;
        }
        org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLPasswordMessagePacket passwordMessagePacket = new org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLPasswordMessagePacket(((org.apache.shardingsphere.db.protocol.postgresql.payload.PostgreSQLPacketPayload) (payload)));
        /* NPEX_PATCH_BEGINS */
        if (currentAuthResult == null) {
            return null;
        }
        org.apache.shardingsphere.proxy.frontend.postgresql.auth.PostgreSQLLoginResult loginResult = org.apache.shardingsphere.proxy.frontend.postgresql.auth.PostgreSQLAuthenticationHandler.loginWithMd5Password(currentAuthResult.getUsername(), currentAuthResult.getDatabase(), md5Salt, passwordMessagePacket);
        if (org.apache.shardingsphere.db.protocol.postgresql.constant.PostgreSQLErrorCode.SUCCESSFUL_COMPLETION != loginResult.getErrorCode()) {
            org.apache.shardingsphere.db.protocol.postgresql.packet.generic.PostgreSQLErrorResponsePacket responsePacket = createErrorPacket(loginResult.getErrorCode(), loginResult.getErrorMessage());
            context.writeAndFlush(responsePacket);
            context.close();
            return org.apache.shardingsphere.proxy.frontend.engine.AuthenticationResult.continued();
        } else {
            // TODO implement PostgreSQLServerInfo like MySQLServerInfo
            context.write(new org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLAuthenticationOKPacket(true));
            context.write(new org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLParameterStatusPacket("server_version", "12.3"));
            context.write(new org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLParameterStatusPacket("client_encoding", "UTF8"));
            context.write(new org.apache.shardingsphere.db.protocol.postgresql.packet.handshake.PostgreSQLParameterStatusPacket("server_encoding", "UTF8"));
            context.writeAndFlush(new org.apache.shardingsphere.db.protocol.postgresql.packet.generic.PostgreSQLReadyForQueryPacket());
            return org.apache.shardingsphere.proxy.frontend.engine.AuthenticationResult.finished(currentAuthResult.getUsername(), currentAuthResult.getDatabase());
        }
    }
}
    
    private PostgreSQLErrorResponsePacket createErrorPacket(final PostgreSQLErrorCode errorCode, final String errorMessage) {
        PostgreSQLErrorResponsePacket result = new PostgreSQLErrorResponsePacket();
        result.addField(PostgreSQLErrorResponsePacket.FIELD_TYPE_SEVERITY, "FATAL");
        result.addField(PostgreSQLErrorResponsePacket.FIELD_TYPE_CODE, errorCode.getErrorCode());
        result.addField(PostgreSQLErrorResponsePacket.FIELD_TYPE_MESSAGE, Strings.isNullOrEmpty(errorMessage) ? errorCode.getConditionName() : errorMessage);
        return result;
    }
}
