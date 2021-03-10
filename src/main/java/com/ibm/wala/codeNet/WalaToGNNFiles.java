package com.ibm.wala.codeNet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.InterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.graph.traverse.DFS;

public class WalaToGNNFiles {

	static File outPath;
	
	static {
		outPath = new File(System.getProperty("outputDir"));
		assert outPath.isDirectory();

	}

	private static void withOutput(String outFile, Consumer<PrintWriter>  doit) {
		try (PrintWriter f = new PrintWriter(new FileWriter(new File(outPath, outFile)))) {
			doit.accept(f);
		} catch (IOException e) {
			assert false;
		}

	}
	
	public static void main(String... args) throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException, IOException {
		
		new SourceDirCallGraph().doit(args, (cg, builder, time) -> { 
			Collection<CGNode> roots = cg.getEntrypointNodes();
			assert roots.size() == 1 : roots;

			InterproceduralCFG ipcfg = new InterproceduralCFG(cg,
				n -> n.getMethod().getReference().getDeclaringClass().getClassLoader() == JavaSourceAnalysisScope.SOURCE || 
					 n == cg.getFakeRootNode() ||
					 n == cg.getFakeWorldClinitNode());
			BasicBlockInContext<ISSABasicBlock> entry = ipcfg.getEntry(roots.iterator().next());

			int dfsNumber = 0;
			Map<BasicBlockInContext<ISSABasicBlock>,Integer> dfsFinish = HashMapFactory.make();
			Iterator<BasicBlockInContext<ISSABasicBlock>> search = DFS.iterateFinishTime(ipcfg, new NonNullSingletonIterator<>(entry));
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
			Iterator<BasicBlockInContext<ISSABasicBlock>> reverseSearch = DFS.iterateDiscoverTime(ipcfg, new NonNullSingletonIterator<>(entry));
			while (reverseSearch.hasNext()) {
				dfsFinish.put(reverseSearch.next(), reverseDfsNumber++);
			} 

			// nodes files
			withOutput("num-node-list.csv.gz", f -> {
				f.println("" + dfsFinish.size());
			});
			
			// DFS order file
			withOutput("node_dfs_order.csv.gz", f -> {
				ipcfg.stream().filter(n -> dfsFinish.containsKey(n)).forEach(new Consumer<BasicBlockInContext<ISSABasicBlock>>() {
					public void accept(BasicBlockInContext<ISSABasicBlock> n) {
						f.println("" + dfsFinish.get(n));
					}
				});
				f.flush();
			});
		});
	}
}
