import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import com.prodigi.service.UriTemplateValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit test for uriTemplateValidatorImpl
 */
@RunWith(MockitoJUnitRunner.class)
public class UriTemplateValidatorTest {

    @InjectMocks
    private static UriTemplateValidator uriTemplateValidator;

    /**
     * Single template. Match.
     */
    @Test
    public void uriTemplateMatchesUri() {
        String uri =
                "http://prodigi.com/human/v1/rec/en-US/movies/id/123.json";
        String t =
                "http://prodigi.com/human/v1/{typeA}/en-US/{typeB}/id/{typeC}.json";

        Map<String, String> resultMap = uriTemplateValidator.matches(uri, t);

        assertEquals(" Number of keys should match", 3, resultMap.size());
        assertEquals(" key should match", resultMap.get("typeA"), "rec");
        assertEquals(" key should match", resultMap.get("typeB"), "movies");
        assertEquals(" key should match", resultMap.get("typeC"), "123");

        // isMatch(uri) should be consistent with match(uri)
        assertTrue(uriTemplateValidator.isMatch(uri, t));
    }

    /**
     * Single template with special characters. Match.
     */
    @Test
    public void uriTemplateMatchesUriWithEscapedCharacter() {
        String uri =
                "http://api.prodigisoftware.com/human/v1/rec/en-US/movies/id/123.json";
        String t =
                "http://api.prodigisoftware.com/human/v1/{type-A}/en-US/{type:B}/id/{type\\&C}.json";

        Map<String, String> resultMap = uriTemplateValidator.matches(uri, t);

        assertEquals(" Number of keys should match", 3, resultMap.size());
        assertEquals(" key should match", resultMap.get("type-A"), "rec");
        assertEquals(" key should match", resultMap.get("type:B"), "movies");
        assertEquals(" key should match", resultMap.get("type\\&C"), "123");

        // isMatch(uri) should be consistent with match(uri)
        assertTrue(uriTemplateValidator.isMatch(uri, t));
    }

    /**
     * Single template. No Match.
     */
    @Test
    public void uriTemplateNoMatch() {
        String uri =
                "http://api.prodigisoftware.com/human/v1/rec/en-US/movies/id/123.json";
        String t =
                "http://api.prodigisoftware.com/human/v1/{type-A}/en-US/{type:B}/id/sssssssssssssssssssss.json";

        Map<String, String> resultMap = uriTemplateValidator.matches(uri, t);

        // return size=0 because not a complete match
        assertEquals(" Number of keys should match", 0, resultMap.size());

        // isMatch(uri) should be consistent with match(uri)
        assertFalse(uriTemplateValidator.isMatch(uri, t));
    }

    /**
     * Single template. Match. Url contains HTTP Port.
     */
    @Test
    public void uriTemplateMatchesUriWithHttpPort() {
        String uri =
                "http://api.prodigisoftware.com:9999/human/v1/rec/en-US/movies/id/123.json";
        String t =
                "http://api.prodigisoftware.com/human/v1/{typeA}/en-US/{typeB}/id/{typeC}.json";

        Map<String, String> resultMap = uriTemplateValidator.matches(uri, t);

        assertEquals(" Number of keys should match", 3, resultMap.size());
        assertEquals(" key should match", resultMap.get("typeA"), "rec");
        assertEquals(" key should match", resultMap.get("typeB"), "movies");
        assertEquals(" key should match", resultMap.get("typeC"), "123");

        // isMatch(uri) should be consistent with match(uri)
        assertTrue(uriTemplateValidator.isMatch(uri, t));
    }

    /**
     * Exact match
     */
    @Test
    public void uriTemplateExactMatch() {
        String uri =
                "http://api.prodigisoftware.com:9999/human/v1/rec/en-US/movies/id/123.json";
        String t =
                "http://api.prodigisoftware.com:9999/human/v1/rec/en-US/movies/id/123.json";

        Map<String, String> resultMap = uriTemplateValidator.matches(uri, t);

        assertEquals(" Number of keys should match", 0, resultMap.size());
        assertTrue(uriTemplateValidator.isMatch(uri, t));
    }

    /**
     * Substring match with leading slash
     */
    @Test
    public void uriTemplateSubstringMatchWithLeadingSlash() {
        String uri =
                "http://api.prodigisoftware.com:9999/human/v1/rec/en-US/movies/id/123.json";
        String t =
                "http://api.prodigisoftware.com/human/v1/rec/en-US/movies/id/123.json";

        Map<String, String> resultMap = uriTemplateValidator.matches(uri, t);

        assertEquals(" Number of keys should match", 0, resultMap.size());
        assertTrue(uriTemplateValidator.isMatch(uri, t));

    }

    /**
     * Substring match without leading slash
     */
    @Test
    public void uriTemplateSubstringMatchWithoutLeadingSlash() {
        String uri =
                "http://api.prodigisoftware.com:9999/human/v1/rec/en-US/movies/id/123.json";
        String t =
                "http://api.prodigisoftware.com:9999/human/v1/rec/en-US/movies/id/123.json";

        Map<String, String> resultMap = uriTemplateValidator.matches(uri, t);

        assertEquals(" Number of keys should match", 0, resultMap.size());
        assertTrue(uriTemplateValidator.isMatch(uri, t));

    }

    @Test
    public void weird_characters() {
        String t;
        String uri;

        uri =
                "http://localhost:8080/kitty-ws/v1/search?w=Kung+Fu+Panda&rpr=&un=prodigikids&fl=SynFilterMovies^&tv_ip=10.4.18.166&tv_port=9000&DS=255&RFTR=256&XUA=prodigi&XPID=pkg00@341769312.prodigi&PN=13&app=TVTAP";
        t =
                "http://10.154.0.113:8080/kitty-ws/v1/search?w={w}&rpr={rpr}&un={un}&fl=SynFilterMovies^{locale}&tv_ip=10.4.18.166&tv_port=9000&DS=255&RFTR=256&XUA=prodigi&XPID=pkg00@341769312.prodigi&PN=13&app=TVTAP";
        assertTrue(uriTemplateValidator.isMatch(uri, t));

    }

    /**
     * Print each Map KV pair to console for debugging purpose
     *
     * @param map KV pair
     */
    private static void debugMap(Map<String, String> map) {
        System.out.println("  map.size() is " + map.size());
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println("    [key=" + entry.getKey() + ", value="
                    + entry.getValue() + "]");
        }
    }
}
