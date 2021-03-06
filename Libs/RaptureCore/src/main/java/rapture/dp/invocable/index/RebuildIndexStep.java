/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.dp.invocable.index;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;
import rapture.repo.NVersionedRepo;
import rapture.repo.Repository;

public class RebuildIndexStep extends AbstractInvocable {
    private static Logger log = Logger.getLogger(RebuildIndexStep.class.getName());

    public RebuildIndexStep(String workerURI) {
        super(workerURI);
    }

    @Override
    public String invoke(CallingContext ctx) {
        log.info("Rebuilding index");
        KernelScript ks = new KernelScript();
        ks.setCallingContext(ctx); 
        String authority = ks.getDecision().getContextValue(getWorkerURI(), "Authority");
        log.info("Rebuilding for " + authority);
        Repository repo = Kernel.getKernel().getRepo(authority);
        if (repo != null && repo instanceof NVersionedRepo) {
            log.info("Rebuild ok because NVersionedRepo");
            NVersionedRepo nRepo = (NVersionedRepo) repo;
            nRepo.rebuild();
        } else {
            log.info("Not a valid repo for rebuilding");
        }
        return "ok";
    }

}
