/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.qpid.proton.jms;

import javax.jms.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract public class JMSVendor {

    public abstract BytesMessage createBytesMessage();

    public abstract StreamMessage createStreamMessage();

    public abstract Message createMessage();

    public abstract TextMessage createTextMessage();

    public abstract ObjectMessage createObjectMessage();

    public abstract MapMessage createMapMessage();

    public abstract void setJMSXUserID(Message msg, String value);

    @Deprecated
    public Destination createDestination(String name) {
        return null;
    }

    @SuppressWarnings("deprecation")
    public <T extends Destination> T createDestination(String name, Class<T> kind) {
        return kind.cast(createDestination(name));
    }

    public abstract void setJMSXGroupID(Message msg, String groupId);

    public abstract void setJMSXGroupSequence(Message msg, int i);

    public abstract void setJMSXDeliveryCount(Message rc, long l);

    public abstract String toAddress(Destination msgDestination);

}
