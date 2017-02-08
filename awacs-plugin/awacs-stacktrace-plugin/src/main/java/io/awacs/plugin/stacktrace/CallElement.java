/**
 * Copyright 2016 AWACS Project.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.awacs.plugin.stacktrace;

import java.util.LinkedHashMap;

/**
 * element of CallStack
 * Created by pixyonly on 2/7/17.
 */
public class CallElement {

    private LinkedHashMap<String, CallElement> subElements;

    private String clazz;

    private String method;

    private int callCounter = 1;

    private long timestamp;

    private long elapsedTime = 0l;

    CallElement(String clazz, String method) {
        this.clazz = clazz;
        this.method = method;
        this.timestamp = System.currentTimeMillis();
        subElements = new LinkedHashMap<>();
    }

    public String id() {
        return clazz + "#" + method;
    }

    public CallElement end() {
        this.elapsedTime = System.currentTimeMillis() - this.timestamp;
        return this;
    }

    public CallElement callSub(CallElement callee) {
        CallElement yetValue = subElements.putIfAbsent(callee.id(), callee);
        if (yetValue != null) {
            yetValue.callCounter++;
            return yetValue;
        }
        return callee;
    }

    @Override
    public String toString() {
        return '{' +
                "\"caller\":\"" + id() + "\"," +
                "\"timestamp\":" + timestamp + "," +
                "\"elapsedTime\":" + elapsedTime + "," +
                "\"callCount\":" + callCounter + "," +
                "\"subMethods\":" + subElements.values().toString() +
                '}';
    }
}