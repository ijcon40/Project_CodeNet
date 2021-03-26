package com.ibm.wala.codeNet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UTFDataFormatException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.InterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
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
	
	public static void main(String... args) throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException, IOException {
		
		new SourceDirCallGraph().doit(args, (cg, builder, time) -> { 
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
							
			int dfsNumber = 0;
			Map<BasicBlockInContext<ISSABasicBlock>,Integer> dfsFinish = HashMapFactory.make();
			Supplier<FilterIterator<BasicBlockInContext<ISSABasicBlock>>> entryPoints = 
				() -> new FilterIterator<>(ipcfg.iterator(), n -> n.equals(entry) || (n.isEntryBlock() && n.getMethod().isClinit()));
			Iterator<BasicBlockInContext<ISSABasicBlock>> search = DFS.iterateFinishTime(ipcfg, entryPoints.get());
			while (search.hasNext()) {
				dfsFinish.put(search.next(), dfsNumber++);
			} 			
			ipcfg.stream().filter(n -> dfsFinish.containsKey(n)).forEach(new Consumer<BasicBlockInContext<ISSABasicBlock>>() {
				int id = 0;
				public void accept(BasicBlockInContext<ISSABasicBlock> n) {
					n.setGraphNodeId(id++);
				}
			});

			int reverseDfsNumber = 0;
			Map<BasicBlockInContext<ISSABasicBlock>,Integer> dfsStart = HashMapFactory.make();
			Iterator<BasicBlockInContext<ISSABasicBlock>> reverseSearch = DFS.iterateDiscoverTime(ipcfg, entryPoints.get());
			while (reverseSearch.hasNext()) {
				dfsStart.put(reverseSearch.next(), reverseDfsNumber++);
			} 

			Map<BasicBlockInContext<ISSABasicBlock>,Integer> bfsDepth = HashMapFactory.make();
			BoundedBFSIterator<BasicBlockInContext<ISSABasicBlock>> depths = new BoundedBFSIterator<>(ipcfg, new NonNullSingletonIterator<>(entry), 1000);
			while(depths.hasNext()) {
				BasicBlockInContext<ISSABasicBlock> n = depths.next();
				bfsDepth.put(n, depths.getCurrentHops());
			}
			
			// nodes files
			withOutput("num-node-list.csv", f -> {
				f.println("" + dfsFinish.size());
			});
			
			withOutput("node_dfs_order.csv", f -> {
				ipcfg.stream().filter(n -> dfsFinish.containsKey(n)).forEach(n -> f.println("" + dfsFinish.get(n)));
				f.flush();
			});

			withOutput("node_depth.csv", f -> {
				ipcfg.stream().filter(n -> dfsFinish.containsKey(n)).forEach(n -> f.println("" + bfsDepth.get(n)));
				f.flush();
			});

			Function<BasicBlockInContext<ISSABasicBlock>, Set<String>> features = (n) -> {
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
			};

			withOutput("node-feat.csv", f -> {
				ipcfg.stream().filter(n -> dfsFinish.containsKey(n)).forEach(n -> {
					features.apply(n).forEach(s -> f.print(s + " "));
					f.println();
				});
				f.flush();
			});

			withOutput("node_is_attributed.csv", f -> {
				ipcfg.stream().filter(n -> dfsFinish.containsKey(n)).forEach(n -> {
					f.println(! features.apply(n).isEmpty());
				});
				f.flush();
			});
			
			// edge files
			class EdgeProcessor {
				void doit(BiFunction<BasicBlockInContext<ISSABasicBlock>,BasicBlockInContext<ISSABasicBlock>,Void> edges) {
					ipcfg.stream()
					.filter(p -> dfsFinish.containsKey(p))
					.forEach(p -> { 
						ipcfg.getSuccNodes(p).forEachRemaining(s -> {
							if (dfsFinish.containsKey(s) &&
									!( (dfsStart.get(p) > dfsStart.get(s)) &&	
									   (dfsFinish.get(p) < dfsFinish.get(s)) )) {
								edges.apply(p, s);
							}
						});
					});
				}
			};
			
			withOutput("edges.csv", f -> {
				new EdgeProcessor().doit((p, s) -> {
					f.println(dfsFinish.get(p) + "," + dfsFinish.get(s));
					return null;
				});
			});

			withOutput("num-edge-list.csv", f -> {
				Map<BasicBlockInContext<ISSABasicBlock>,Set<BasicBlockInContext<ISSABasicBlock>>> es = HashMapFactory.make();
				new EdgeProcessor().doit((p, s) -> {
						if (! es.containsKey(p)) {
							es.put(p, HashSetFactory.make());
						}
						es.get(p).add(s);
						return null;
				});
				ipcfg.stream()
						.filter(p -> dfsFinish.containsKey(p))
						.forEach(p -> { 
							f.println("" + (es.containsKey(p)? es.get(p).size(): 0));
						});
			});
			
			
			// graph files
			withOutput("graph-label.csv", f -> {
				f.println("" + System.getProperty("graphLabel"));
			});

			
		});
	}
}
