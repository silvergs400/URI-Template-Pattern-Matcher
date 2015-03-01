package com.prodigi.object;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.prodigi.exception.TrieException;
import com.prodigi.service.UriTemplateValidator;
import org.apache.log4j.Logger;

/**
 * Node of PatternMatchingTrie
 *
 * @author Wilkin Cheung
 */
public class Node implements Serializable {

    // Pattern to test a string is partial wildcard or not
    public static final Pattern PARTIAL_WILDCARD_PATTERN = Pattern
            .compile(".*\\{(\\w+)?\\}.*");

    // required for (de-)serialization
    protected static final long serialVersionUID = 1L;

    // If URL template PARAM VALUE contains ANY of the following characters, then
    // no match
    //
    // Certain characters are not printable, they must be represented in hex. So
    // might as well use hex value everywhere.
    //
    // List of invalid character and their hex value is below:
    //
    // 00-1F (ascii control characters)
    // 7F (ascii control characters)
    // 20 (whitespace)
    // 80-FF (non-ascii characters)
    //
    // Reserved characters
    // 26 &
    // 2C ,
    // 2F /
    // 3A :
    // 3B ;
    // 3D =
    // 3F ?
    // 40 @
    //
    // Unsafe characters
    // 22 "
    // 3C <
    // 3E >
    // 23 #
    // 7B {
    // 7D }
    // 7C |
    // 5C \
    // 5E ^
    // 5B [
    // 5D ]
    // 60 `
    //
    private static String RESERVED_CHARS_HEX_VALUE =
            "\\x26\\x2C\\x2F\\x3A\\x3B\\x3D\\x3F\\x40";
    private static String UNSAFE_CHARS_HEX_VALUE =
            "\\x22\\x3C\\x3E\\x23\\x7B\\x7D\\x7C\\x5C\\x5E\\x5B\\x5D\\x60";
    private static String ASCII_CONTROL_CHARS_HEX_VALUE = "\\x00-\\x1F";
    private static String NON_ASCII_CHARS_HEX_VALUE = "\\x80-\\xFF";
    private static String WHITESPACE_CHARS_HEX_VALUE = "\\x20";

    // Pattern that represents invalid characters defined above
    // The idea is that, if ANY of invalid characters appear in the URL Template
    // param, then there is no match
    private static Pattern INVALID_CHARS_IN_PARAM = Pattern.compile(".*["
            + RESERVED_CHARS_HEX_VALUE + UNSAFE_CHARS_HEX_VALUE
            + NON_ASCII_CHARS_HEX_VALUE + WHITESPACE_CHARS_HEX_VALUE
            + ASCII_CONTROL_CHARS_HEX_VALUE + "].*");

    private static Logger logger = Logger.getLogger(Node.class);

    // Value of node
    private String value;

    // if PARTIAL_WILDCARD, then use pattern matching. Support one or more {}
    private UriTemplateValidator.UriTemplate uriTemplatePart = null;

    // @see Enum nodeType
    private NodeType nodeType;

    // Flag that indicates this node has a child value that is complete wildcard
    // For example, value={abc} then this flag = true
    // For example, value=vodfolder.(*,vodfolder);id={id} then this flag = false
    private boolean hasCompleteWildcardChild = false;

    // Set this flag if this is the last pattern in url (ie. leaf node)
    private boolean isLeaf = false;

    // Each node connects to child nodes through an Edge
    // In most use-case, an Edge is forward slash.
    private Map<Edge, Set<Node>> edgeToChildrenMap = new HashMap<>();

    // store param value, for later check for invalid chars
    // this variable is used only in comparing URL against URL Template.
    // moot point to mark it volatile actually (not used in assembling Trie) but
    // mark it as transient anyway
    private transient Map<String, String> paramValues = new LinkedHashMap<>();

    /**
     * Constructor. This method also figures out nodeType
     *
     * @param v    com.prodigi.object.Node value
     * @param edge right edge
     */
    Node(String v, Edge edge) {
        this.value = v;
        this.nodeType = getNodeType();

        debug(String.format("  nodeType=%s for value %s", nodeType, value));
    }

    /**
     * add child node to parent node
     *
     * @param parentNode parent com.prodigi.object.Node
     * @param value      child node value
     * @param isLast     is this the last node in template or url?
     * @param edge       Edge object
     * @return newly created child node
     */
    static Node addChildToParentNode(Node parentNode, String value,
                                            boolean isLast, Edge edge) {
        // **WILDCARD HANDLING
        // if child is complete wildcard, then it will consume all other complete
        // wildcard siblings
        // siblings properties and siblings' children will be merged into a single
        // node
        if (parentNode.isCompleteWildcard(value)) {
            debug(String.format("   value [%s] is complete wildcard", value));

            Node completeWildcardChild =
                    parentNode.getCompleteWildcardChildOrNull(edge);

            debug("     ->Check if existing children has wildcard...");

            // if no existing wildcard child, then consume all siblings, merge their
            // properties
            // if a property is true, then it stays true
            if (completeWildcardChild == null) {
                debug("      ->No wildcard child exists; creating new wildcard child");

                // initialize
                Node newWildcardChild = new Node(value, edge);
                newWildcardChild.nodeType = NodeType.COMPLETE_WILDCARD;
                newWildcardChild.isLeaf = isLast;

                debug(String.format("       ->newWildcardChild.value=%s, edge=%s",
                        value, edge));

                Iterator<Node> iter = parentNode.getChildren(edge).iterator();

                if (iter.hasNext() == false) {
                    debug("       ->No sibling to consolidate");
                } else {
                    debug("       ->Consolidating children nodes into single collection, looking up sibling with same edge");
                }
                while (iter.hasNext()) {
                    Node sibling = iter.next();
                    debug(String.format("         =>Sibling: value=%s, nodeType=%s",
                            sibling.value, sibling.nodeType));

                    if (sibling.nodeType == NodeType.COMPLETE_WILDCARD) {
                        // consolidate children of siblings
                        newWildcardChild.getChildren(edge)
                                .addAll(sibling.getChildren(edge));

                        // set properties
                        if (sibling.isLeaf) {
                            newWildcardChild.isLeaf = true;
                            debug("            Copied from sibling: newWildcardChild.isLast = true");
                        }
                        if (sibling.hasCompleteWildcardChild) {
                            newWildcardChild.hasCompleteWildcardChild = true;
                            debug("            Copied from sibling: newWildcardChild.hasCompleteWildcardChild = true");
                        }
                    }
                }

                parentNode.hasCompleteWildcardChild = true;

                // delete existing children, because new wildcard child makes them
                // obsolete
                parentNode.getChildren(edge).add(newWildcardChild);

                debug(String.format("       ->newWildcardChild.isLast=%s",
                        newWildcardChild.isLeaf));

                // for later pattern comparison to find invalid character
                newWildcardChild.uriTemplatePart = new UriTemplateValidator.UriTemplate(value);

                return newWildcardChild;
            } else {
                debug("      ->Found existing complete wildcard node...!");

                // Current node has a wildcard child and it is the only wildcard child.
                // do not create new child node. Instead, merge properties
                // If a property is true, then it stays true
                if (isLast) {
                    completeWildcardChild.isLeaf = true;
                }
                return completeWildcardChild;
            }
        }

        debug(String.format("   ->value [%s] is just regular child", value));

        // Just create a new node and set its properties
        Node child = new Node(value, edge);

        if (PARTIAL_WILDCARD_PATTERN.matcher(value).matches()) {
            debug(String
                    .format("     ->value [%s] is partial wildcard. Create child node, and pattern",
                            value));
            child.uriTemplatePart = new UriTemplateValidator.UriTemplate(value);
        }

        child.isLeaf = isLast;
        parentNode.getChildren(edge).add(child);
        return child;
    }

    /**
     * Check if invalid characters. Invalid character means reserved, unsafe,
     * whitespace, or ascii control characters.
     *
     * @param str String input
     * @return true if invalid characters found; false otherwise
     */
    private static boolean hasInvalidCharacter(String str) {
        boolean containsInvalidChars =
                INVALID_CHARS_IN_PARAM.matcher(str).matches();

        if (containsInvalidChars) {
            debug(String.format(" ***Invalid char found in string: %s", str));
        }

        return containsInvalidChars;
    }

    /**
     * Helper method for debugging
     *
     * @param s debug statement
     */
    private static void debug(String s) {
        // System.out.println(s);
        if (logger.isDebugEnabled()) {
            logger.debug(s);
        }
    }

    /**
     * Find nodeType based on value
     *
     * @return NodeType enum
     */
    private NodeType getNodeType() {
        if (value == null) {
            throw new TrieException("IN: getNodeType(): value null -- cannot determine NodeType");
        }

        // if nodeType is already set, then return it ASAP
        if (nodeType != null) {
            return nodeType;
        }

        // else find nodeType
        if (isCompleteWildcard(value)) {
            nodeType = NodeType.COMPLETE_WILDCARD;
        } else if (PARTIAL_WILDCARD_PATTERN.matcher(value).matches()) {
            nodeType = NodeType.PARTIAL_WILDCARD;
        } else { // default
            nodeType = NodeType.NOT_WILDCARD;
        }
        return nodeType;
    }

    /**
     * Check if value is complete wildcard or not.
     *
     * @param value com.prodigi.object.Node value
     * @return true if value is complete wildcard; false otherwise
     */
    private boolean isCompleteWildcard(String value) {
        return value.startsWith("{") && value.endsWith("}")
                && value.lastIndexOf('{') == 0;
    }

    /**
     * Get child nodes of current node for edge
     *
     * @param edge Edge
     * @return Set of child com.prodigi.object.Node
     */
    Set<Node> getChildren(Edge edge) {
        // if map does not already contains edge, then create the holding data
        // structure
        if (!edgeToChildrenMap.keySet().contains(edge)) {
            Set<Node> children = new HashSet<Node>();

            edgeToChildrenMap.put(edge, children);
        }
        return edgeToChildrenMap.get(edge);
    }

    /**
     * Add all child nodes from ALL edges.
     *
     * @return Set of child node
     */
    Set<Node> getAllChildrenForAllEdges() {
        Set<Node> combinedSetOfNode = new HashSet<>();

        for (Set<Node> oneSetOfNode : edgeToChildrenMap.values()) {
            combinedSetOfNode.addAll(oneSetOfNode);
        }
        return combinedSetOfNode;
    }

    /**
     * add child to current node
     *
     * @param child  child node value
     * @param isLast child node is last node of template or not?
     * @param edge   edge as String
     * @return newly added child com.prodigi.object.Node
     */
     Node addChildToCurrentNode(String child, boolean isLast, Edge edge) {
        return addChildToParentNode(this, child, isLast, edge);
    }

    /**
     * Getter for a node value
     *
     * @return value com.prodigi.object.Node's value
     */
    public String value() {
        return value;
    }

    /**
     * Return all edges for this node
     *
     * @return Set of Edge for this node
     */
    public Set<Edge> getAllEdges() {
        return edgeToChildrenMap.keySet();
    }

    /**
     * get complete wildcard child or return null if not found
     *
     * @param edge Edge object
     * @return Child node that is completeWildcardChild
     */
    public Node getCompleteWildcardChildOrNull(Edge edge) {
        Iterator<Node> iter = getChildren(edge).iterator();
        while (iter.hasNext()) {
            Node child = iter.next();
            if (child.nodeType == NodeType.COMPLETE_WILDCARD) {
                return child;
            }
        }
        return null;
    }

    /**
     * Is input node a child?
     *
     * @param urlNode input com.prodigi.object.Node
     * @param edge    Edge object
     * @return true if match; false otherwise
     */
    public boolean isAChild(Node urlNode, Edge edge) {
        debug("  IN: isAChild()");
        debug(String
                .format("   ->checking if urlNode=[%s] is child of [%s] (%s children):%s, nodeType=%s",
                        urlNode, value, getChildren(edge).size(), getChildren(edge)
                        .toString(), nodeType));
        debug(String.format("   ->hasCompleteWildcardChild=%s",
                hasCompleteWildcardChild));

        // shortcut for complete wildcard
        if (hasCompleteWildcardChild) {
            debug("    **completeWildcard, therefore a match");
            return true;
        }

        // traverse each child node, looking for a
        // match
        for (Node childNode : getChildren(edge)) {
            // exact match
            if (childNode.nodeType == NodeType.NOT_WILDCARD
                    && childNode.value.equals(urlNode.value)) {
                return true;
            }
            // partial match
            if (childNode.nodeType == NodeType.PARTIAL_WILDCARD
                    && childNode.matchPattern(urlNode.value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if value matches uri template using java pattern/matcher
     *
     * @param value a node value
     * @return true if match; false otherwise
     */
    private boolean matchPattern(String value) {
        try {
            return matchPatternThrowable(value);
        } catch (TrieException e) {
            return false;
        }
    }

    /**
     * Check if value matches uri template using java pattern/matcher
     *
     * @param value String value
     * @return true if match; false otherwise
     * @throws TrieException if invalid character found
     */
    private boolean matchPatternThrowable(String value) throws TrieException {
        debug(String.format("   IN: matchPattern for value [%s]", value));
        if (uriTemplatePart == null) {
            throw new RuntimeException("  ***uriTemplate cannot be null at this point*** ");
        }

        // save paramValues for later use
        paramValues = uriTemplatePart.match(value);

        if (paramValues == null || paramValues.size() == 0) {
            return false;
        }

        // throw com.prodigi.exception.TrieException if value has invalid characters
        for (String paramValue : paramValues.values()) {
            if (hasInvalidCharacter(paramValue)) {
                debug(String
                        .format("*******************has invalid char in paramValue=%s",
                                paramValue));
                throw new TrieException(String.format("Character not allowed in parameter: %s",
                        paramValue));
            }
        }
        return true;
    }

    /**
     * Helper method to find a child node based on value. Use exact match.
     *
     * @param childValue child node value
     * @param edge       Edge object
     * @return child node if found; null otherwise
     */
    Node getExactChildOrNull(String childValue, Edge edge) {
        return getChildOrNull(childValue, true, edge);
    }

    /**
     * Helper method to find a child node based on value. Do not use exact match.
     *
     * @param childValue child node value
     * @param edge       Edge object
     * @return child node if found; null otherwise
     */
    Node getChildUrlNodeOrNull(String childValue, Edge edge) {
        return getChildOrNull(childValue, false, edge);
    }

    /**
     * Helper method to find a child node based on value.
     *
     * @param inputValue    child node value
     * @param useExactMatch character-by-character comparison?
     * @param edge          Edge object
     * @return child node if found; null otherwise
     */
    public Node getChildOrNull(String inputValue, boolean useExactMatch, Edge edge) {
        Set<Node> setOfNodes;

        if (edge == Edge.ANY) {
            setOfNodes = getAllChildrenForAllEdges();
        } else {
            setOfNodes = getChildren(edge);
        }

        Iterator<Node> iter = setOfNodes.iterator();
        debug(String
                .format(" IN: getChildOrNull() for edge=%s, inputValue is %s, children.size is %s children): %s",
                        edge, inputValue, setOfNodes.size(), setOfNodes.toString()));

        while (iter.hasNext()) {
            Node child = iter.next();
            if (child.value.equals(inputValue)) {
                return child;
            }

            if (!useExactMatch && child.nodeType == NodeType.COMPLETE_WILDCARD
                    && child.matchPattern(inputValue)) {
                return child;
            }

            if (!useExactMatch && child.nodeType == NodeType.PARTIAL_WILDCARD
                    && child.matchPattern(inputValue)) {
                debug(String
                        .format("    => [%s] matches [%s]", child.value, inputValue));
                return child;
            }
        }
        return null;
    }

    /**
     * Check if current node is last element in URL template
     *
     * @return true if current node if last element in template
     */
    public boolean isLeaf() {
        return isLeaf;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return value;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    /**
     * NodeType
     *
     * @author Wilkin Cheung
     */
    public enum NodeType {
        NOT_WILDCARD, COMPLETE_WILDCARD, PARTIAL_WILDCARD
    }

    /**
     * Edge connects parent node to child node
     *
     * @author Wilkin Cheung
     */
    public static class Edge implements Serializable {
        public static final Edge FORWARD_SLASH = new Edge("/");
        public static final Edge DEFAULT = FORWARD_SLASH;
        // ANY is just a holder, not an actual Edge
        public static final Edge ANY = new Edge("ANY");
        // Special Edge for LAST node
        public static final Edge LAST = new Edge("LAST");
        // required for (de-)serialization
        protected static final long serialVersionUID = 12L;
        // value of edge
        private String value;

        /**
         * Constructor
         *
         * @param v edge value
         */
        Edge(String v) {
            this.value = v;
        }

        /**
         * Getter for edge value
         *
         * @return String edge value
         */
        public String value() {
            return value;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Edge [value=" + value + "]";
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Edge other = (Edge) obj;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }
    }
}
