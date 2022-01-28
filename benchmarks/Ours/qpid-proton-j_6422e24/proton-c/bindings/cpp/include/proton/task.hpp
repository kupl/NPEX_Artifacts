#ifndef PROTON_CPP_TASK_H
#define PROTON_CPP_TASK_H

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

/// @cond INTERNAL
/// XXX needs more discussion
    
#include "proton/export.hpp"
#include "proton/object.hpp"

#include "proton/reactor.h"

namespace proton {

/// A task for timer events.
class task : public object<pn_task_t> {
  public:
    task(pn_task_t* t) : object<pn_task_t>(t) {}

    /// Cancel the scheduled task.
    PN_CPP_EXTERN void cancel();
};

}

/// @endcond

#endif // PROTON_CPP_TASK_H
