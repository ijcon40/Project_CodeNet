package com.ibm.wala.codeNet;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

import com.ibm.wala.analysis.nullpointer.IntraproceduralNullPointerAnalysis;
import com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.InterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;

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
