package com.ibm.wala.codeNet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UTFDataFormatException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.InterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
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
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.traverse.BoundedBFSIterator;
import com.ibm.wala.util.graph.traverse.DFS;

public class WalaToGNNFiles {

	static File outPath;
	
	static {
		outPath = new File(System.getProperty("outputDir"));
		assert outPath.isDirectory();

	}

	private static void withOutput(String outFile, Consumer<PrintWriter>  doit) {
		try (PrintWriter f = new PrintWriter(new FileWriter(new File(outPath, outFile), true))) {
			doit.accept(f);
		} catch (IOException e) {
			assert false;
		}

	}
	
	private static Set<String> ipcfgFeatures(BasicBlockInContext<ISSABasicBlock> n) {
		Set<String> fss = HashSetFactory.make();
		n.iterator().forEachRemaining(inst -> { 
			if (inst instanceof SSAAbstractInvokeInstruction) {
				try {
					fss.add(((SSAAbstractInvokeInstruction) inst).getDeclaredTarget().getName().toUnicodeString());
				} catch (UTFDataFormatException e) {
					assert false : e;
				}
			} else if (inst instanceof SSANewInstruction) {
				fss.add(((SSANewInstruction)inst).getConcreteType().getName().toUnicodeString());
			}
		});
		return fss;
	}

	public static void main(String... args) throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException, IOException {
		
		new SourceDirCallGraph().doit(args, (cg, builder, time) -> { 
			if (Boolean.getBoolean("SDG")) {
				sdgToGNNFiles(cg, builder);
			} else {
				ipcfgToGNNFiles(cg, builder);
			}
		});
	}

	private static Set<String> sdgFeatures(Statement n) {
		if (n instanceof MethodEntryStatement) {
			return Collections.singleton("entry " + n.getNode().getMethod().getName());
		} else if (n instanceof MethodExitStatement) {
			return Collections.singleton("exit " + n.getNode().getMethod().getName());
		} else if (n instanceof PhiStatement || 
				   n instanceof ParamCaller || 
				   n instanceof ParamCallee ||
				   n instanceof NormalReturnCallee ||
				   n instanceof NormalReturnCaller) {
			return Collections.singleton("flow");
		} else if (n instanceof NormalStatement) {
			SSAInstruction inst = ((NormalStatement)n).getInstruction();
			if (inst instanceof SSABinaryOpInstruction) {
				return Collections.singleton(((SSABinaryOpInstruction)inst).getOperator().toString());
			} else if (inst instanceof SSAUnaryOpInstruction) {
				return Collections.singleton(((SSAUnaryOpInstruction)inst).getOpcode().toString());
			} else if (inst instanceof SSAConditionalBranchInstruction) {
				return Collections.singleton(((SSAConditionalBranchInstruction)inst).getOperator().toString());
			} else if (inst instanceof SSAAbstractInvokeInstruction) {
				return Collections.singleton(((SSAAbstractInvokeInstruction)inst).getDeclaredTarget().getName().toString());
			} else if (inst instanceof SSANewInstruction) {
				return Collections.singleton(((SSANewInstruction)inst).getConcreteType().getName().toString());
			} else {	
				return Collections.emptySet();
			}

		} else {	
			return Collections.emptySet();
		}
	}
	
	private static void sdgToGNNFiles(CallGraph cg, CallGraphBuilder<?> builder) {
	    SDG<? extends InstanceKey> sdg =
	    		new SDG<>(
	                cg,
	                ((PropagationCallGraphBuilder)builder).getPointerAnalysis(),
	                new AstJavaModRef<>(),
	                Slicer.DataDependenceOptions.NO_HEAP_NO_EXCEPTIONS,
	                Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);

		Graph<Statement> srcSdg = GraphSlicer.prune(sdg, 
			n -> n.getNode().getMethod().getReference().getDeclaringClass().getClassLoader() == JavaSourceAnalysisScope.SOURCE);

		Supplier<Iterator<Statement>> getEntries = 
			() -> cg.getEntrypointNodes().stream().map(n -> { return (Statement)new MethodEntryStatement(n); }).iterator();
	
		writeGraph(getEntries, srcSdg, WalaToGNNFiles::sdgFeatures);
		
		System.err.println(srcSdg);
	}
	
	private static void ipcfgToGNNFiles(CallGraph cg, CallGraphBuilder<?> builder) {
		Collection<CGNode> roots = cg.getEntrypointNodes();
		assert roots.size() == 1 : roots;

		InterproceduralCFG full_ipcfg = new InterproceduralCFG(cg,
			n -> n.getMethod().getReference().getDeclaringClass().getClassLoader() == JavaSourceAnalysisScope.SOURCE || 
				 n == cg.getFakeRootNode() ||
				 n == cg.getFakeWorldClinitNode());
		BasicBlockInContext<ISSABasicBlock> entry = full_ipcfg.getEntry(roots.iterator().next());

		Graph<BasicBlockInContext<ISSABasicBlock>> ipcfg = 
			GraphSlicer.prune(full_ipcfg, 
				n -> n.getMethod().getReference().getDeclaringClass().getClassLoader() == JavaSourceAnalysisScope.SOURCE);
				
		Supplier<Iterator<BasicBlockInContext<ISSABasicBlock>>> entryPoints = 
				() -> new FilterIterator<>(ipcfg.iterator(), n -> n.equals(entry) || (n.isEntryBlock() && n.getMethod().isClinit()));

		writeGraph(entryPoints, ipcfg, WalaToGNNFiles::ipcfgFeatures);
	}

	private static <T> void writeGraph(Supplier<Iterator<T>> entryPoints, 
			Graph<T> ipcfg,
			Function<T, Set<String>> features) 
	{
		int dfsNumber = 0;
		Map<T,Integer> dfsFinish = HashMapFactory.make();
		Iterator<T> search = DFS.iterateFinishTime(ipcfg, entryPoints.get());
		while (search.hasNext()) {
			dfsFinish.put(search.next(), dfsNumber++);
		} 			

		int reverseDfsNumber = 0;
		Map<T,Integer> dfsStart = HashMapFactory.make();
		Iterator<T> reverseSearch = DFS.iterateDiscoverTime(ipcfg, entryPoints.get());
		while (reverseSearch.hasNext()) {
			dfsStart.put(reverseSearch.next(), reverseDfsNumber++);
		} 

		Map<T,Integer> bfsDepth = HashMapFactory.make();
		BoundedBFSIterator<T> depths = new BoundedBFSIterator<>(ipcfg, entryPoints.get(), 1000);
		while(depths.hasNext()) {
			T n = depths.next();
			bfsDepth.put(n, depths.getCurrentHops());
		}
		
		// nodes files
		withOutput("num-node-list.csv", f -> {
			f.println("" + dfsFinish.size());
		});
		
		withOutput("node_dfs_order.csv", f -> {
			ipcfg.stream()
				.filter(n -> dfsFinish.containsKey(n))
				.sorted((l, r) -> dfsFinish.get(l) - dfsFinish.get(r))
				.forEach(n -> f.println("" + dfsFinish.get(n)));
			f.flush();
		});

		withOutput("node_depth.csv", f -> {
			ipcfg.stream()
			.filter(n -> dfsFinish.containsKey(n))
			.sorted((l, r) -> dfsFinish.get(l) - dfsFinish.get(r))
			.forEach(n -> { 
				assert bfsDepth.containsKey(n) : n;
				f.println("" + bfsDepth.get(n));
			});
			f.flush();
		});

		withOutput("node-feat", f -> {
			ipcfg.stream()
			.filter(n -> dfsFinish.containsKey(n))
			.sorted((l, r) -> dfsFinish.get(l) - dfsFinish.get(r))
			.forEach(n -> {
				if (features.apply(n).isEmpty()) {
					f.print("none");
				} else {
					features.apply(n).forEach(s -> f.print(s + " "));
				}
				f.println();
			});
			f.flush();
		});

		withOutput("node_is_attributed.csv", f -> {
			ipcfg.stream()
				.filter(n -> dfsFinish.containsKey(n))
				.sorted((l, r) -> dfsFinish.get(l) - dfsFinish.get(r))
				.forEach(n -> {
				f.println(features.apply(n).isEmpty()?  0: 1);
			});
			f.flush();
		});

		withOutput("node_doc.txt", f -> {
			ipcfg.stream()
				.filter(n -> dfsFinish.containsKey(n))
				.sorted((l, r) -> dfsFinish.get(l) - dfsFinish.get(r))
				.forEach(n -> f.println(dfsFinish.get(n) + ":" + n));
			f.flush();
		});

		// edge files
		class EdgeProcessor {
			void doit(BiFunction<T,T,Void> edges) {
				ipcfg.stream()
				.filter(p -> dfsFinish.containsKey(p))
				.forEach(p -> { 
					ipcfg.getSuccNodes(p).forEachRemaining(s -> {
						if (dfsFinish.containsKey(s) &&
								!( (dfsStart.get(p) >= dfsStart.get(s)) &&	
								   (dfsFinish.get(p) <= dfsFinish.get(s)) )) {
							edges.apply(p, s);
						}
					});
				});
			}
		};
		
		withOutput("edge.csv", f -> {
			new EdgeProcessor().doit((p, s) -> {
				f.println(dfsFinish.get(p) + "," + dfsFinish.get(s));
				return null;
			});
		});

		withOutput("num-edge-list.csv", f -> {
			class Box {
				int i = 0;
			}
			Box b = new Box();
			new EdgeProcessor().doit((p, s) -> {
				b.i++;
				return null;
			});
			f.println(b.i);
		});
		
		
		// graph files
		withOutput("graph-label", f -> {
			f.println("" + System.getProperty("graphLabel"));
		});
	}
}
