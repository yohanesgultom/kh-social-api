package id.kawalharga.facebook;

import facebook4j.Post;
import id.kawalharga.model.CommodityInput;
import id.kawalharga.model.Geolocation;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by yohanesgultom on 11/06/16.
 */
public class ServiceTest {

    final static Logger logger = Logger.getLogger(ServiceTest.class);

    Service service;

    @Before
    public void setup() throws Exception {
        service = new Service("src/test/resources/config.properties");
    }

    @Test
    public void renewAccessTokenTest() {
        try {
            service.renewAccessToken();
            assert true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }

    @Test
    public void urlTest() {

        try {
            String actual = service.getGoogleMapUrlString(new Geolocation(-6.239879, 106.8623443, 0));
            String expected = "http://maps.google.com/maps?q=-6.239879,106.862344";
            assertEquals(expected, actual);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }

    }

    @Test
    public void getInputToBePostedTest() {
        try {
            CommodityInput input = service.getInputToBePosted(Service.SOCIAL_MEDIA_TABLE);
            assert true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }


//    @Test
    public void postTest() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
            String dateInString = "02-06-2016";
            List<CommodityInput> list = service.getService().getInputsToBePosted(sdf.parse(dateInString), 1, Service.SOCIAL_MEDIA_TABLE);
            CommodityInput input = list.get(0);
            String id = service.post(input);
            logger.info("Posted to page: " + id);
            assert id != null;
            if (id != null) {
                Post post = service.getPost(id);
                service.insertPost(post, input);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }

    @Test
    public void getLastPostedInputCreatedDateTest() {
        try {
            Date date = service.getLastPostedInputCreatedDate();
            assert date != null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }

    @Test
    public void getLastPostIdsTest() {
        try {
            List<String> ids = service.getLastPostIds(10);
            assert ids != null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }

    @Test
    public void updatePostsStatusTest() {
        try {
            List<String> ids = service.getLastPostIds(10);
            service.updatePostsStatus(ids);
            assert true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }

    @Test
    public void getLastPostedCommodityInputTest() {
        try {
            List<CommodityInput> inputs = service.getLastPostedCommodityInput(2);
            assert inputs != null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            assert false;
        }
    }
}
