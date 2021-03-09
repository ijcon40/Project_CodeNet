package com.ibm.wala.codeNet;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.NodeManager;

public class WalaSPTGraph extends AbstractGraph<JSONObject> {
	NodeManager<JSONObject> nodes;
	private EdgeManager<JSONObject> edges;

	public WalaSPTGraph(JSONObject parseTreeJson) {
		nodes = new NodeManager<JSONObject>() {
			private Set<JSONObject> a = HashSetFactory.make();
			
			{
				parseTreeJson
					.getJSONObject("graph")
					.getJSONArray("nodes")
					.forEach(n -> a.add((JSONObject)n));
			}

			@Override
			public Stream<JSONObject> stream() {
				return a.stream();
			}

			@Override
			public int getNumberOfNodes() {
				return a.size();
			}

			@Override
			public void addNode(JSONObject n) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void removeNode(JSONObject n) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean containsNode(JSONObject n) {
				return a.contains(n);
			}
			
		};
		edges = new EdgeManager<JSONObject>() {
			private Map<JSONObject,Set<JSONObject>> forward = HashMapFactory.make();
			private Map<JSONObject,Set<JSONObject>> backward = HashMapFactory.make();
			
			{
				Map<Integer,JSONObject> idToNode = HashMapFactory.make();
				JSONArray nodes = parseTreeJson
					.getJSONObject("graph")
					.getJSONArray("nodes");
				for(int i = 0; i < nodes.length(); i++) {
					JSONObject node = nodes.getJSONObject(i);
					idToNode.put(node.getInt("id"), node);
				}
				parseTreeJson
					.getJSONObject("graph")
					.getJSONArray("edges")
					.forEach(n -> {
						JSONObject e = (JSONObject)n;
						JSONArray edge = e.getJSONArray("between");
						JSONObject src = idToNode.get(edge.getInt(0));
						JSONObject dst = idToNode.get(edge.getInt(1));
						if (! forward.containsKey(src)) {
							forward.put(src, HashSetFactory.make());
						}
						forward.get(src).add(dst);
						if (! backward.containsKey(dst)) {
							backward.put(dst, HashSetFactory.make());
						}
						backward.get(dst).add(src);
					});
			}

			@Override
			public Iterator<JSONObject> getPredNodes(JSONObject n) {
				return backward.get(n).iterator();
			}

			@Override
			public int getPredNodeCount(JSONObject n) {
				return backward.get(n).size();
			}

			@Override
			public Iterator<JSONObject> getSuccNodes(JSONObject n) {
				if (forward.containsKey(n)) {
					return forward.get(n).iterator();
				} else {
					return EmptyIterator.instance();
				}
			}

			@Override
			public int getSuccNodeCount(JSONObject N) {
				return forward.get(N).size();
			}

			@Override
			public void addEdge(JSONObject src, JSONObject dst) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void removeEdge(JSONObject src, JSONObject dst) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public void removeAllIncidentEdges(JSONObject node) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public void removeIncomingEdges(JSONObject node) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public void removeOutgoingEdges(JSONObject node) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean hasEdge(JSONObject src, JSONObject dst) {
				return forward.get(src).contains(dst);
			}
			
		};
	}

	@Override
	protected NodeManager<JSONObject> getNodeManager() {
		return nodes;
	}

	@Override
	protected EdgeManager<JSONObject> getEdgeManager() {
		return edges;
	}
}
