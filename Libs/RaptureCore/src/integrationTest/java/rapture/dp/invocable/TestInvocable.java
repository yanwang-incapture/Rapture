/**
 * Copyright (C) 2011-2015 Incapture Technologies LLC
 *
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */
package rapture.dp.invocable;

import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;
import rapture.kernel.Kernel;

public class TestInvocable extends AbstractInvocable {

    public TestInvocable(String workerURI) {
        super(workerURI);
    }

    @Override
    public String invoke(CallingContext context) {
        System.out.println("Hello from TestInvocable!");
        Kernel.getDecision().setContextLiteral(context, getWorkerURI(), "a3", "" + 35);
        Object aliasVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), "a3");
        Object rawVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), "amount");
        System.out.println(String.format("alias val = %s; raw val = %s;", aliasVal, rawVal));
        return "";
    }

}
