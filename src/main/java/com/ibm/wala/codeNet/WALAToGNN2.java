package com.ibm.wala.codeNet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UTFDataFormatException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ibm.wala.analysis.nullpointer.IntraproceduralNullPointerAnalysis;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.cast.js.ipa.modref.JavaScriptModRef;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.python.modref.PythonModRef;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.util.SourceBuffer;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.InterproceduralCFG;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.MethodExitStatement;
import com.ibm.wala.ipa.slicer.NormalReturnCallee;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.ParamCaller;
import com.ibm.wala.ipa.slicer.PhiStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.labeled.LabeledGraph;
import com.ibm.wala.util.graph.labeled.SlowSparseNumberedLabeledGraph;
import com.ibm.wala.util.graph.traverse.BoundedBFSIterator;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.codeNet.nullpointer.*;

public class WALAToGNN2 {


	private static ClassLoaderReference loader(Statement s) {
		return s.getNode().getMethod().getDeclaringClass().getClassLoader().getReference();
	}

	public static void main(String... args) throws IllegalArgumentException, IOException, CancelException, WalaException {	
		if (System.getProperty("readGraph") != null) {
			//readGraph(System.getProperty("readGraph"));
		} else {

			Collection<ModuleEntry> srcFiles = Collections.singleton(new SourceFileModule(new File(args[0]), args[0].substring(args[0].lastIndexOf('/')+1), null));
			Collection<Module> src = Collections.singleton(new Module() {
				@Override
				public Iterator<? extends ModuleEntry> getEntries() {
					return srcFiles.iterator();
				}
			});

			ModRef<InstanceKey> mr = null;
			Predicate<Statement> keep = null;
			PropagationCallGraphBuilder builder = null;
			switch (args[0].substring(args[0].lastIndexOf('.'))) {
			case ".js":
				builder = JSWalaBuilder.makeBuilder(src);
				mr = new JavaScriptModRef<>();
				keep = (s) -> {
					boolean jsCode = loader(s) == JavaScriptTypes.jsLoader;
					if (!jsCode) {
						return false;
					}
					if (s.getNode().getMethod().toString().contains("prologue.js")) {
						return false;
					}
					System.err.println(s.getNode().getMethod());
					return true;
				};
				break;
			case ".java":
				builder = JavaWalaBuilder.makeBuilder(src);
				mr = new AstJavaModRef<>();
				keep = (s) -> loader(s) == JavaSourceAnalysisScope.SOURCE;
				break;
			case ".py":
				builder = PythonWalaBuilder.makeBuilder(src);
				mr = new PythonModRef();
				keep = (s) -> loader(s) == PythonTypes.pythonLoader;
				break;
			}

			CallGraph cg = builder.makeCallGraph(builder.getOptions());
			Collection<CGNode> entry = cg.getEntrypointNodes();
			for(CGNode node : entry) {
				IR i = node.getIR();
				IntraproceduralNullPointerAnalysis x = new IntraproceduralNullPointerAnalysis(i);
			}
			
		}
	}

	
	}


