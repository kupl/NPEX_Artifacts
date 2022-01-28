/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
#include "proton/connection_options.hpp"
#include "proton/handler.hpp"
#include "proton/reconnect_timer.hpp"
#include "proton/transport.hpp"
#include "proton/ssl.hpp"
#include "proton/sasl.hpp"

#include "contexts.hpp"
#include "connector.hpp"
#include "messaging_adapter.hpp"
#include "msg.hpp"

#include "proton/connection.h"
#include "proton/transport.h"

namespace proton {

template <class T> struct option {
    T value;
    bool set;

    option() : value(), set(false) {}
    option& operator=(const T& x) { value = x;  set = true; return *this; }
    void override(const option<T>& x) { if (x.set) *this = x.value; }
};

class connection_options::impl {
  public:
    option<proton_handler*> handler;
    option<uint32_t> max_frame_size;
    option<uint16_t> max_channels;
    option<duration> idle_timeout;
    option<duration> heartbeat;
    option<std::string> container_id;
    option<std::string> link_prefix;
    option<reconnect_timer> reconnect;
    option<class ssl_client_options> ssl_client_options;
    option<class ssl_server_options> ssl_server_options;
    option<std::string> peer_hostname;
    option<std::string> resume_id;
    option<bool> sasl_enabled;
    option<std::string> allowed_mechs;
    option<bool> allow_insecure_mechs;
    option<std::string> sasl_config_name;
    option<std::string> sasl_config_path;

    void apply(connection& c) {
        pn_connection_t *pnc = connection_options::pn_connection(c);
        pn_transport_t *pnt = pn_connection_transport(pnc);
        connector *outbound = dynamic_cast<connector*>(
            connection_context::get(c).handler.get());
        bool uninit = (c.state() & endpoint::LOCAL_UNINIT);

        // pnt is NULL between reconnect attempts.
        // Only apply transport options if uninit or outbound with
        // transport not yet configured.
        if (pnt && (uninit || (outbound && !outbound->transport_configured())))
        {
            // SSL
            if (outbound && outbound->address().scheme() == url::AMQPS) {
                const char* id = resume_id.value.empty() ? NULL : resume_id.value.c_str();
                pn_ssl_t *ssl = pn_ssl(pnt);
                if (pn_ssl_init(ssl, ssl_client_options.value.pn_domain(), id))
                    throw error(MSG("client SSL/TLS initialization error"));
                if (peer_hostname.set && !peer_hostname.value.empty())
                    if (pn_ssl_set_peer_hostname(ssl, peer_hostname.value.c_str()))
                        throw error(MSG("error in SSL/TLS peer hostname \"") << peer_hostname.value << '"');
            } else if (!outbound) {
                pn_acceptor_t *pnp = pn_connection_acceptor(pnc);
                if (pnp) {
                    listener_context &lc(listener_context::get(pnp));
                    if (lc.ssl) {
                        pn_ssl_t *ssl = pn_ssl(pnt);
                        if (pn_ssl_init(ssl, ssl_server_options.value.pn_domain(), NULL))
                            throw error(MSG("server SSL/TLS initialization error"));
                    }
                }
            }

            // SASL
            transport t = c.transport();
            if (!sasl_enabled.set || sasl_enabled.value) {
                if (sasl_enabled.set)  // Explicitly set, not just default behaviour.
                    t.sasl();          // Force a sasl instance.  Lazily create one otherwise.
                if (allow_insecure_mechs.set)
                    t.sasl().allow_insecure_mechs(allow_insecure_mechs.value);
                if (allowed_mechs.set)
                    t.sasl().allowed_mechs(allowed_mechs.value);
                if (sasl_config_name.set)
                    t.sasl().config_name(sasl_config_name.value);
                if (sasl_config_path.set)
                    t.sasl().config_path(sasl_config_path.value);
            }

            if (max_frame_size.set)
                pn_transport_set_max_frame(pnt, max_frame_size.value);
            if (max_channels.set)
                pn_transport_set_channel_max(pnt, max_channels.value);
            if (idle_timeout.set)
                pn_transport_set_idle_timeout(pnt, idle_timeout.value.milliseconds);
        }
        // Only apply connection options if uninit.
        if (uninit) {
            if (reconnect.set && outbound)
                outbound->reconnect_timer(reconnect.value);
            if (container_id.set)
                pn_connection_set_container(pnc, container_id.value.c_str());
            if (link_prefix.set)
                connection_context::get(pnc).link_gen.prefix(link_prefix.value);
        }
    }

    void override(const impl& x) {
        handler.override(x.handler);
        max_frame_size.override(x.max_frame_size);
        max_channels.override(x.max_channels);
        idle_timeout.override(x.idle_timeout);
        heartbeat.override(x.heartbeat);
        container_id.override(x.container_id);
        link_prefix.override(x.link_prefix);
        reconnect.override(x.reconnect);
        ssl_client_options.override(x.ssl_client_options);
        ssl_server_options.override(x.ssl_server_options);
        resume_id.override(x.resume_id);
        peer_hostname.override(x.peer_hostname);
        sasl_enabled.override(x.sasl_enabled);
        allow_insecure_mechs.override(x.allow_insecure_mechs);
        allowed_mechs.override(x.allowed_mechs);
        sasl_config_name.override(x.sasl_config_name);
        sasl_config_path.override(x.sasl_config_path);
    }

};

connection_options::connection_options() : impl_(new impl()) {}
connection_options::connection_options(const connection_options& x) : impl_(new impl()) {
    *this = x;
}
connection_options::~connection_options() {}

connection_options& connection_options::operator=(const connection_options& x) {
    *impl_ = *x.impl_;
    return *this;
}

void connection_options::override(const connection_options& x) { impl_->override(*x.impl_); }

connection_options& connection_options::handler(class handler *h) { impl_->handler = h->messaging_adapter_.get(); return *this; }
connection_options& connection_options::max_frame_size(uint32_t n) { impl_->max_frame_size = n; return *this; }
connection_options& connection_options::max_channels(uint16_t n) { impl_->max_frame_size = n; return *this; }
connection_options& connection_options::idle_timeout(duration t) { impl_->idle_timeout = t; return *this; }
connection_options& connection_options::heartbeat(duration t) { impl_->heartbeat = t; return *this; }
connection_options& connection_options::container_id(const std::string &id) { impl_->container_id = id; return *this; }
connection_options& connection_options::link_prefix(const std::string &id) { impl_->link_prefix = id; return *this; }
connection_options& connection_options::reconnect(const reconnect_timer &rc) { impl_->reconnect = rc; return *this; }
connection_options& connection_options::ssl_client_options(const class ssl_client_options &c) { impl_->ssl_client_options = c; return *this; }
connection_options& connection_options::ssl_server_options(const class ssl_server_options &c) { impl_->ssl_server_options = c; return *this; }
connection_options& connection_options::resume_id(const std::string &id) { impl_->resume_id = id; return *this; }
connection_options& connection_options::peer_hostname(const std::string &name) { impl_->peer_hostname = name; return *this; }
connection_options& connection_options::sasl_enabled(bool b) { impl_->sasl_enabled = b; return *this; }
connection_options& connection_options::allow_insecure_mechs(bool b) { impl_->allow_insecure_mechs = b; return *this; }
connection_options& connection_options::allowed_mechs(const std::string &s) { impl_->allowed_mechs = s; return *this; }
connection_options& connection_options::sasl_config_name(const std::string &n) { impl_->sasl_config_name = n; return *this; }
connection_options& connection_options::sasl_config_path(const std::string &p) { impl_->sasl_config_path = p; return *this; }

void connection_options::apply(connection& c) const { impl_->apply(c); }
class ssl_client_options &connection_options::ssl_client_options() { return impl_->ssl_client_options.value; }
class ssl_server_options &connection_options::ssl_server_options() { return impl_->ssl_server_options.value; }
proton_handler* connection_options::handler() const { return impl_->handler.value; }
pn_connection_t* connection_options::pn_connection(connection &c) { return c.pn_object(); }
} // namespace proton
