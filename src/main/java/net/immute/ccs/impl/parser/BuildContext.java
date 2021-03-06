package net.immute.ccs.impl.parser;

import net.immute.ccs.CcsProperty;
import net.immute.ccs.Origin;
import net.immute.ccs.impl.dag.DagBuilder;
import net.immute.ccs.impl.dag.Key;
import net.immute.ccs.impl.dag.Node;
import net.immute.ccs.impl.dag.Tally;

import java.util.HashSet;
import java.util.Set;

public abstract class BuildContext {
    protected final DagBuilder dag;

    abstract public Node traverse(SelectorLeaf selector);
    abstract public Node getNode();

    public BuildContext(DagBuilder dag) {
        this.dag = dag;
    }

    void addProperty(String name, Value value, Origin origin, boolean local, boolean override) {
        getNode().addProperty(name, new CcsProperty(value.toString(), origin, dag.nextProperty(), override), local);
    }

    public void addConstraint(Key key) {
        getNode().addConstraint(key);
    }

    public BuildContext descendant(Node node) {
        return new Descendant(dag, node);
    }

    // this one's actually a separate class since it's the root. we need to start someplace after all!
    public static class Descendant extends BuildContext {
        private final Node node;

        public Descendant(DagBuilder dag, Node node) {
            super(dag);
            this.node = node;
        }

        @Override public Node traverse(SelectorLeaf selector) {
            return selector.traverse(this);
        }

        @Override public Node getNode() {
            return node;
        }
    }

    private static abstract class TallyBuildContext extends BuildContext {
        private final Node firstNode;
        private final BuildContext baseContext;

        abstract protected Tally newTally(Node firstNode, Node secondNode);

        public TallyBuildContext(DagBuilder dag, Node node, BuildContext baseContext) {
            super(dag);
            this.firstNode = node;
            this.baseContext = baseContext;
        }

        @Override
        public Node traverse(SelectorLeaf selector) {
            Node secondNode = selector.traverse(baseContext);
            Set<Tally> tallies = new HashSet<Tally>();
            tallies.addAll(firstNode.getTallies());
            tallies.retainAll(secondNode.getTallies());
            assert tallies.isEmpty() || tallies.size() == 1;
            if (tallies.isEmpty()) {
                Tally tally = newTally(firstNode, secondNode);
                firstNode.addTally(tally);
                secondNode.addTally(tally);
                return tally.getNode();
            } else {
                return tallies.iterator().next().getNode();
            }
        }

        @Override
        public Node getNode() {
            return firstNode;
        }
    }

    public BuildContext disjunction(final Node node, final BuildContext baseContext) {
        return new TallyBuildContext(dag, node, baseContext) {
            @Override
            protected Tally.OrTally newTally(Node firstNode, Node secondNode) {
                return new Tally.OrTally(new Node(), new Node[] {firstNode, secondNode});
            }
        };
    }

    public BuildContext conjunction(final Node node, final BuildContext baseContext) {
        return new TallyBuildContext(dag, node, baseContext) {
            @Override
            protected Tally.AndTally newTally(Node firstNode, Node secondNode) {
                return new Tally.AndTally(new Node(), new Node[] {firstNode, secondNode});
            }
        };
    }
}
