package net.immute.ccs;

import net.immute.ccs.impl.SearchState;
import net.immute.ccs.impl.dag.Key;
import net.immute.ccs.impl.dag.Node;

import java.util.concurrent.atomic.AtomicReference;

public class CcsContext {
    private final AtomicReference<SearchState> searchState = new AtomicReference<SearchState>();

    private final CcsContext parent;
    private final Key key;

    CcsContext(Node root, CcsLogger log) {
        searchState.set(new SearchState(root, this, log));
        parent = null;
        key = null;
    }

    private CcsContext(CcsContext parent, String element) {
        this.parent = parent;
        key = new Key(element);
    }

    private CcsContext(CcsContext parent, String element, String id) {
        this.parent = parent;
        key = new Key(element);
        key.setId(id);
    }

    private CcsContext(CcsContext parent, String element, String id,
                       String... classes) {
        this.parent = parent;
        key = new Key(element, classes);
        key.setId(id);
    }

    public CcsContext constrain(String element) {
        return new CcsContext(this, element);
    }

    public CcsContext constrain(String element, String id) {
        return new CcsContext(this, element, id);
    }

    public CcsContext constrain(String element, String id, String... classes) {
        return new CcsContext(this, element, id, classes);
    }

    public String getKey() {
        return key.toString();
    }

    public boolean hasProperty(String propertyName) {
        return findProperty(propertyName, true) != null;
    }

    public CcsProperty getProperty(String propertyName) {
        return findProperty(propertyName, true);
    }

    public String getString(String propertyName) {
        CcsProperty prop = getProperty(propertyName);
        if (prop == null) throw new NoSuchPropertyException(propertyName, this);
        return prop.getValue();
    }

    public String getString(String propertyName, String defaultValue) {
        CcsProperty property = getProperty(propertyName);
        String result = property == null ? defaultValue : property.getValue();
        return result;
    }

    public int getInt(String propertyName) {
        int result = Integer.parseInt(getString(propertyName));
        return result;
    }

    public int getInt(String propertyName, int defaultValue) {
        CcsProperty property = getProperty(propertyName);
        int result = property == null ? defaultValue : Integer.parseInt(property.getValue());
        return result;
    }

    public double getDouble(String propertyName) {
        double result = Double.parseDouble(getString(propertyName));
        return result;
    }

    public double getDouble(String propertyName, double defaultValue) {
        CcsProperty property = getProperty(propertyName);
        double result = property == null ? defaultValue : Double.parseDouble(property.getValue());
        return result;
    }

    public boolean getBoolean(String propertyName) {
        boolean result = Boolean.parseBoolean(getString(propertyName));
        return result;
    }

    public boolean getBoolean(String propertyName, boolean defaultValue) {
        CcsProperty property = getProperty(propertyName);
        boolean result = property == null ? defaultValue : Boolean.parseBoolean(property.getValue());
        return result;
    }

    private CcsProperty findProperty(String propertyName, boolean locals, boolean override) {
        // first, look in nodes newly matched by this pattern...
        CcsProperty prop = getSearchState().findProperty(propertyName, locals, override);
        if (prop != null) return prop;

        // if not, then inherit...
        if (parent != null) {
            return parent.findProperty(propertyName, false, override);
        }

        return null;
    }

    private CcsProperty findProperty(String propertyName, boolean locals) {
        CcsProperty prop = findProperty(propertyName, locals, true);
        if (prop == null) prop = findProperty(propertyName, locals, false);
        return prop;
    }

    private SearchState getSearchState() {
        if (searchState.get() == null) {
            SearchState tmp = parent.getSearchState().newChild(this);

            boolean includeDirectChildren = true;
            CcsContext p = parent;
            while (p != null) {
                // TODO be nice to replace parent with 'this', but it's really not clear that that's even possible...
                p.getSearchState().extend(key, parent, includeDirectChildren, tmp);
                includeDirectChildren = false;
                p = p.parent;
            }

            searchState.compareAndSet(null, tmp);
        }

        return searchState.get();
    }

    @Override
    public String toString() {
        if (parent != null) {
            if (parent.parent != null)
                return parent + " > " + key;
            else
                return key.toString();
        } else {
            return "<root>";
        }
    }
}
