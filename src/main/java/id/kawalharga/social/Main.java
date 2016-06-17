package id.kawalharga.social;

import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by yohanesgultom on 17/06/16.
 */
public class Main {

    final static Logger logger = Logger.getLogger(Main.class);

    public static void main(String args[]) {
        try {
            // args: action, config file
            if (args.length > 1) {
                String action = args[0];
                String config = args[1];
                logger.info(action + " starting");
                if ("fb-page-update".equalsIgnoreCase(action)) {
                    id.kawalharga.facebook.Service service = new id.kawalharga.facebook.Service(config);
                    List<String> postIds = service.getLastPostIds(10);
                    service.updatePostsStatus(postIds);
                } else if ("fb-page-single-post".equalsIgnoreCase(action)) {
                    id.kawalharga.facebook.Service service = new id.kawalharga.facebook.Service(config);
                    service.postSingleTodayInput();
                } else if ("tw-check-report".equalsIgnoreCase(action)) {
                    int minutes = 5;
                    id.kawalharga.twitter.Service service = new id.kawalharga.twitter.Service(config);
                    service.checkReport(minutes);
                } else if ("tw-single-post".equalsIgnoreCase(action)) {
                    id.kawalharga.twitter.Service service = new id.kawalharga.twitter.Service(config);
                    service.postSingleTodayInput();
                }
                logger.info(action + " completed");
            } else {
                logger.error("Insufficient arguments");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
