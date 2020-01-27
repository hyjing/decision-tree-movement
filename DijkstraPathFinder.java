package decision;

import java.util.ArrayList;
import java.util.Collections;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class DijkstraPathFinder {
	public static NodeRecord smallestElement(ArrayList<NodeRecord> open) {
		int index = -1;
		int min = Integer.MAX_VALUE;

		for (int i = 0; i < open.size(); i++) {
			if (open.get(i).costSoFar <= min) {
				min = open.get(i).costSoFar;
				index = i;
			}
		}

		return open.get(index);
	}

	public static NodeRecord findNodeRecord(ArrayList<NodeRecord> list, String name) {
		for (NodeRecord node : list) {
			if (node.node.equals(name)) {
				return node;
			}
		}
		return null;
	}

	/**
	 * Find the path given graph, start and goal position
	 *
	 * @param g the graph given
	 * @param start the start point
	 * @param goal the destination point
	 * @return array of the path
	 */
	public static ArrayList<String> findPath(Graph g, String start, String goal) {
		NodeRecord startRecord = new NodeRecord();
		startRecord.node = start;
		startRecord.connection = null;
		startRecord.costSoFar = 0;

		ArrayList<NodeRecord> open = new ArrayList<NodeRecord>();
		ArrayList<NodeRecord> closed = new ArrayList<NodeRecord>();

		NodeRecord current = new NodeRecord();
		current.node = start;
		current.connection = null;
		current.costSoFar = 0;
		open.add(current);
		while(!open.isEmpty()) {
			current = smallestElement(open);
			if (current.node.equals(goal)) {
				break;
			}

			Iterable<Edge> connections = g.getNode(current.node).getEachEdge();
			for (Edge connection : connections) {
				// System.out.println(connection.getTargetNode().getId());
				String endNodeID = connection.getNode0().getId();
				String sourceNodeID = connection.getNode1().getId();
				if (endNodeID.equals(current.node)) {
					endNodeID = connection.getNode1().getId();
					sourceNodeID = connection.getNode0().getId();
				}
				Node endNode = g.getNode(endNodeID);
				int endNodeCost = current.costSoFar + (int) connection.getNumber("weight");
				NodeRecord endNodeRecord = findNodeRecord(open, endNode.getId());
				if (findNodeRecord(closed, endNode.getId()) != null) {
					continue;
				}
				else if (endNodeRecord != null) {
					if (endNodeRecord.costSoFar <= endNodeCost) {
						continue;
					}
				}
				else {
					endNodeRecord = new NodeRecord();
					endNodeRecord.node = endNode.getId();
				}
				endNodeRecord.costSoFar = endNodeCost;
				endNodeRecord.connection = sourceNodeID;

				if (findNodeRecord(open, endNode.getId()) == null) {
					open.add(endNodeRecord);
				}
			}

			open.remove(current);
			closed.add(current);
		}

		if (!current.node.equals(goal)) {
			return null;
		}
		else {
			ArrayList<String> path = new ArrayList<String>();
			while (current != null && !current.node.equals(start)) {
				path.add(current.node);
				current = findNodeRecord(closed, current.connection);
				//				System.out.println("a: " + current.node);
				//				System.out.println(current.connection);
			}
			path.add(start);

			Collections.reverse(path);
			return path;
		}
	}
}
