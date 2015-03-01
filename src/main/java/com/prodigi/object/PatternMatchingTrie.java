package com.prodigi.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.prodigi.object.Node.Edge;
import org.apache.log4j.spi.LoggerFactory;

/**
 * PatternMatchingTrie data structure. See the code below for description.
 *
 * @param <E> com.prodigi.object.Node element
 * @author Wilkin Cheung
 * @see http://en.wikipedia.org/wiki/Radix_tree
 * @see http://tools.ietf.org/html/rfc6570
 */
public class PatternMatchingTrie<E extends Node> implements Serializable {

    // PatternMatchingTrie data structure
    //
    // This implementation of Trie is very specialized N-ary tree structure and
    // intended to organize thousands of URL templates into a single tree
    // structure, per apiKey.
    //
    // Walking a Trie from root Node to any bottom Node will re-construct a template.
    //
    //
    // Prior to using Trie, incoming URL is compared against each template. If
    // there are thousands of templates, then it will take up to thousands
    // comparison of java pattern.
    //
    // The advantage of using Trie over List is performance. The more templates live in a
    // trie, the better the performance gain. During runtime,
    // when determining if incoming URL matches one of the templates,
    // it takes O(log k) to walk down the tree where k is number of URL node.
    // Each URL and URL Template can be expressed in a set of Nodes.
    // Each node begins and ends with the forward slash character '/'.
    //
    // For example, if URL Template looks like:
    //
    // "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id}
    //
    // Then, there are 6 nodes. From left to right. First node is "puppy-ws".
    // Last node is {id}
    //
    // In Condor, Template means URL Template Level 1. Each URL Template contains
    // one or more curly braces pair.
    //
    // Example of Level 1 URL template example is:
    //
    // "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}"
    //
    // A matching URL example is:
    //
    // "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=5,6"
    //
    // A simplified version on how to create a PatternMatchingTrie:
    //
    // For each URL Template, PatternMatchingTrie splits URL Template into chunks (nodes),
    // and throws away the host and port. Parse node from left to right with slash
    // character, insert each node into tree structure.
    // There are special rules for curly brace (ie. wildcard). See
    // <code>com.prodigi.object.Node</code> for details.
    //
    // To compare URL to templates which are now living in a PatternMatchingTrie, split URL
    // with forward slash, then walk the URL part down the Trie.
    // If there is no match for the value of Trie com.prodigi.object.Node, return immediately.
    // If there is a match, then continue walking down the Trie
    //
    // The entire PatternMatchingTrie and its Nodes are expected to be cached in
    // memory/memcached for improved performance.
    //
    // Pattern to identify special node:
    // Example is "{A}.{B}"

    // required for (de-)serialization
    private static final long serialVersionUID = 3L;

    // Single serial version UID for all trie classes.
    // Since all trie classes (com.prodigi.object.PatternMatchingTrie, com.prodigi.object.Node, Edge) are cached as single object,
    // one change to any of those classes should invalidate that cached object.
    // Therefore those classes should all point to the same serialVersionUID.
    private static Logger logger = Logger.getLogger(PatternMatchingTrie.class);

    // URL pattern; assuming URL starts with http or https
    private static Pattern URL_PATTERN = Pattern.compile("^https?://(.*?)\\/(.*)$");

    // root node is the only reference to Trie
    private Node root = new Node("root", Edge.FORWARD_SLASH);

    /**
     * Helper method to remove http host and port from URL
     *
     * @param original Incoming URL String
     * @return URL String without http host and port
     */
    static String removeHttpHostAndPort(String original) {
        Matcher matcher = URL_PATTERN.matcher(original);

        // strip out host and port, if found; otherwise just return the entire
        // string
        if (matcher.find()) {
            return matcher.group(2);
        }
        return original;
    }

    /**
     * Create a NodeWalker object. Parse incoming string (template or url). By
     * default, cut up a string delimited by '/'. Left edge is passed into this
     * method, because left edge cannot be inferred from input. Right edge can be
     * inferred though.
     *
     * @param leftEdge  left edge of a node
     * @param remaining remaining portion of template or url
     * @return NodeWalker object
     */
    static NodeWalker walk(Edge leftEdge, String remaining) {
        // initialize
        NodeWalker next = new NodeWalker();
        next.leftEdge = leftEdge;

        // find next slash index
        int slashIndex = remaining.indexOf('/');
        debug(String.format("   ==>slashIndex=%s", slashIndex));

        // if cannot find next slash, then use the entire string
        String substringUpToNextSlash =
                (slashIndex == -1) ? remaining : remaining.substring(0, slashIndex);

        debug(String.format("     =>substringUpToNextSlash=%s",
                substringUpToNextSlash));

        // upToSlash substring is good enough, so use it as Next node
        next.value = substringUpToNextSlash;

        // if no more slash, this is last
        if (slashIndex == -1) {
            next.isLeaf = true;

            // "fake" rightEdge anyway for purpose of debugging
            // removing the following line, theoretically, does not break
            // @see <code>com.prodigi.object.PatternMatchingTrie.getAllTemplates()</code>
            next.rightEdge = Edge.FORWARD_SLASH;
        } else {
            // not last; need to set variables for next loop to consume
            next.rightEdge = Edge.FORWARD_SLASH;
            next.index = next.value.length();
            next.remaining = remaining.substring(next.index + 1, remaining.length());
        }

        debug(String.format("  %s", next));
        return next;
    }

    /**
     * Helper method to debug
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
     * recursive call to assemble URL Template, from Nodes
     *
     * @param n         com.prodigi.object.Node
     * @param crumb     String crumb (accumulative parents' value)
     * @param templates List of String
     */
    private static void assembleTemplates(Node n, String crumb,
                                          List<String> templates) {
        Set<Node> nodes = n.getAllChildrenForAllEdges();

        for (Node node : nodes) {
            if (node.isLeaf()) {
                templates.add(String.format("%s/%s", crumb, node.value()));
            }
            // recursive call for each child node
            for (Edge edge : node.getAllEdges()) {
                assembleTemplates(node,
                        String.format("%s%s%s", crumb, edge.value(),
                                node.value()), templates);
            }
        }
    }

    /**
     * Add a new template to this Trie. This method parses templateValue into
     * nodes
     *
     * @param templateValue New template value
     */
    public void addTemplate(String templateValue) {
        debug(String.format("IN: addTemplate(), value=%s", templateValue));

        Node rootNode = root;

        // first, remove http host and port
        String template = removeHttpHostAndPort(templateValue);

        // initialize edge to slash
        NodeWalker walker = walk(Edge.FORWARD_SLASH, template);

        Node parentNode = rootNode;

        // extract substring from template, then insert it to trie
        while (walker.index != -1) {
            debug(String
                    .format(" ->chunk is %s, previousIndex is %s, nextIndex is %s",
                            walker.remaining, walker.previous, walker.index));

            String newValue = walker.value;

            debug(String
                    .format("  => adding child node under parentNode=%s, and edge=%s",
                            parentNode, walker.rightEdge));

            // add new child node
            Node childNode =
                    insertChildNode(parentNode, newValue, false, walker.rightEdge);

            // create new node walker for next loop
            walker = walk(walker.rightEdge, walker.remaining);

            debug(String
                    .format("  -> finding next parentNode with edge=%s, value=%s",
                            walker.leftEdge, childNode.value()));

            // For the next level deeper, childNode is now parentNode
            parentNode =
                    parentNode.getExactChildOrNull(childNode.value(), walker.leftEdge);

            // last one, just create child node
            if (walker.isLeaf) {
                debug("  ->insert last child node to trie...");

                insertChildNode(parentNode, walker.value, true, walker.rightEdge);
                // insertChildNode(parentNode, walker.value, true, Edge.LAST);
            }
        }
        debug("Finished adding all templates to trie! ");
    }

    /**
     * Is url matching template(s)?
     *
     * @param url incoming url for comparison
     * @return true if url matches a template; false otherwise
     */
    public boolean matches(String url) {
        try {
            debug("IN: matches()*************************************************************");

            // first, remove http host and port from url
            String urlWithoutHostPort = removeHttpHostAndPort(url);

            // initialize
            NodeWalker next = walk(Edge.FORWARD_SLASH, urlWithoutHostPort);

            Node parentUrlNode = root;
            Node currentUrlNode = null;

            // loop until a match is found or not found
            while (next.index != -1) {
                debug("::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::");
                debug(String.format("Remaining: %s", next.remaining));

                currentUrlNode = new Node(next.value, next.rightEdge);

                debug(String.format(" Find if childNode: %s a child of parent: %s",
                        currentUrlNode, parentUrlNode));

                // if not a match, then return right away
                if (!parentUrlNode.isAChild(currentUrlNode, next.rightEdge)) {
                    debug(String.format("  =>parentNode has %s, children: %s",
                            parentUrlNode.getChildren(next.rightEdge).size(),
                            parentUrlNode.getChildren(next.rightEdge)));
                    debug(String
                            .format("  =>childNode:[%s] is not child of parent:[%s], returning false!",
                                    currentUrlNode.value(), parentUrlNode.value()));
                    return false;
                } else {
                    debug(String.format("  =>childNode:[%s] is child of parent:[%s]",
                            currentUrlNode.value(), parentUrlNode.value()));
                }

                next = walk(next.rightEdge, next.remaining);

                parentUrlNode =
                        parentUrlNode.getChildUrlNodeOrNull(currentUrlNode.value(),
                                next.leftEdge);

                if (parentUrlNode == null) {
                    debug("   =>parentUrlNode is null. Returning false");
                    return false;
                }

                // last one (leaf), just create node
                if (next.isLeaf) {
                    debug(" Find last child (leaf) node...");

                    debug(String.format("  =>parentUrlNode=%s", parentUrlNode));
                    debug(String
                            .format("  =>next.value=%s, next.leftEdge=%s, next.rightEdge=%s",
                                    next.value, next.leftEdge, next.rightEdge));

                    currentUrlNode =
                            parentUrlNode.getChildUrlNodeOrNull(next.value, Edge.ANY);

                    debug(String.format("  =>parentNode: %s, childNode: %s",
                            parentUrlNode, currentUrlNode));
                    if (currentUrlNode == null) {
                        debug("   =>Cannot find child under parent; returning false");
                        return false;
                    } else if (!currentUrlNode.isLeaf()) {
                        debug("   =>childNode not last but should be; returning false");
                        return false;
                    } else {
                        debug("   =>match!");
                    }
                }
            }
        } catch (Throwable throwable) {
            logger.error("Exception occured while matching Uri in Trie : "
                    + throwable.getMessage());
            return false;

        }
        return true;
    }

    /**
     * Insert a child node to parent node
     *
     * @param parentNode Parent com.prodigi.object.Node
     * @param childValue value of child
     * @param isLast     is last element in URL?
     * @param edge       Edge object
     * @return child node that is inserted
     */
    private Node insertChildNode(Node parentNode, String childValue,
                                 boolean isLast, Edge edge) {
        debug(String
                .format("  =>Inserting child node with value: %s to parentNode: %s",
                        childValue, parentNode.value()));
        return parentNode.addChildToCurrentNode(childValue, isLast, edge);
    }

    /**
     * Reconstruct templates from trie. Original template ordering not maintained.
     * Then print to debug. Intended for debugging.
     */
    public void printAllTemplates() {
        List<String> templates = getAllTemplate();

        debug("***********************************************");
        int i = 1;
        for (String template : templates) {
            debug(String.format("Template #%d: %s", i, template));
            i++;
        }
        debug("***********************************************");
    }

    /**
     * Utility method to reconstruct all templates from trie. Original template ordering not maintained.
     * This method is intended for debugging.
     *
     * @return List<String>
     *         List of templates reconstructed from Trie
     */
    public List<String> getAllTemplate() {
        List<String> templates = new ArrayList<>();
        assembleTemplates(root, "", templates);
        return templates;
    }

    /**
     * Store a node metadata. Used for walking nodes down a Trie.
     */
    private static class NodeWalker {
        boolean isLeaf = false;
        int index = -1;
        int previous = -1;
        String value = null;
        Edge leftEdge = null;
        Edge rightEdge = null;
        String remaining = null;

        @Override
        public String toString() {
            return String
                    .format("NodeWalker [isLast=%s, index=%s, previous=%s, value=%s, leftEdge=%s, rightEdge=%s, remaining=%s]",
                            isLeaf, index, previous, value, leftEdge, rightEdge, remaining);
        }
    }

}
