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
package rapture.script.reflex;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureScript;
import rapture.common.ScriptResult;
import rapture.common.api.ScriptingApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.ConfigLoader;
import rapture.index.IndexHandler;
import rapture.kernel.Kernel;
import rapture.kernel.pipeline.PipelineReflexSuspendHandler;
import rapture.kernel.script.KernelScript;
import rapture.log.MDCService;
import rapture.script.IActivityInfo;
import rapture.script.IRaptureScript;
import rapture.script.RaptureDataContext;
import rapture.script.ScriptRunInfoCollector;
import reflex.AddingOutputReflexHandler;
import reflex.DummyReflexOutputHandler;
import reflex.IReflexHandler;
import reflex.IReflexOutputHandler;
import reflex.ReflexException;
import reflex.ReflexExecutor;
import reflex.ReflexLexer;
import reflex.ReflexParser;
import reflex.ReflexTreeWalker;
import reflex.Scope;
import reflex.debug.NullDebugger;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

/**
 * Provides an execution context for Rapture Reflex Scripts
 * 
 * @author amkimian
 */
public class ReflexRaptureScript implements IRaptureScript {
    private static Logger log = Logger.getLogger(ReflexRaptureScript.class);

    private static final String EXCEPTION = "exception";
    private String auditLogUri = null;

    public void setAuditLogUri(String uri) {
        auditLogUri = uri;
    }

    private void addContextScope(ReflexTreeWalker walker, CallingContext context) {
        walker.currentScope.assign("_ctx", context == null ? new ReflexNullValue() : new ReflexValue(context));
        KernelScript kh = new KernelScript();
        kh.setCallingContext(context);
        walker.getReflexHandler().setApi(kh);
        // walker.currentScope.assign("_rk", new ReflexValue(kh));
        walker.currentScope.assign("_cfg", new ReflexValue(ConfigLoader.getConf()));
        // addStandard(walker, context, kh);
    }

    private void addObjectExtra(ReflexTreeWalker walker, Map<String, ?> extra) {

        walker.currentScope.assign("_params", new ReflexValue(extra));
    }

    private RaptureException generateError(RecognitionException e) {
        String message;
        if (e.getMessage() != null) {
            message = e.getMessage();
        } else {
            message = String.format("%s at line [%s], position [%s]", e.getClass().getName(), e.line, e.charPositionInLine);
        }
        Kernel.writeAuditEntry(EXCEPTION, 2, message);
        return RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error running script: " + message, e);
    }

    private ReflexTreeWalker getParserWithStandardContext(CallingContext context, String script, Map<String, ?> extra) throws RecognitionException {
        ReflexTreeWalker walker = getStandardWalker(context, script);
        if (extra != null && !extra.isEmpty()) {
            addObjectExtra(walker, extra);
        }
        addContextScope(walker, context);
        return walker;
    }

    public ReflexParser getParser(CallingContext ctx, String script) throws RecognitionException {
        ReflexLexer lexer = new ReflexLexer();
        lexer.dataHandler = new ReflexIncludeHelper(ctx);
        lexer.setCharStream(new ANTLRStringStream(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);
        parser.parse();
        return parser;
    }

    private ReflexTreeWalker getStandardWalker(CallingContext ctx, String script) throws RecognitionException {
        ReflexLexer lexer = new ReflexLexer();
        lexer.dataHandler = new ReflexIncludeHelper(ctx);
        lexer.setCharStream(new ANTLRStringStream(script));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ReflexParser parser = new ReflexParser(tokens);
        CommonTree tree;
        tree = (CommonTree) parser.parse().getTree();
        CommonTreeNodeStream nodes = new CommonTreeNodeStream(tree);
        ReflexTreeWalker walker = new ReflexTreeWalker(nodes, parser.languageRegistry);
        walker.setReflexHandler(new AddingOutputReflexHandler());
        walker.getReflexHandler().setOutputHandler(new SimpleCollectingOutputHandler());
        walker.getReflexHandler().setOutputHandler(new DummyReflexOutputHandler());
        walker.getReflexHandler().setDataHandler(new ReflexDataHelper(ctx));
        walker.getReflexHandler().setIOHandler(new BlobOnlyIOHandler());
        return walker;
    }

    @Override
    public boolean runFilter(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters) {

        // A filter is basically a program that returns true or false. No return
        // is equivalent to false
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), parameters);
            ReflexNode res = walker.walk();
            return res.evaluateWithoutScope(new NullDebugger()).asBoolean();
        } catch (RecognitionException e) {
            throw generateError(e);
        }
    }

    @Override
    public void runIndexEntry(CallingContext context, RaptureScript script, IndexHandler indexHandler, RaptureDataContext data) {
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), null);
            walker.currentScope.assign("_data", new ReflexValue(JacksonUtil.getHashFromObject(data)));
            walker.currentScope.assign("_index", new ReflexValue(indexHandler));
            ReflexNode res = walker.walk();
            res.evaluateWithoutScope(new NullDebugger());
        } catch (RecognitionException e) {
            throw generateError(e);
        }
    }

    @Override
    public List<Object> runMap(CallingContext context, RaptureScript script, RaptureDataContext data, Map<String, Object> parameters) {
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), parameters);
            walker.currentScope.assign("_data", new ReflexValue(JacksonUtil.getHashFromObject(data)));
            ReflexNode res = walker.walk();
            List<ReflexValue> ret = res.evaluateWithoutScope(new NullDebugger()).asList();
            List<Object> realRet = new ArrayList<Object>(ret.size());
            for (ReflexValue v : ret) {
                realRet.add(v.asObject());
            }
            return realRet;
        } catch (RecognitionException e) {
            throw generateError(e);
        }
    }

    @Override
    public String runOperation(CallingContext context, RaptureScript script, String ctx, Map<String, Object> params) {
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), params);
            walker.currentScope.assign("_ctx", new ReflexValue(ctx));
            ReflexNode res = walker.walk();
            return res.evaluateWithoutScope(new NullDebugger()).toString();
        } catch (RecognitionException e) {
            throw generateError(e);
        }
    }

    @Override
    public String runProgram(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> extraVals) {
        return runProgram(context, activity, script, extraVals, -1);
    }

    public String runProgram(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> extraVals, int timeout) {
        String ret = null;
        if (script == null) log.info("in runProgram: RaptureScript is null");
        else try {
            MDCService.INSTANCE.setReflexMDC(script.getName());
            // At this point we want to collect some information about the
            // running
            // script and use that to generate a blob that we can
            // record to the system blob.
            // it will contain information about who run this script, what
            // script it was
            // when it was run
            // the output from the script
            // the instrumentation from the script

            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), extraVals);
            ReflexNode res = walker.walk();
            ScriptRunInfoCollector collector = ScriptRunInfoCollector.createServerCollector(context, script.getAddressURI().getFullPath());
            ProgressDebugger progress = (timeout < 0) ? new TimeoutReflexDebugger(activity, script.getScript(), timeout) : new ProgressDebugger(activity,
                    script.getScript());
            log.info("Running script " + getScriptName(script));
            ReflexExecutor.injectSystemIntoScope(walker.currentScope);
            if (timeout <= 0) {
                ret = res.evaluateWithoutScope(progress).toString();
            } else {
                // TODO replace this with abortable invocation
                ret = res.evaluateWithoutScope(progress).toString();
            }
            for (Map.Entry<String, Object> val : extraVals.entrySet()) {
                log.debug("Looking to inject " + val.getKey());
                ReflexValue v = walker.currentScope.resolve(val.getKey());
                if (v != null && v.getValue() != ReflexValue.Internal.VOID && v.getValue() != ReflexValue.Internal.NULL) {
                    log.debug("Injecting " + v.asObject() + " as " + val.getKey());
                    val.setValue(v.asObject());
                }
            }
            progress.getInstrumenter().log();
            if (walker.getReflexHandler() instanceof AddingOutputReflexHandler) {
                AddingOutputReflexHandler aorf = (AddingOutputReflexHandler) walker.getReflexHandler();
                SimpleCollectingOutputHandler sc = aorf.getOutputHandlerLike(SimpleCollectingOutputHandler.class);
                collector.addOutput(sc.getLog());
            }
            collector.addInstrumentationLog(progress.getInstrumenter().getTextLog());
            // Now record collector
            if (auditLogUri != null) {
                Kernel.getAudit().writeAuditEntry(context, auditLogUri, "debug", 1, collector.getBlobContent());
            } else {
                ScriptCollectorHelper.writeCollector(context, collector);
            }
        } catch (RecognitionException e) {
            throw generateError(e);
        } finally {
            MDCService.INSTANCE.clearReflexMDC();
        }
        return ret;
    }

    private String getScriptName(RaptureScript script) {
        if (script.getAuthority() == null) {
            return script.getName();
        } else {
            return script.getStorageLocation().toString();
        }
    }

    @Override
    public ScriptResult runProgramExtended(CallingContext context, IActivityInfo activity, RaptureScript script, Map<String, Object> params) {
        ScriptResult res = new ScriptResult();
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), params);
            ScriptRunInfoCollector collector = ScriptRunInfoCollector.createServerCollector(context, "remote");
            ProgressDebugger progress = new ProgressDebugger(activity, script.getScript());
            // Setup an alternate output handler, and a standard data handler
            walker.getReflexHandler().setDataHandler(new ReflexDataHelper(context));
            walker.getReflexHandler().setOutputHandler(new ScriptResultOutputHandler(res));
            ReflexNode execRes = walker.walk();
            ReflexValue val = execRes.evaluateWithoutScope(progress);
            if (walker.getReflexHandler() instanceof AddingOutputReflexHandler) {
                AddingOutputReflexHandler aorf = (AddingOutputReflexHandler) walker.getReflexHandler();
                SimpleCollectingOutputHandler sc = aorf.getOutputHandlerLike(SimpleCollectingOutputHandler.class);
                collector.addOutput(sc.getLog());
            }
            collector.addInstrumentationLog(progress.getInstrumenter().getTextLog());
            // Now record collector
            ScriptCollectorHelper.writeCollector(context, collector);
            res.setReturnValue(val.asString());
        } catch (RecognitionException e) {
            throw generateError(e);
        } catch (ReflexException e) {
            res.setInError(true);
            res.setReturnValue(e.getMessage());
            res.getOutput().add("Error when running script");
        }
        return res;
    }

    public String runProgramWithScope(CallingContext context, String script, Scope s) throws RecognitionException {
        IReflexHandler handler = new ReflexHandler(context);
        ReflexTreeWalker walker = ReflexExecutor.getWalkerForProgram(script, handler);
        walker.setReflexHandler(handler);

        final StringBuilder sb = new StringBuilder();
        walker.getReflexHandler().setOutputHandler(new IReflexOutputHandler() {

            @Override
            public boolean hasCapability() {
                return true;
            }

            @Override
            public void printLog(String text) {
                sb.append(text);
            }

            @Override
            public void printOutput(String text) {
                sb.append(text);
            }

            @Override
            public void setApi(ScriptingApi api) {

            }

        });
        ReflexNode res = walker.walk();
        res.evaluate(new NullDebugger(), s);
        s = walker.currentScope;
        return sb.toString();
    }

    public ReflexValue runProgram(CallingContext context, ReflexTreeWalker walker, IActivityInfo activity, Map<String, Object> extraVals) {
        walker.getReflexHandler().setDataHandler(new ReflexDataHelper(context));
        walker.currentScope.assign("_params", new ReflexValue(extraVals));
        ReflexNode res;
        try {
            res = walker.walk();
            return res.evaluateWithoutScope(new ProgressDebugger(activity, ""));
        } catch (RecognitionException e) {
            throw generateError(e);
        }
    }

    public String runProgramWithSuspend(CallingContext context, RaptureScript script, IActivityInfo activity, Map<String, Object> extraVals,
            PipelineReflexSuspendHandler suspendHandler, IReflexOutputHandler outputHandler) {
        try {
            ScriptResult result = new ScriptResult();
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), extraVals);
            walker.getReflexHandler().setSuspendHandler(suspendHandler);
            if (outputHandler != null) {
                walker.getReflexHandler().setOutputHandler(outputHandler);
            } else {
                walker.getReflexHandler().setOutputHandler(new ScriptResultOutputHandler(result));
            }
            ReflexNode res = walker.walk();
            ProgressDebugger progress = new ProgressDebugger(activity, script.getScript());
            String scriptName = getScriptName(script);
            if (scriptName == null) {
                log.info("Running anonymous Reflex script");
            } else {
                log.info(String.format("Running script with name '%s'", scriptName));
            }
            ReflexValue retVal = res.evaluateWithoutScope(progress);
            for (Map.Entry<String, Object> val : extraVals.entrySet()) {
                ReflexValue v = walker.currentScope.resolve(val.getKey());
                if (v != null && v.getValue() != ReflexValue.Internal.VOID && v.getValue() != ReflexValue.Internal.NULL) {
                    val.setValue(v.asObject());
                }
            }
            progress.getInstrumenter().log();
            return retVal.toString();
        } catch (RecognitionException e) {
            throw generateError(e);
        }
    }

    public String runProgramWithResume(CallingContext context, RaptureScript script, IActivityInfo activity, Map<String, Object> extraVals,
            PipelineReflexSuspendHandler suspendHandler, IReflexOutputHandler outputHandler, String scopeContext) {
        try {
            ReflexTreeWalker walker = getParserWithStandardContext(context, script.getScript(), extraVals);
            walker.getReflexHandler().setSuspendHandler(suspendHandler);
            walker.getReflexHandler().setOutputHandler(outputHandler);
            ReflexNode res = walker.walk();
            Scope scope = JacksonUtil.objectFromJson(scopeContext, Scope.class);
            ProgressDebugger progress = new ProgressDebugger(activity, script.getScript());
            log.info("Running script " + getScriptName(script));
            res.evaluateWithResume(progress, scope);
            for (Map.Entry<String, Object> val : extraVals.entrySet()) {
                ReflexValue v = walker.currentScope.resolve(val.getKey());
                if (v != null && v.getValue() != ReflexValue.Internal.VOID && v.getValue() != ReflexValue.Internal.NULL) {
                    val.setValue(v.asObject());
                }
            }
            progress.getInstrumenter().log();
            return JacksonUtil.jsonFromObject(res.getScope());
        } catch (RecognitionException e) {
            throw generateError(e);
        }
    }

    @Override
    public String validateProgram(CallingContext context, RaptureScript script) {
        try {
            // We call this as it parses the program and throws an exception if
            // the script
            // is not parseable.
            getStandardWalker(context, script.getScript());
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() == null) {
                return e.getClass().toString();
            } else {
                return e.getMessage();
            }
        }
        return "";
    }
}
