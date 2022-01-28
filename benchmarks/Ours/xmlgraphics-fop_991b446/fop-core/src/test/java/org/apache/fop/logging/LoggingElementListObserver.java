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

/* $Id$ */

package org.apache.fop.logging;

import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.layoutmgr.ElementListObserver.Observer;
import org.apache.fop.layoutmgr.ElementListUtils;
import org.apache.fop.layoutmgr.ListElement;

/**
 * <p>Logs all observed element lists.
 * </p>
 * <p>You can enable/disabled individual categories separately, for example for JDK 1.4 logging:
 * </p>
 * <p>org.apache.fop.logging.LoggingElementListObserver.level = INFO</p>
 * <p>org.apache.fop.logging.LoggingElementListObserver.table-cell.level = FINE</p>
 */
public class LoggingElementListObserver implements Observer {

    /** @see org.apache.fop.layoutmgr.ElementListObserver.Observer */
    public void observe(List elementList, String category, String id) {
        Log log = LogFactory.getLog(LoggingElementListObserver.class.getName() + "." + category);
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(" ");
        int len = (elementList != null ? ElementListUtils.calcContentLength(elementList) : 0);
        log.debug("ElementList: category=" + category + ", id=" + id + ", len=" + len + "mpt");
        if (elementList == null) {
            log.debug("<<empty list>>");
            return;
        }
        ListIterator tempIter = elementList.listIterator();
        ListElement temp;
        while (tempIter.hasNext()) {
            temp = (ListElement) tempIter.next();
            if (temp.isBox()) {
                log.debug(tempIter.previousIndex()
                        + ") " + temp);
            } else if (temp.isGlue()) {
                log.debug(tempIter.previousIndex()
                        + ") " + temp);
            } else {
                log.debug(tempIter.previousIndex()
                        + ") " + temp);
            }
            if (temp.getPosition() != null) {
                log.debug("            " + temp.getPosition());
            }
        }
        log.debug(" ");
    }

}
