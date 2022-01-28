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
#include "proton/condition.hpp"

#include "proton/condition.h"

namespace proton {

bool condition::operator!() const {
    return !pn_condition_is_set(condition_);
}

std::string condition::name() const {
    const char* n = pn_condition_get_name(condition_);
    return n ? n : "";
}

std::string condition::description() const {
    const char* d = pn_condition_get_description(condition_);
    return d ? d : "";
}

value condition::info() const {
    pn_data_t* t = pn_condition_info(condition_);
    return t ? t : value();
}

std::string condition::str() const {
    if (!*this) {
        return "No error condition";
    } else {
      const char* n = pn_condition_get_name(condition_);
      const char* d = pn_condition_get_description(condition_);
      std::string s;
      if (n) s += n;
      if (d) {
          if (n) s += ": ";
          s += d;
      }
      return s;
    }
}

}
