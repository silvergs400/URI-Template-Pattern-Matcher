package com.prodigi.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Top-level class.
 *
 * General use-case is:
 * <code>
 *
 * </code>
 *
 * Support parsing of UriTemplate level 1 For details, see
 * http://tools.ietf.org/html/rfc6570
 *
 * @author Wilkin Cheung
 */
public class UriTemplateValidator {

    private static Logger logger = Logger.getLogger(UriTemplateValidator.class);

    /**
     * Check if uri matches uriTemplate
     * <p/>
     * For Example:
     *
     * <code>
     *   String uri = "http://api.prodigisoftware.com/human/v1/rec/en-US/movies/id/123.json";
     *   String uriTemplate = "/human/v1/{typeA}/en-US/{typeB}/id/{typeC}.json";
     *   UriTemplateValidator.matches(uri, uriTemplate);
     *   ...returns:
     *   {typeA=rec, typeB=movies, typeC=123}
     * </code>
     *
     * If no match, then returning <code>map</code> will have size 0.
     *
     * @param uri         request uri. This is typically
     *                    <code>request.getRequestUrl()</code>
     * @param uriTemplate uriTemplate
     * @return true if there is url matches one or more uriTemplate; false otherwise.
     */
    public Map<String, String> matches(String uri, String uriTemplate) {
        return new UriTemplate(removeHttpHostAndPort(uriTemplate))
                .match(removeHttpHostAndPort(uri));
    }

    /**
     * Check if uri matches uriTemplate
     *
     * For Example:
     * <code>
     *   String uri = "http://api.prodigisoftware.com/human/v1/rec/en-US/movies/id/123.json";
     *   String uriTemplate = "/human/v1/{typeA}/en-US/{typeB}/id/{typeC}.json";
     *   UriTemplateValidator.matches(uri, uriTemplate);
     * </code>
     *   ... returns true
     *
     * @param uri         request uri. This is typically
     *                    <code>request.getRequestUrl()</code>
     * @param uriTemplate uriTemplate
     * @return true if there is url matches one or more uriTemplate; false otherwise.
     */
    public boolean isMatch(String uri, String uriTemplate) {
        return (new UriTemplate(removeHttpHostAndPort(uriTemplate)).match(uri)
                .size() > 0)
                || (matchesDirectly(removeHttpHostAndPort(uri),
                removeHttpHostAndPort(uriTemplate)));
    }

    /**
     * If uri matches uriTemplate in a direct way. uriTemplate is not required to
     * define {variableName} but it can still match uri if both are equal or uri
     * minus baseUrl (IP + Port) For example, the following should match: String
     * uri = "http://api.prodigisoftware.com/human/v1/rec/en-US/movies/id/123.json";
     * String uriTemplate =
     * "http://api.prodigisoftware.com/human/v1/rec/en-US/movies/id/123.json"; The
     * following should also match: String uri =
     * "http://api.prodigisoftware.com/human/v1/rec/en-US/movies/id/123.json"; String
     * uriTemplate = "/human/v1/rec/en-US/movies/id/123.json";
     *
     * @param uri         request uri. This is typically
     *                    <code>request.getRequestUrl()</code>
     * @param uriTemplate uriTemplate
     * @return true if there is url matches uriTemplate is the aforementioned
     *         fashion
     */
    private boolean matchesDirectly(String uri, String uriTemplate) {

        return uri.equals(uriTemplate);
    }

    /**
     * Remove host and port from url
     *
     * @param original String of format "http://host:port/remaining"
     * @return remaining portion of
     */
    public String removeHttpHostAndPort(String original) {
        Pattern pattern = Pattern.compile("^https?://(.*?)\\/(.*)$");
        Matcher matcher = pattern.matcher(original);

        if (matcher.find()) {
            return matcher.group(2);
        }
        return original;
    }

    /**
     * Inner class represents UriTemplate
     */
    public static class UriTemplate implements Serializable {

        // required for (de-)serialization
        protected static final long serialVersionUID = 1L;

        /**
         * uriTemplate as String
         */
        private final String uriTemplate;

        /**
         * Ordered keyNames
         */
        private final List<String> keys;

        /**
         * Pattern
         */
        private final Pattern pattern;

        /**
         * UriTemplate for internal parsing to regular expression
         *
         * @param uriTemplate uriTemplate to be parsed
         */
        public UriTemplate(String uriTemplate) {
            LevelOneParser parser = new LevelOneParser(uriTemplate);
            this.pattern = parser.getPattern();
            this.uriTemplate = uriTemplate;
            this.keys = parser.getKeyNames();
        }

        /**
         * Match the given URI to a map of key values. Keys in the returned map are
         * key names, values are key values, as occurred in the given URI.
         *
         * For example:
         * <code>
         *   UriTemplate t = new UriTemplate("/human/v1/{typeA}/en-US/{typeB}/id/{typeC}.json");
         *   t.match("http://api.prodigisoftware.com/human/v1/rec/en-US/movies/id/123.json");
         * </code>
         * ...would return: {typeA=rec, typeB=movies, typeC=123}
         *
         * If not a match, then returning <code>map</code> will have size 0.
         *
         * @param uri to try to match to
         * @return Map of matching key name (Map key), key value (Map value)
         */
        public Map<String, String> match(String uri) {
            // LinkedHashMap to maintain key insertion order
            Map<String, String> result =
                    new LinkedHashMap<>(keys.size());

            Matcher matcher = pattern.matcher(uri);

            // find next part in uri that matches the pattern
            if (matcher.find()) {
                for (int i = 0; i < matcher.groupCount(); i++) {
                    String name = keys.get(i);
                    String value = matcher.group(i + 1);
                    result.put(name, value);
                }
            }
            return result;
        }

        /**
         * See if uri matches the pattern.
         *
         * @param uri String for matching
         * @return true if there is one or more matches; false otherwise
         */
        public boolean matches(String uri) {
            return match(uri).size() > 0;
        }

        /**
         * Return uriTemplate in string format
         *
         * @return uriTemplate in string format
         */
        public String toString() {
            return uriTemplate;
        }
    }

    /**
     * Inner class for parsing RFE 6570 Level 1 Template into a RegEx.
     */
    public static class LevelOneParser implements Serializable {

        // required for (de-)serialization
        protected static final long serialVersionUID = 1L;

        // For each pattern {keyName} replaces it with (.*)
        private static final Pattern LEVEL_ONE_PATTERN = Pattern
                .compile("\\{([^/]+?)\\}");

        // Replaces each {keyName} with (.*)
        private static final String REPLACES_WITH = "(.*)";

        // pattern builder
        private StringBuilder patternBuilder = new StringBuilder();

        // List of key names
        private List<String> keyNames = new ArrayList<String>();

        /**
         * Constructor
         *
         * @param uriTemplate uriTemplate to be parsed
         */
        private LevelOneParser(String uriTemplate) {
            Matcher m = LEVEL_ONE_PATTERN.matcher(uriTemplate);
            int start;
            int end = 0;

            // In each loop, find next pattern in URI that is "{keyName}"
            // If found, then add "keyName" to keyNames, and append the substring to
            // patternBuilder.
            while (m.find()) {

                // move start pointer to last match
                start = m.start();

                // Mark the pattern as escaped
                String escaped = Pattern.quote(uriTemplate.substring(end, start));

                patternBuilder.append(escaped);

                patternBuilder.append(REPLACES_WITH);

                // save the previously matched sequence (that is, keyName)
                // group(1) means the substring within (.*)
                keyNames.add(m.group(1));

                // move end pointer to the end of matched string
                end = m.end();
            }

            // Mark the pattern as escaped
            patternBuilder.append(Pattern.quote(uriTemplate.substring(end,
                    uriTemplate
                            .length())));
        }

        /**
         * Return List of keyNames
         *
         * @return keyNames as List
         */
        List<String> getKeyNames() {
            return keyNames;
        }

        /**
         * Get match pattern
         *
         * @return compiled pattern
         */
        Pattern getPattern() {
            return Pattern.compile(patternBuilder.toString());
        }
    }
}
