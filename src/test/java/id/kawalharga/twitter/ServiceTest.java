package id.kawalharga.twitter;

import org.junit.Before;
import org.junit.Test;
import twitter4j.Status;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by yohanesgultom on 17/06/16.
 */
public class ServiceTest {

    Service service;

    @Before
    public void setup() {
        try {
            service = new Service("src/test/resources/config.properties");
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }


    @Test
    public void patternTest() {
        String text = "@pantauharga lapor harga nama komoditas Rp 777777/kg di lokasi";
        try {
            Matcher m = Service.PATTERN_PRICE_REPORT.matcher(text);
            assert m.find();
            assert m.groupCount() == 4;
            assert "nama komoditas".equals(m.group(1));
            assert "777777".equals(m.group(2));
            assert "kg".equals(m.group(3));
            assert "lokasi".equals(m.group(4));
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test
    public void apiConfigTest() {
        try {
            List<Status> statuses = service.twitter.getMentionsTimeline();
            assert (statuses != null);
            assert (statuses.size() > 0);
        } catch (Exception e) {
            assert false;
        }
    }
}
