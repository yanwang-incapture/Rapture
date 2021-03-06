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
package rapture.common.mime;

import java.util.HashMap;
import java.util.Map;

import rapture.common.RaptureTransferObject;

// Mime type application/vnd.rapture.reflex.script.resume

public class MimeReflexScriptResume implements RaptureTransferObject {
    public Map<String, Map<String, Object>> getContext() {
        return context;
    }

    public void setContext(Map<String, Map<String, Object>> context) {
        this.context = context;
    }

    public String getResumePoint() {
        return resumePoint;
    }

    public void setResumePoint(String resumePoint) {
        this.resumePoint = resumePoint;
    }

    public String getReflexScript() {
        return reflexScript;
    }

    public void setReflexScript(String reflexScript) {
        this.reflexScript = reflexScript;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    private String reflexScript;
    private Map<String, Object> parameters = new HashMap<String, Object>();
    private Map<String, Map<String, Object>> context = new HashMap<String, Map<String, Object>>();
    private String scopeContext;

    private String resumePoint = "";

    public static String getMimeType() {
        return "application/vnd.rapture.reflex.script.resume";
    }

    public String getScopeContext() {
        return scopeContext;
    }

    public void setScopeContext(String scopeContext) {
        this.scopeContext = scopeContext;
    }
}
