#ifndef PROTON_CPP_RECEIVER_H
#define PROTON_CPP_RECEIVER_H

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

#include "proton/export.hpp"
#include "proton/endpoint.hpp"
#include "proton/link.hpp"
#include "proton/types.h"
#include <string>

struct pn_connection_t;

namespace proton {

/// A link for receiving messages.
class receiver : public link {
  public:
    /// @cond INTERNAL
    receiver(pn_link_t* r=0) : link(r) {}
    /// @endcond

    /// Add credit to the link
    PN_CPP_EXTERN void flow(int count);
};

}

#endif // PROTON_CPP_RECEIVER_H
