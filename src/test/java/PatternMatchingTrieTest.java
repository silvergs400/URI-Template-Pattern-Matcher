import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.prodigi.object.Node;
import com.prodigi.object.PatternMatchingTrie;
import org.junit.Test;

public class PatternMatchingTrieTest {
    @Test
    public void data_template_actual_from_samson_July17_2014() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://www.prodigisoftware.com/hamster/v2.b/0/lookup/airing/{id}");
        tree
                .addTemplate("http://www.prodigisoftware.com/hamster/v2.b/0/lookup/airing/{id}/synopses/first?by=length%3D{length},length%3D{length2},length%3D{length3},length%3D{length4}");

        tree.printAllTemplates();

        matches(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/lookup/airing/1127268189");
        matches(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/lookup/airing/1127268189/synopses/first?by=length%3D1,length%3D2,length%3D3,length%3D4");
    }

    @Test
    public void illegal_extra_slash_test() {
        PatternMatchingTrie<Node> tree = new PatternMatchingTrie<>();

        tree.addTemplate("http://prodigi.com/{key1}/{key2}");

        // algorithm detects illegal slash
        notMatches(tree, "http://prodigi.com/1/2/3");
    }

    @Test
    public void empty_trie_test() {
        PatternMatchingTrie<Node> tree = new PatternMatchingTrie<>();
        notMatches(tree, "http://prodigi.com/1/2/3");
    }

    @Test
    public void trie_basics_without_questionMark() {
        PatternMatchingTrie<Node> tree = new PatternMatchingTrie<>();

        tree.addTemplate("http://prodigi.com/{key1}/v1/{key2}");
        tree.addTemplate("http://prodigi.com/some/thing/really");
        tree.addTemplate("http://blah.org:80800/{key1}/v1/{key2}/hello/world");
        tree
                .addTemplate("http://blah.org:80800/{key1}/v1/{key2}/hello/world/my.json");
        tree
                .addTemplate("http://blah.org:80800/{key1}/v1/{key2}/hello/world/really/my.json");
        tree.addTemplate("http://blah.org:80800/{key1}/v1/{key2}/dog/{key3:?}");

        matches(tree, "http://blah.oo.com:1234/{key1}/v1/{key2}/hello/world");
        matches(tree, "http://blah/1/v1/2/hello/world");
        matches(tree, "http://blah.org:80800/{key1}/v1/{key2}/dog/{key3:?}");
        matches(tree, "http://blah.org:80800/{key1}/v1/{key2}/dog/cat");
        matches(tree, "http://blah.oo.com:1234/{key1}/v1/{key2}/hello/world");
        matches(tree, "http://blah/1/v1/2/hello/world");

        notMatches(tree, "http://blah/1/v1000/2/hello/world");
        notMatches(tree, "http://prodigi.com/some/thing");
    }

    @Test
    public void url_encoded_characters_in_param() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=%E1%E2,%55%66");
    }

    @Test
    public void single_id_surrounded_by_slashes_but_contains_NO_NEED_TO_ENCODE_characters() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id}/logos/first?by={by},{by2}&in={in},{in2}");

        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/-/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/$/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/_/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/./logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/!/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/*/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/'/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/(/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/)/logos/first?by=3,4&in=5,6");
    }

    @Test
    public void double_id_surrounded_by_slashes_but_contains_NO_NEED_TO_ENCODE_characters() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/-,-/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/$,$/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/_,_/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/.,./logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/!,!/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/*,*/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/','/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/(,(/logos/first?by=3,4&in=5,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/),)/logos/first?by=3,4&in=5,6");
    }

    @Test
    public void single_id_surrounded_by_slashes_but_contains_INVALID_characters() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id}/logos/first?by={by},{by2}&in={in},{in2}");

        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1<2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1#/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1{/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1}/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1>/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1|/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1|/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1\\/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1^/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1[/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1]/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1`/logos/first?by=3,4&in=5,6");
    }

    @Test
    public void double_id_surrounded_by_slashes_but_contains_INVALID_characters() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,<2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1#,2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1{,2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1},2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1>,2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1|,2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1|,2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1\\,2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1^,2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1[,2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1],2/logos/first?by=3,4&in=5,6");
        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1`/logos/first?by=3,4&in=5,6");
    }

    @Test
    public void url_encoded_space_in_param() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1+,+2/logos/first?by=3+++,4+4+4&in=5+a+b+c,6");
    }

    @Test
    public void safe_characters_in_param_in_query() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a$b,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a-b,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a_b,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a.b,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a!b,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a*b,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a'b,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a(b,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a)b,6");
        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/1,2/logos/first?by=3,4&in=a$b,6");
    }

    @Test
    public void unsafe_chars_not_allowed_in_param_in_query() {
        // special characters are $-_.!*'()
        // unsafe characters are "<>#{}|\^[]`
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/,/logos/first?by=1,2&in=\\,2");

    }

    @Test
    public void reserved_chars_allowed_in_param_in_query() {
        // reserved characters are ~
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        matches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/~,~/logos/first?by=1,2&in=~,~");

    }

    @Test
    public void reserved_chars_not_allowed_in_param_in_query() {
        // reserved characters are &,/:;=?@
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/,/logos/first?by=1,;&in=&,/");

    }

    @Test
    public void ascii_control_chars_not_allowed_in_param_in_query() {
        // ascii control characters are 0x00 through 0x1F and 0x7F
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/,/logos/first?by=,&in=\\0x01,");

    }

    @Test
    public void ascii_80_thru_FF_not_allowed_in_param_in_query() {
        // ascii control characters are 0x00 through 0x1F and 0x7F
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/,/logos/first?by=,&in=\\x00,");
    }

    @Test
    public void blank_space_not_allowed_in_param() {
        // ascii control characters are 0x00 through 0x1F and 0x7F
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2}/logos/first?by={by},{by2}&in={in},{in2}");

        notMatches(tree,
                "http://prodigisoftware.com/puppy-ws/v2.b/0/batch/source/,/logos/first?by=,&in= ,");
    }

    @Test
    public void time_loading_hundredsOfTemplates_into_memory_May2014()
            throws IOException {
        loadTemplates("/whale.a.templates");
    }

    @Test
    public void multiple_commas() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://www.prodigisoftware.com/puppy-ws/v2.b/0/batch/source/{id},{id2},{id3}/logos/first?by={by},{by2},{by3},{by4},{by5}&in={in},{in2},{in3}");

        matches(tree,
                "http://www.prodigisoftware.com/puppy-ws/v2.b/0/batch/source/69021163,,/logos/first?by=,,,,&in=,,");
        matches(tree,
                "http://www.prodigisoftware.com/puppy-ws/v2.b/0/batch/source/,,/logos/first?by=asd,asda,asd,sdd,&in=,,");
    }

    @Test
    public void date_with_dash() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://www.prodigisoftware.com/puppy-ws/v2.b/0/hamster/service/{id}");
        tree
                .addTemplate("http://www.prodigisoftware.com/puppy-ws/v2.b/0/hamster/service/{id}/channels?page={page}&size=50");
        tree
                .addTemplate("http://www.prodigisoftware.com/puppy-ws/v2.b/0/hamster/service/{id}/channels?page={page}&size=10");
        tree
                .addTemplate("http://www.prodigisoftware.com/puppy-ws/v2.b/0/hamster/service/{id}/{id2}/channels?page={page}&size=1");
        tree
                .addTemplate("http://www.prodigisoftware.com/puppy-ws/v2.b/0/hamster/service/{id}/schedule/{date}?page={page}&size=50&duration=6&block={block}&inprogress=false");

        matches(tree,
                "http://www.prodigisoftware.com/puppy-ws/v2.b/0/hamster/service///channels?page={page}&size=1");
    }

    @Test
    public void pluses() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://www.prodigisoftware.com/puppy-ws/v2.b/0/hamster/airing/{id}");
        matches(tree,
                "http://www.prodigisoftware.com/puppy-ws/v2.b/0/hamster/airing/2131441++3413414++1341341");
    }

    @Test
    public void merge_nodes() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree.addTemplate("http://blah.org:80800/hello/{key1}/my.json");
        tree.addTemplate("http://blah.org:80800/hello/{key2}");
        tree.addTemplate("http://blah.org:80800/hello/{key3}/some/my.json");
        tree.addTemplate("http://blah.org:80800/hello/world/1/2/3.json");

        notMatches(tree, "http://blah.org/hello/hello/1/2/3.json");
        matches(tree, "http://blah.oo.com:12/hello/world");
        matches(tree, "http://blah.oo.com:12/hello/1/some/my.json");
    }

    @Test
    public void url_with_questionMark() {
        PatternMatchingTrie tree = new PatternMatchingTrie();

        tree.addTemplate("http://prodigisoftware.com/{namespace}?key=value&key={id}");

        matches(tree, "http://prodigisoftware.com/{namespace}?key=value&key={id}");
    }

    @Test
    public void trie_partial_wildcard_no_match() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        // v2.b1
        tree
                .addTemplate("http://10.154.0.114:8080/whale/v2.b1/0/browse/vodfolder.({PATH},vodfolder);id={id}");

        // v1.b1
        notMatches(tree,
                "http://10.154.0.114:8080/whale/v1.b1/0/browse/vodfolder.(*,vodfolder);id=1000");

        // v2.b2
        notMatches(tree,
                "http://10.154.0.114:8080/whale/v2.b2/0/browse/vodfolder.(*,vodfolder);id=1000");

        // wS (case sensitive)
        notMatches(tree,
                "http://10.154.0.114:8080/whale/v2.b2/0/browse/vodfolder.(*,vodfolder);id=1000");
    }

    @Test
    public void trie_partial_wildcard_not_at_the_end() {
        PatternMatchingTrie tree = new PatternMatchingTrie();
        tree
                .addTemplate("http://10.154.0.114:8080/whale/v2.b1/0/browse/vodfolder.(*,vodfolder);id={id}/end");

        matches(tree,
                "http://10.154.0.114:8080/whale/v2.b1/0/browse/vodfolder.(*,vodfolder);id=/end");
        matches(tree,
                "http://10.154.0.114:8080/whale/v2.b1/0/browse/vodfolder.(*,vodfolder);id=1000/end");
    }

    @Test
    public void special_characters_1() {
        // actual test case by search team
        String t =
                "http://localhost:8080/kitty-ws/v1/search?w={w}&rpr={rpr}&un={un}&fl=SynFilterMovies^{locale}&tv_ip=10.4.18.166&tv_port=9000&DS=255&RFTR=256&XUA=prodigi&XPID=pkg00@341769312.prodigi&PN=13&app=TVTAP";
        String url =
                "http://localhost:8080/kitty-ws/v1/search?w=Kung+Fu+Panda&rpr=&un=prodigikids&fl=SynFilterMovies^&tv_ip=10.4.18.166&tv_port=9000&DS=255&RFTR=256&XUA=prodigi&XPID=pkg00@341769312.prodigi&PN=13&app=TVTAP";
        matches(t, url);

        // remove ^ after SynFilterMovies
        url =
                "http://localhost:8080/kitty-ws/v1/search?w=Kung+Fu+Panda&rpr=&un=prodigikids&fl=SynFilterMovies&tv_ip=10.4.18.166&tv_port=9000&DS=255&RFTR=256&XUA=prodigi&XPID=pkg00@341769312.prodigi&PN=13&app=TVTAP";
        notMatches(t, url);
    }

    @Test
    public void special_characters_3() {
        // actual test case by puppy team
        String t =
                "http://localhost:8080/puppy-ws/rest/Movie/{id}/credits;isCast=false?in={in}&page={page}&size={size}&by={by}";
        String url =
                "http://localhost:8080/puppy-ws/rest/Movie/111/credits;isCast=false?in=&page=&size=22&by=";
        matches(t, url);
    }

    @Test
    public void dot() {
        // actual test case by search team
        String t = "http://localhost:8080/image/{id}.png";
        String url = "http://localhost:8080/image/ID.png";
        matches(t, url);
    }

    @Test
    public void batch_x2() {
        // actual test case by search team
        String t = "http://localhost:8080/kitty-ws/v1/search/{A}.{B}";
        String url = "http://localhost:8080/kitty-ws/v1/search/1.2";
        matches(t, url);
    }

    @Test
    public void batch_x3() {
        // actual test case by search team
        String t = "http://localhost:8080/kitty-ws/v1/search/{1},{2},{3}";
        String url = "http://localhost:8080/kitty-ws/v1/search/1,2,3";
        matches(t, url);
    }

    @Test
    public void batch_x4() {
        // actual test case by search team
        String t = "http://localhost:8080/kitty-ws/v1/search/{1},{2},{3},{4}";
        String url = "http://localhost:8080/kitty-ws/v1/search/1,2,3,4";
        matches(t, url);
    }

    @Test
    public void batch_x4_match_greedy() {
        // actual test case by search team
        String t = "http://localhost:8080/kitty-ws/v1/search/{1},{2},{3},{4}";

        // one extra params (5th) therefore no match
        String url = "http://localhost:8080/kitty-ws/v1/search/1,2,3,4,5";
        notMatches(t, url);
    }

    @Test
    public void dataTemplate_actual_test() throws IOException {
        PatternMatchingTrie<Node> tree = loadTemplates("/hamster.a.templates");

        tree.printAllTemplates();

        matchesWithTiming(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/lookup/airing/1127268189");
        matchesWithTiming(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/lookup/airing/1127268189/synopses/first?by=length%3D1,length%3D2,length%3D3,length%3D4");
    }

    @Test
    public void dataTemplate_560_templates() throws IOException {
        PatternMatchingTrie<Node> tree = loadTemplates("/hamster.b.templates");

        // tree.printAllTemplates();

        matchesWithTiming(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/lookup/airing/1127268189");
        matchesWithTiming(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/lookup/airing/1127268189/synopses/first?by=length%3D1,length%3D2,length%3D3,length%3D4");
    }

    @Test
    public void dataTemplate_Sonya_templates() throws IOException {
        PatternMatchingTrie<Node> tree = loadTemplates("/hamster.c.templates");

        // tree.printAllTemplates();

        matchesWithTiming(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/lookup/movie/914220929?in=en,,");

        matchesWithTiming(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/lookup/movie/914220929?in=,,");
        matchesWithTiming(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/batch/content/906647964,,/images/first?by=,,,,&in=,,");

        matchesWithTiming(tree,
                "http://www.prodigisoftware.com/hamster/v2.b/0/lookup/airing/1060369903/synopses/first?by=length%3Dplain,length%3Dshort,length%3Dshort,length%3Dplain");
    }

    /**
     * Helper method for matching url against template
     *
     * @param t   template as String
     * @param url url as String
     */
    private static void matches(String t, String url) {
        PatternMatchingTrie tree = new PatternMatchingTrie<>();
        tree.addTemplate(t);
        matches(tree, url);
    }

    /**
     * Helper method for matching url against template
     *
     * @param t   template as String
     * @param url url as String
     */
    private static void notMatches(String t, String url) {
        PatternMatchingTrie tree = new PatternMatchingTrie<>();
        tree.addTemplate(t);
        notMatches(tree, url);
    }

    /**
     * Helper method.
     *
     * @param tree Trie tree
     * @param s    value to check against
     */
    public static void notMatches(PatternMatchingTrie tree, String s) {
        assertFalse("trie tree should NOT match", tree.matches(s));
    }

    /**
     * Helper method to match URL against Trie
     *
     * @param tree Trie tree
     * @param s    URL value to check against
     */
    public static void matches(PatternMatchingTrie tree, String s) {
        assertTrue("trie tree should match", tree.matches(s));
    }

    /**
     * Helper method to match URL against Trie. Print to stdout time taken
     *
     * @param tree Trie tree
     * @param s    URL value to check against
     */
    public static void matchesWithTiming(PatternMatchingTrie tree, String s) {
        long start = System.currentTimeMillis();

        matches(tree, s);

        System.out.println("********Comparing URL to templates takes "
                + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * Helper method to load templates from file system.
     *
     * @return com.prodigi.object.PatternMatchingTrie<com.prodigi.object.Node> a trie that contains templates from file
     * @throws IOException Cannot load file
     */
    private PatternMatchingTrie<Node> loadTemplates(String filename) throws IOException {
        BufferedReader reader =
                new BufferedReader(new FileReader(getClass().getResource(filename)
                        .getFile()));

        String line = null;

        List<String> templates = new ArrayList<>();

        // read into memory first
        while ((line = reader.readLine()) != null) {
            templates.add(line);
        }

        // Now calculate time for adding templates to tree
        long start = System.currentTimeMillis();

        PatternMatchingTrie tree = new PatternMatchingTrie();
        for (String template : templates) {
            tree.addTemplate(template);
        }
        System.out.println(String.format("******Loading %d templates takes %d ms",
                templates.size(),
                (System.currentTimeMillis() - start)));

        return tree;
    }

}
