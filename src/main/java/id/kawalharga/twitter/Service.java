package id.kawalharga.twitter;

import facebook4j.Post;
import id.kawalharga.model.CommodityInput;
import id.kawalharga.model.User;
import id.kawalharga.social.AbstractService;
import org.apache.log4j.Logger;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yohanesgultom on 17/06/16.
 */
public class Service extends AbstractService {

    static final Logger logger = Logger.getLogger(id.kawalharga.database.Service.class);
    // admin
    static final String DEFAULT_USER = "admin";
    // monas jakarta
    static final Double DEFAULT_LAT = -6.17553;
    static final Double DEFAULT_LNG = 106.820596;
    static final String DEFAULT_REGION = "DKI JAKARTA";
    static final String THANKYOU_REPLY = "@%s terimakasih atas laporan %s";
    static final String SOCIAL_MEDIA_TABLE = "post_tw";

    // Format: @pantauharga lapor harga (nama komoditas) Rp (harga)/kg di (lokasi)
    static final Pattern PATTERN_PRICE_REPORT = Pattern.compile("@pantauharga lapor harga ([\\w\\s\\d]+) Rp ([\\d\\.,]+)/([\\w]+) di ([\\w\\s\\d]+)", Pattern.CASE_INSENSITIVE);

    Twitter twitter;

    public Service(String config) throws Exception {
        super(config);
        this.twitter = TwitterFactory.getSingleton();
        this.createTableIfNotExist();
    }

    void createTableIfNotExist() throws Exception {
        Connection connection = this.getService().connectToDatabase();
        Statement statement = connection.createStatement();
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + SOCIAL_MEDIA_TABLE + "("
                + "id VARCHAR(50) NOT NULL, "
                + "comodity_input_id BIGINT NOT NULL, "
                + "text VARCHAR(500) NOT NULL, "
                + "likes INT DEFAULT 0, "
                + "share INT DEFAULT 0, "
                + "created_date DATE NOT NULL, " + "PRIMARY KEY (id) "
                + ")";
        boolean result = statement.execute(createTableSQL);
        if (result) {
            logger.info(createTableSQL);
        }
        this.getService().closeDatabaseConnection();
    }

    boolean insertStatus(Status status, CommodityInput commodityInput) throws Exception {
        Connection dbConnection = this.getService().connectToDatabase();
        String insertSQL = "INSERT INTO " + SOCIAL_MEDIA_TABLE + " (id, comodity_input_id, text, created_date) VALUES (?, ?, ?, ?)";
        PreparedStatement statement = dbConnection.prepareStatement(insertSQL);
        statement.setString(1, String.valueOf(status.getId()));
        statement.setLong(2, commodityInput.getId());
        statement.setString(3, status.getText());
        statement.setDate(4, new java.sql.Date(status.getCreatedAt().getTime()));
        logger.debug(statement);
        boolean success = statement.execute();
        if (statement != null) statement.close();
        this.getService().closeDatabaseConnection();
        return success;
    }

    public List<CommodityInput> getPriceReport(int minutes) throws Exception {
        List<CommodityInput> list = new ArrayList<CommodityInput>();
        List<Status> statuses = twitter.getMentionsTimeline();

        Map<String, Long> regionMap = this.service.getRegionMap();
        Map<String, Long> commodityMap = this.service.getCommodityMap();

        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, -1 * minutes);

        int count = 0;
        for (Status status : statuses) {
            Matcher m = PATTERN_PRICE_REPORT.matcher(status.getText());
            if (m.find() && m.groupCount() == 4 && status.getCreatedAt().after(now.getTime())) {
                logger.info(status.getUser().getName() + ":" + status.getText());

                // nama dan id
                String name = m.group(1);
                Long commodityId = findKeyInMap(name, commodityMap);

                // jika tidak bisa dideteksi, abaikan
                if (commodityId != null) {
                    String priceStr = m.group(2);
                    // buang sen ",00"
                    int commaIndex = priceStr.indexOf(",");
                    if (commaIndex > 0) {
                        priceStr = priceStr.substring(0, priceStr.indexOf(","));
                    }
                    priceStr = priceStr.replaceAll("\\.", "");
                    double price = Double.parseDouble(priceStr);
                    String priceUnit = m.group(3);
                    String location = m.group(4);

                    Double lat = DEFAULT_LAT;
                    Double lng = DEFAULT_LNG;
                    if (status.getGeoLocation() != null) {
                        lat = status.getGeoLocation().getLatitude();
                        lng = status.getGeoLocation().getLongitude();
                    }

                    // TODO cari region yang bener
                    Long regionId = this.findKeyInMap(location, regionMap);
                    regionId = (regionId != null) ? regionId : regionMap.get(DEFAULT_REGION);

                    // tanda input dari twitter
                    String desc = this.getDesc(status);

                    // TODO cari user yang bener
                    User user = service.getUser(DEFAULT_USER);
                    CommodityInput input = new CommodityInput(commodityId, user, name, location, price, lat, lng, regionId, desc, new Date());
                    list.add(input);
                    // reply
                    try {
                        Status reply = twitter.updateStatus(String.format(THANKYOU_REPLY, status.getUser().getScreenName(), input.toString()));
                        logger.info("Tweet replied: " + reply.getText());
                    } catch (Exception e1) {
                        logger.error(e1.getMessage());
                    }

                    count++;
                } else {
                    logger.warn("Cannot find commodity: " + name);
                }

            }

        }

        logger.info("Processed price report tweet: " + count);

        return list;
    }

    private String getDesc(Status status) {
        return  "tw_id:"+status.getId()+";tw_user:"+status.getUser().getScreenName();
    }

    private Long findKeyInMap(String str, Map<String, Long> map) {
        Long found = null;
        for (Map.Entry<String, Long> entry:map.entrySet()) {
            if(entry.getKey().contains(str.toUpperCase())) {
                found = entry.getValue();
                break;
            }
        }
        return found;
    }

    public String getTweetMessage(CommodityInput commodityInput) {
        Locale locale = new Locale("in", "ID");
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", locale);
        Currency idr = Currency.getInstance("IDR");
        NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
        nf.setCurrency(idr);
        return String.format("%s %s/kg di %s (%f, %f) dilaporkan %s %s", new Object[]{commodityInput.getName(), nf.format(commodityInput.getPrice()), commodityInput.getLocation(), Double.valueOf(commodityInput.getGeo().getLat()), Double.valueOf(commodityInput.getGeo().getLng()), commodityInput.getUser().getName(), sdf.format(commodityInput.getCreatedAt())});
    }

    public void checkReport(int minutes) throws Exception {
        List<CommodityInput> commodityInputList = this.getPriceReport(minutes);
        this.service.batchInsertCommodityInput(commodityInputList);
    }

    @Override
    public void postSingleTodayInput() throws Exception {
        CommodityInput input = this.getInputToBePosted(SOCIAL_MEDIA_TABLE);
        if (input != null) {
            Status status = twitter.updateStatus(this.getTweetMessage(input));
            logger.info("Tweeted: " + status.getText());
            this.insertStatus(status, input);
        } else {
            logger.info("Nothing to be tweeted yet");
        }
    }
}
