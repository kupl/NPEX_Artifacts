#ifndef CONFIG_HPP
#define CONFIG_HPP

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

/// @cond INTERNAL
/// XXX discuss

/// @file
///
/// Configuration macros.  They can be set via -D compiler options or
/// in code.
///
/// On a C++11 compliant compiler, all C++11 features are enabled by
/// default.  Otherwise they can be enabled or disabled separately
/// with -D on the compile line.

#ifndef PN_HAS_CPP11
#if (defined(__cplusplus) && __cplusplus >= 201100)
#define PN_HAS_CPP11 1
#else
#define PN_HAS_CPP11 0
#endif
#endif

#ifndef PN_HAS_STD_PTR
#define PN_HAS_STD_PTR PN_HAS_CPP11
#endif

#ifndef PN_HAS_LONG_LONG
#define PN_HAS_LONG_LONG PN_HAS_CPP11
#endif

#ifndef PN_HAS_STATIC_ASSERT
#define PN_HAS_STATIC_ASSERT PN_HAS_CPP11
#endif

#endif // CONFIG_HPP

/// @endcond
