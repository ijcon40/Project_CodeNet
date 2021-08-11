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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.ibm.wala.analysis.nullpointer.IntraproceduralNullPointerAnalysis;
import com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
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
import com.ibm.wala.ssa.*;
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

public class WalaNullPointerAnalysis {

    public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException, IOException {
        //params given here should be -mainClass=<program> -sourceDir=<program>
        //where program is the relative path from the codenet directory
        if (System.getProperty("readGraph") != null) {
            //readGraph(System.getProperty("readGraph"));
        } else {
            new SourceDirCallGraph().doit(args, (cg, builder, time) -> {
                //here we can use the context graph to get the nodes from the nullpointer analysis
                Collection<CGNode> nodes  = cg.getEntrypointNodes();
//                CGNode fakeRoot = cg.getFakeRootNode();
//                IR ir = fakeRoot.getIR();
//                IntraproceduralNullPointerAnalysis nullPointerAnalysis = new IntraproceduralNullPointerAnalysis(ir);
//
                InterproceduralCFG full_ipcfg = new InterproceduralCFG(cg,
                        n -> n.getMethod().getReference().getDeclaringClass().getClassLoader() == JavaSourceAnalysisScope.SOURCE ||
                                n == cg.getFakeRootNode() ||
                                n == cg.getFakeWorldClinitNode());

                for(CGNode cgnode: nodes){
                    IR ir = cgnode.getIR();
                    IntraproceduralNullPointerAnalysis nullPointerAnalysis = new IntraproceduralNullPointerAnalysis(ir);
                    BasicBlockInContext<ISSABasicBlock> entry = full_ipcfg.getEntry(cgnode);
                    for (SSAInstruction instruction : entry) {
                        nullPointerAnalysis.nullPointerExceptionThrowState(instruction);
                    }

                }
                Collection<CGNode> roots = cg.getEntrypointNodes();
                assert roots.size() == 1 : roots;

                BasicBlockInContext<ISSABasicBlock> entry = full_ipcfg.getEntry(roots.iterator().next());

                Graph<BasicBlockInContext<ISSABasicBlock>> ipcfg =
                        GraphSlicer.prune(full_ipcfg,
                                n -> n.getMethod().getReference().getDeclaringClass().getClassLoader() == JavaSourceAnalysisScope.SOURCE);

                Supplier<Iterator<BasicBlockInContext<ISSABasicBlock>>> entryPoints =
                        () -> new FilterIterator<>(ipcfg.iterator(), n -> n.equals(entry) || (n.isEntryBlock() && n.getMethod().isClinit()));

                //now we can iterate through and hopefully create Intra
                for (Iterator<BasicBlockInContext<ISSABasicBlock>> it = entryPoints.get(); it.hasNext(); ) {
                    BasicBlockInContext<ISSABasicBlock> node = it.next();
                    for(Iterator<SSAInstruction> instructions = node.iterator(); instructions.hasNext();) {
                        SSAInstruction instruction = instructions.next();
                        //IntraproceduralNullPointerAnalysis nullPointerAnalysis = new IntraproceduralNullPointerAnalysis(instruction.)
                    }


                }

            });
        }
    }
}
