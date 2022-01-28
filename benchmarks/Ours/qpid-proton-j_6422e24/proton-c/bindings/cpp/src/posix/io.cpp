/*
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
 */

#include "msg.hpp"
#include <proton/io.hpp>
#include <proton/url.hpp>

#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

namespace proton {
namespace io {

const descriptor INVALID_DESCRIPTOR = -1;

std::string error_str() {
#ifdef USE_STRERROR_R
    char buf[256];
    strerror_r(errno, buf, sizeof(buf));
    return buf;
#elifdef USE_STRERROR_S
    char buf[256];
    strerror_s(buf, sizeof(buf), errno);
    return buf;
#elifdef USE_OLD_STRERROR
    char buf[256];
    strncpy(buf, strerror(errno), sizeof(buf));
    return buf;
#else
    std::ostringstream os;
    os <<  "system error (" << errno << ")";
    return os.str();
#endif
}

namespace {

template <class T> T check(T result, const std::string& msg=std::string()) {
    if (result < 0) throw io_error(msg + error_str());
    return result;
}

void gai_check(int result, const std::string& msg="") {
    if (result) throw io_error(msg + gai_strerror(result));
}

}

void socket_engine::init() {
    check(fcntl(socket_, F_SETFL, fcntl(socket_, F_GETFL, 0) | O_NONBLOCK), "set nonblock: ");
}

socket_engine::socket_engine(descriptor fd, handler& h, const connection_options &opts)
    : connection_engine(h, opts), socket_(fd)
{
    init();
}

socket_engine::socket_engine(const url& u, handler& h, const connection_options& opts)
    : connection_engine(h, opts), socket_(connect(u))
{
    init();
}

size_t socket_engine::io_read(char *buf, size_t size) {
    ssize_t n = ::read(socket_, buf, size);
    if (n > 0)
        return n;
    if (n == 0)
        throw proton::closed_error();
    if (errno == EAGAIN || errno == EWOULDBLOCK)
        return 0;
    check(n, "read: ");
    return n;
}

size_t socket_engine::io_write(const char *buf, size_t size) {
    ssize_t n = ::write(socket_, buf, size);
    if (n == EAGAIN || n == EWOULDBLOCK) return 0;
    if (n < 0) check(n, "write: ");
    return n;
}

void socket_engine::io_close() { ::close(socket_); }

void socket_engine::run() {
    fd_set self;
    FD_ZERO(&self);
    FD_SET(socket_, &self);
    while (!closed()) {
        process();
        if (!closed()) {
            int n = select(FD_SETSIZE,
                           can_read() ? &self : NULL,
                           can_write() ? &self : NULL,
                           NULL, NULL);
            check(n, "select: ");
        }
    }
}

namespace {
struct auto_addrinfo {
    struct addrinfo *ptr;
    auto_addrinfo() : ptr(0) {}
    ~auto_addrinfo() { ::freeaddrinfo(ptr); }
    addrinfo* operator->() const { return ptr; }
};
}

descriptor connect(const proton::url& u) {
    descriptor fd = INVALID_DESCRIPTOR;
    try{
        auto_addrinfo addr;
        gai_check(::getaddrinfo(u.host().empty() ? 0 : u.host().c_str(),
                                u.port().empty() ? 0 : u.port().c_str(),
                                0, &addr.ptr), u.str()+": ");
        fd = check(::socket(addr->ai_family, SOCK_STREAM, 0), "connect: ");
        check(::connect(fd, addr->ai_addr, addr->ai_addrlen), "connect: ");
        return fd;
    } catch (...) {
        if (fd >= 0) close(fd);
        throw;
    }
}

listener::listener(const std::string& host, const std::string &port) : socket_(INVALID_DESCRIPTOR) {
    try {
        auto_addrinfo addr;
        gai_check(::getaddrinfo(host.empty() ? 0 : host.c_str(),
                                port.empty() ? 0 : port.c_str(), 0, &addr.ptr),
                  "listener address invalid: ");
        socket_ = check(::socket(addr->ai_family, SOCK_STREAM, 0), "listen: ");
        int yes = 1;
        check(setsockopt(socket_, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(yes)), "setsockopt: ");
        check(::bind(socket_, addr->ai_addr, addr->ai_addrlen), "bind: ");
        check(::listen(socket_, 32), "listen: ");
    } catch (...) {
        if (socket_ >= 0) close(socket_);
        throw;
    }
}

listener::~listener() { ::close(socket_); }

descriptor listener::accept(std::string& host_str, std::string& port_str) {
    struct sockaddr_in addr;
    ::memset(&addr, 0, sizeof(addr));
    socklen_t size = sizeof(addr);
    int fd = check(::accept(socket_, (struct sockaddr *)&addr, &size), "accept: ");
    char host[NI_MAXHOST], port[NI_MAXSERV];
    gai_check(getnameinfo((struct sockaddr *) &addr, sizeof(addr),
                          host, sizeof(host), port, sizeof(port), 0),
              "accept invalid remote address: ");
    host_str = host;
    port_str = port;
    return fd;
}

// Empty stubs, only needed on windows.
void initialize() {}
void finalize() {}
}}
