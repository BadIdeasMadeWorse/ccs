package net.immute.ccs.impl.dag;

import net.immute.ccs.CcsProperty;
import net.immute.ccs.impl.SearchState;

import java.util.*;

import static java.util.Collections.emptyList;

public class Node {
    private final HashMap<Key, Node> children = new HashMap<Key, Node>();
    private final Set<Tally> tallies = new HashSet<Tally>();
    private final HashMap<String, List<CcsProperty>> props = new HashMap<String, List<CcsProperty>>();
    private final HashMap<String, List<CcsProperty>> localProps = new HashMap<String, List<CcsProperty>>();
    private final Key constraints = new Key();

    public Set<Tally> getTallies() {
        return tallies;
    }

    public void addTally(Tally tally) {
        tallies.add(tally);
    }

    public Node getChild(Key key) {
        return children.get(key);
    }

    public void addChild(Key key, Node child) {
        children.put(key, child);
    }

    public void getChildren(Key key, Specificity spec, SearchState searchState) {
        for (Map.Entry<Key, Node> entry : children.entrySet())
            if (entry.getKey().matches(key))
                entry.getValue().activate(spec.add(entry.getKey().getSpecificity()), searchState);
    }

    public List<CcsProperty> getProperty(String name, boolean locals) {
        List<CcsProperty> values = null;
        if (locals) values = localProps.get(name);
        if (values == null) values = props.get(name);
        if (values == null) values = emptyList();
        return values;
    }

    public void addProperty(String name, CcsProperty value, boolean isLocal) {
        HashMap<String, List<CcsProperty>> theProps = isLocal ? localProps : props;
        List<CcsProperty> values = theProps.get(name);
        if (values == null) {
            values = new ArrayList<CcsProperty>();
            theProps.put(name, values);
        }
        values.add(value);
    }

    public void addConstraint(Key key) {
        constraints.addAll(key);
    }

    public void activate(Specificity spec, SearchState searchState) {
        searchState.add(spec, this);
        searchState.constrain(constraints);
        for (Tally tally : this.tallies) tally.activate(this, spec, searchState);
    }
}
