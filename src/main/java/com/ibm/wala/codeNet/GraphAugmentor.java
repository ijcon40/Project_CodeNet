package com.ibm.wala.codeNet;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph;
import com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph.Processor;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;

public class GraphAugmentor {

	public static void main(String... args) throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException, IOException {
		Map<Integer, Pair<Integer, Integer>> tokenMap = HashMapFactory.make();
		CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(new FileReader(System.getProperty("tokenFile")));
		for (CSVRecord token : csvParser) {
			int id = Integer.valueOf(token.get("seqnr"));
			int startOffset = Integer.valueOf(token.get("start"));
			int endOffsetInclusive = Integer.valueOf(token.get("stop"));
			tokenMap.put(id, Pair.make(startOffset, endOffsetInclusive));
		}
		
		JSONObject parseTreeJson = 
			(JSONObject)new JSONTokener(new FileInputStream(System.getProperty("parseTreeFile")))
			.nextValue();

		Graph<JSONObject> parseTree = new WalaSPTGraph(parseTreeJson);
		
		(new SourceDirCallGraph()).doit(args, new Processor() {
			
			private Map<JSONObject, Pair<Integer, Integer>> locations = HashMapFactory.make();
			private SortedMap<Integer,Set<JSONObject>> offsetToNodes = new TreeMap<>(new Comparator<Integer>() {
				@Override
				public int compare(Integer o1, Integer o2) {
					return o1 - o2;
				}
			});
			
			private Pair<Integer,Integer> location(JSONObject node) {
				Pair<Integer,Integer> result;
				if (locations.containsKey(node)) {
					return locations.get(node);
				} else {
					if (node.getString("node-type").equals("Token")) {
						result = tokenMap.get(node.getInt("token-id"));
					} else {
						int start = Integer.MAX_VALUE;
						int end = Integer.MIN_VALUE;
						Iterator<JSONObject> ss = parseTree.getSuccNodes(node);
						while (ss.hasNext()) {
							Pair<Integer,Integer> s = location(ss.next());
							if (s.fst < start) {
								start = s.fst;
							}
							if (s.snd > end) {
								end = s.snd;
							}
						}
						result = Pair.make(start, end);
					}
					locations.put(node, result);
					for(int i = result.fst; i <= result.snd; i++) {
						if (! offsetToNodes.containsKey(i)) {
							offsetToNodes.put(i,  HashSetFactory.make());
						}
						
						offsetToNodes.get(i).add(node);
					}
					return result;
				}
			}
			
			@Override
			public void process(CallGraph cg, CallGraphBuilder<?> builder, long time) {
				parseTree.forEach(jsonNode -> { 
					location(jsonNode);
				});
				
				for (CGNode n : cg) {
					if (n.getMethod() instanceof AstMethod) {
						DefUse DU = n.getDU();
						DebuggingInformation DI = ((AstMethod)n.getMethod()).debugInfo();
						Set<Pair<JSONObject,JSONObject>> df = HashSetFactory.make();
						n.getIR().iterateAllInstructions().forEachRemaining(inst -> { 
							if (inst.iIndex() >= 0 && inst.getDef() > 0) {
								Position src = DI.getInstructionPosition(inst.iIndex());
								if (src != null) {
									for(int i = src.getFirstOffset(); i <= src.getLastOffset(); i++) {
										if (offsetToNodes.containsKey(i)) {											
											offsetToNodes.get(i).forEach(srcNode -> {
												if ("expression".equals(srcNode.get("type-rule-name"))) {
													JSONArray dataflowTo;
													if (srcNode.has("dataflow")) {
														dataflowTo = srcNode.getJSONArray("dataflow");
													} else {
														dataflowTo = new JSONArray();
														srcNode.put("dataflow", dataflowTo);
													}
													DU.getUses(inst.getDef()).forEachRemaining(new Consumer<SSAInstruction>() {
														private final Set<SSAInstruction> history = HashSetFactory.make();

														@Override
														public void accept(SSAInstruction succ) {
															if (! history.contains(succ)) {
																history.add(succ);
																if (succ instanceof SSAPhiInstruction) {
																	DU.getUses(succ.getDef()).forEachRemaining(ss -> accept(succ));
																}  else {
																	Position dst = DI.getInstructionPosition(succ.iIndex());
																	if (dst != null) {
																		for(int j = dst.getFirstOffset(); j <= dst.getLastOffset(); j++) {
																			if (offsetToNodes.containsKey(j)) {
																				offsetToNodes.get(j).forEach(dstNode -> {
																					if ("expression".equals(dstNode.get("type-rule-name"))) {
																						Pair<JSONObject, JSONObject> key = Pair.make(srcNode,  dstNode);
																						if (! df.contains(key)) {
																							df.add(key);
																							dataflowTo.put(dstNode.getInt("id"));
																						}
																					}
																				});
																			}
																		}
																	}
																}
															}
														}	
													});
												}
											});
										}
									}
								}
							}
						});
					}
				}
				
				try (Writer out = new FileWriter(System.getProperty("parseTreeFile").substring(0, System.getProperty("parseTreeFile").length()-5) + ".df.json")) {
					parseTreeJson.write(out, 4, 0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
