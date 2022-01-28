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
package hivemall.tools.math;

import hivemall.utils.hadoop.HiveUtils;
import hivemall.utils.math.MathUtils;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.UDFType;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorUtils;
import org.apache.hadoop.io.FloatWritable;

// @formatter:off
@Description(name = "sigmoid", value = "_FUNC_(x) - Returns 1.0 / (1.0 + exp(-x))", 
            extended = "WITH input as (\n" + 
                    "  SELECT 3.0 as x\n" + 
                    "  UNION ALL\n" + 
                    "  SELECT -3.0 as x\n" + 
                    ")\n" + 
                    "select \n" + 
                    "  1.0 / (1.0 + exp(-x)),\n" + 
                    "  sigmoid(x)\n" + 
                    "from\n" + 
                    "  input;\n" + 
                    "0.04742587317756678   0.04742587357759476\n" + 
                    "0.9525741268224334    0.9525741338729858")
// @formatter:on
@UDFType(deterministic = true, stateful = false)
public final class SigmoidGenericUDF extends GenericUDF {

    private PrimitiveObjectInspector argOI;

    @Override
    public ObjectInspector initialize(@Nonnull ObjectInspector[] argOIs)
            throws UDFArgumentException {
        if (argOIs.length != 1) {
            throw new UDFArgumentException("_FUNC_ takes 1 argument");
        }
        this.argOI = HiveUtils.asDoubleCompatibleOI(argOIs[0]);
        return PrimitiveObjectInspectorFactory.writableFloatObjectInspector;
    }

    @Nullable
    @Override
    public FloatWritable evaluate(@Nonnull DeferredObject[] arguments) throws HiveException {
        assert (arguments.length == 1) : "sigmoid takes 1 argument: " + arguments.length;
        DeferredObject arg0 = arguments[0];
        assert (arg0 != null);
        Object obj0 = arg0.get();
        if (obj0 == null) {
            return null;
        }
        double x = PrimitiveObjectInspectorUtils.getDouble(obj0, argOI);
        float v = (float) MathUtils.sigmoid(x);
        return new FloatWritable(v);
    }

    @Override
    public String getDisplayString(String[] children) {
        return "sigmoid(" + Arrays.toString(children) + ')';
    }

}
