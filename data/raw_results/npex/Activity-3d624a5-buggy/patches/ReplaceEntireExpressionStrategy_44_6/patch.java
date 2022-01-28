/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.event;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;

/**
 * @author Daniel Meyer
 */
public abstract class AbstractEventHandler implements EventHandler {

  private static Logger log = Logger.getLogger(AbstractEventHandler.class.getName());
  
public void handleEvent(org.activiti.engine.impl.persistence.entity.EventSubscriptionEntity eventSubscription, java.lang.Object payload, org.activiti.engine.impl.interceptor.CommandContext commandContext) {
    org.activiti.engine.impl.persistence.entity.ExecutionEntity execution = eventSubscription.getExecution();
    org.activiti.engine.impl.pvm.process.ActivityImpl activity = eventSubscription.getActivity();
    if (activity == null) {
        throw new org.activiti.engine.ActivitiException((("Error while sending signal for event subscription '" + eventSubscription.getId()) + "': ") + "no activity associated with event subscription");
    }
    /* NPEX_PATCH_BEGINS */
    if (execution.getActivity() != null ? !execution.getActivity().equals(activity) : true) {
        execution.setActivity(activity);
    }
    if (payload instanceof java.util.Map) {
        @java.lang.SuppressWarnings("unchecked")
        java.util.Map<java.lang.String, java.lang.Object> processVariables = ((java.util.Map<java.lang.String, java.lang.Object>) (payload));
        execution.setVariables(processVariables);
    }
    if (activity.getActivityBehavior() instanceof org.activiti.engine.impl.bpmn.behavior.BoundaryEventActivityBehavior) {
        try {
            activity.getActivityBehavior().execute(execution);
        } catch (java.lang.RuntimeException e) {
            org.activiti.engine.impl.event.AbstractEventHandler.log.log(java.util.logging.Level.SEVERE, ("exception while sending signal for event subscription '" + eventSubscription) + "'", e);
            throw e;
        } catch (java.lang.Exception e) {
            org.activiti.engine.impl.event.AbstractEventHandler.log.log(java.util.logging.Level.SEVERE, ("exception while sending signal for event subscription '" + eventSubscription) + "'", e);
            throw new org.activiti.engine.ActivitiException((("exception while sending signal for event subscription '" + eventSubscription) + "':") + e.getMessage(), e);
        }
    } else {
        // not boundary
        execution.signal("signal", null);
    }
}
}
