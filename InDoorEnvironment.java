package decision;

import java.util.ArrayList;
import java.util.Collections;

import org.graphstream.algorithm.generator.BaseGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.GridGenerator;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import processing.core.PApplet;
import processing.core.PVector;

public class InDoorEnvironment extends PApplet {

	ArriveShape s;
	ArriveShape monster;
	float delta_time = 1;
	boolean arrive = false;
	static Graph graph;
	static int tile_size = 50;
	ArrayList<String> desiredPath = new ArrayList<String>();
	String start = "0_0";
	String goal = "0_0";
	int currentParam = 0;
	PVector target = new PVector(75, this.height - 75);
	DecisionTreeNode decisionTree = new DecisionTreeNode("wander");

	// level 0
	Selector select = new Selector();

	// level 1
	Sequence sequence = new Sequence();
	AtCenter atCenterTask = new AtCenter();

	// level 2
	RandomSelector randomTask = new RandomSelector();
	MonsterWander wanderTask1 = new MonsterWander();
	CharacterNear nearTask1 = new CharacterNear();

	// level 3
	CharacterNear nearTask2 = new CharacterNear();
	SpinDance spinTask = new SpinDance();
	MoveToCenter moveCenterTask = new MoveToCenter();
	MonsterWander wanderTask2 = new MonsterWander();

	public static void main(final String[] args) {
		PApplet.main("decision.InDoorEnvironment");
	}

	@Override
	public void settings() {
		size(1000,1000);
	}

	@Override
	public void setup() {
		s = new ArriveShape(this);
		monster = new ArriveShape(this);
		monster.position = new PVector(500, 500);

		desiredPath = new ArrayList<String>();
		graph = new SingleGraph("grid");
		Generator gen = new GridGenerator(false, false, true, false);

		gen.addSink(graph);
		gen.begin();

		for(int i = 0; i < 20; i++) {
			((BaseGenerator) gen).addEdgeAttribute("weight");
			((BaseGenerator) gen).setEdgeAttributesRange(50, 50);
			gen.nextEvents();
		}

		gen.end();

		// decision tree setup
		decisionTree.cond1 = new PositionRegion(0, 200, 800, 1000);
		decisionTree.cond2 = new PositionRegion(800, 1000, 0, 200);

		decisionTree.trueNode = new DecisionTreeNode("center");
		decisionTree.trueNode.cond1 = new PositionRegion(475, 525, 475, 525);
		decisionTree.trueNode.cond2 = decisionTree.trueNode.cond1;
		DecisionTreeNode ttrueNode = new DecisionTreeNode("return");
		DecisionTreeNode tfalseNode = new DecisionTreeNode("center");
		decisionTree.trueNode.trueNode = ttrueNode;
		decisionTree.trueNode.falseNode = tfalseNode;

		decisionTree.falseNode = new DecisionTreeNode("spin");

		// monster behavior tree setup

		// add children
		select.children.add(atCenterTask);
		select.children.add(sequence);

		sequence.children.add(wanderTask1);
		sequence.children.add(nearTask1);
		sequence.children.add(randomTask);

		randomTask.children.add(moveCenterTask);
		randomTask.children.add(wanderTask2);
		randomTask.children.add(spinTask);
	}

	@Override
	public void draw() {
		background(209);

		// character decision tree
		String action = decisionTree.makeDecision();

		boolean seek = true;
		//		System.out.println(action);
		if (action.equals("wander")) {
			s.wander();
			seek = true;
		} else if (action.equals("center")) {
			calculatePath(500, 500);
			int pathOffset = 1;
			if (arrive && desiredPath != null) {
				currentParam = getPathParam(s.position);
				int targetParam = currentParam + pathOffset;
				PVector new_target = getPositionFromPath(targetParam);
				if (new_target != null) {
					target.set(new_target);
				}
				s.arrive2(200, 1, 1, 1, target);
			}
			seek = true;
		} else if (action.equals("return")) {
			s.position = new PVector(75, 75);
			s.velocity = new PVector(0, 0);
			s.acceleration = new PVector(0, 0);
			seek = true;
		} else if (action.equals("spin")) {
			s.velocity = new PVector(0, 0);
			s.acceleration = new PVector(0, 0);
			s.angular_accel = 0;
			s.rotation = 3;
			seek = false;
		}

		select.run();

		s.applyUpdate(delta_time, seek);
		s.display();
		monster.applyUpdate(delta_time, seek);
		monster.display();

	}

	@Override
	public void mousePressed() {
		desiredPath = new ArrayList<String>();
		graph = new SingleGraph("grid");
		Generator gen = new GridGenerator(false, false, true, false);

		gen.addSink(graph);
		gen.begin();
		for(int i = 0; i < 20; i++) {
			((BaseGenerator) gen).addEdgeAttribute("weight");
			((BaseGenerator) gen).setEdgeAttributesRange(50, 50);
			gen.nextEvents();
		}

		gen.end();

		calculatePath(mouseX, mouseY);
	}

	public void calculatePath(float x, float y) {
		int tileX_index = (int) Math.floor(x / tile_size);
		int tileY_index = (int) Math.floor((this.height - y) / tile_size);

		goal = Integer.toString(tileX_index) + "_" + Integer.toString(tileY_index);
		//		graph.display();

		int startx_index = (int) Math.floor(s.position.x / tile_size);
		int starty_index = (int) Math.floor((this.height - s.position.y) / tile_size);
		start = startx_index + "_" + starty_index;
		if (goal != null && graph.getNode(goal) != null) {
			//			desiredPath = AStarPathFinder.findPath(graph, start, goal);
			desiredPath = DijkstraPathFinder.findPath(graph, start, goal);
			arrive = true;
		} else {
			System.out.println("This place can not be reached!");
			arrive = false;
		}
	}

	public int getPathParam(PVector position) {
		double distance = 9999;
		int currentPos = 0;
		for (int i = currentParam; i < desiredPath.size(); i++) {
			PVector targetPosition = getPositionFromPath(i);
			double newDistance = PVector.dist(position, targetPosition);
			if (newDistance < distance) {
				distance = newDistance;
				currentPos = i;
			}
		}
		return currentPos;
	}

	public PVector getPositionFromPath(int targetParam) {
		if (targetParam >= desiredPath.size()) {
			return null;
		}

		double x = 0, y = 0;
		Node n = graph.getNode(desiredPath.get(targetParam));
		for(String key : n.getEachAttributeKey()) {
			Double[] value = n.getAttribute(key);
			x = value[0] * 50 + 25;
			y = this.height - value[1] * 50 - 25;

		}
		return new PVector((float) x, (float) y);
	}

	class PositionRegion {
		float xmin;
		float xmax;
		float ymin;
		float ymax;

		public PositionRegion(float xmin, float xmax, float ymin, float ymax) {
			this.xmin = xmin;
			this.xmax = xmax;
			this.ymin = ymin;
			this.ymax = ymax;
		}
	}

	class DecisionTreeNode {
		DecisionTreeNode trueNode;
		DecisionTreeNode falseNode;
		PositionRegion cond1;
		PositionRegion cond2;
		String action;

		public DecisionTreeNode(String actionValue) {
			this.action = actionValue;
		}

		public String makeDecision() {
			boolean chooseTrue = cond1 != null && s.position.x > cond1.xmin && s.position.x < cond1.xmax && s.position.y > cond1.ymin && s.position.y < cond1.ymax;
			boolean chooseFalse = cond2 != null && s.position.x > cond2.xmin && s.position.x < cond2.xmax && s.position.y > cond2.ymin && s.position.y < cond2.ymax;
			if (chooseTrue) {
				this.action = trueNode.action;
				return trueNode.makeDecision();
			} else if (chooseFalse){
				this.action = falseNode.action;
				return falseNode.makeDecision();
			} else {
				return this.action;
			}
		}

	}

	class Task {
		ArrayList<Task> children;

		public boolean run() {
			return true;
		}
	}

	class CharacterNear extends Task {
		ArrayList<Task> children = new ArrayList<Task>();

		@Override
		public boolean run() {
			if (PVector.dist(monster.position, s.position) < 10) {
				monster.seek(s.position);
				return true;
			}
			return false;
		}
	}

	class AtCenter extends Task {
		ArrayList<Task> children = new ArrayList<Task>();

		@Override
		public boolean run() {
			if (PVector.dist(monster.position, new PVector(500, 500)) < 5) {
				monster.velocity = new PVector(0, 0);
				monster.acceleration = new PVector(0, 0);
				monster.angular_accel = 0;
				monster.rotation = 3;
				return true;
			}
			return false;
		}
	}

	class SpinDance extends Task {
		ArrayList<Task> children = new ArrayList<Task>();

		@Override
		public boolean run() {
			monster.velocity = new PVector(0, 0);
			monster.acceleration = new PVector(0, 0);
			monster.angular_accel = 0;
			monster.rotation = 3;
			return true;
		}
	}

	class MonsterWander extends Task {
		ArrayList<Task> children = new ArrayList<Task>();

		@Override
		public boolean run() {
			monster.wander();
			return true;
		}
	}

	class MoveToCenter extends Task {
		ArrayList<Task> children = new ArrayList<Task>();

		@Override
		public boolean run() {
			monster.arrive2(200, 1, 1, 1, new PVector(500, 500));
			return true;
		}
	}

	class Selector extends Task {
		ArrayList<Task> children = new ArrayList<Task>();

		@Override
		public boolean run() {
			for (int i = 0; i < children.size(); i++) {
				Task c = children.get(i);
				if(c.run()) {
					return true;
				}
			}
			return false;
		}
	}

	class Sequence extends Task {
		ArrayList<Task> children = new ArrayList<Task>();

		@Override
		public boolean run() {
			for (int i = 0; i < children.size(); i++) {
				Task c = children.get(i);
				if(!c.run()) {
					return false;
				}
			}
			return true;
		}
	}

	class RandomSelector extends Task {
		ArrayList<Task> children = new ArrayList<Task>();

		@Override
		public boolean run() {
			Collections.shuffle(children);
			boolean result = false;
			for (int i = 0; i < children.size(); i++) {
				result = children.get(i).run();
				if (!result) {
					break;
				}
			}
			return result;
		}
	}
}

