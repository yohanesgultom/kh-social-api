package id.kawalharga.social;

import id.kawalharga.database.Service;
import id.kawalharga.model.CommodityInput;
import id.kawalharga.model.Geolocation;
import id.kawalharga.model.User;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Created by yohanesgultom on 17/06/16.
 */
public abstract class AbstractService {

    static final Logger logger = Logger.getLogger(AbstractService.class);
    static final int NUMBER_OF_TODAY_POST_TO_CHECK = 10;
    static final int NUMBER_OF_PREV_POST_TO_CHECK = 2;

    protected static final String GOOGLE_MAP_URL = "http://maps.google.com/maps?q=%f,%f";
    protected id.kawalharga.database.Service service;

    public AbstractService(String dbConfig) throws Exception {
        service = id.kawalharga.database.Service.getInstance(dbConfig);
    }

    public Service getService() {
        return service;
    }

    public abstract void postSingleTodayInput() throws Exception;
    public abstract List<CommodityInput> getLastPostedCommodityInput(int limit) throws Exception;

    public String getGoogleMapUrlString(Geolocation geolocation) {
        return String.format(GOOGLE_MAP_URL, geolocation.getLat(), geolocation.getLng());
    }

    public CommodityInput getInputToBePosted(String socialMediaTable) throws Exception {
        CommodityInput res = null;
        Calendar beginningOfDay = new GregorianCalendar();
        beginningOfDay.set(Calendar.HOUR, 0);
        beginningOfDay.set(Calendar.MINUTE, 0);
        beginningOfDay.set(Calendar.SECOND, 0);
        List<CommodityInput> lastInputs = this.getLastPostedCommodityInput(NUMBER_OF_PREV_POST_TO_CHECK);
        List<CommodityInput> commodityInputList = this.service.getInputsToBePosted(beginningOfDay.getTime(), NUMBER_OF_TODAY_POST_TO_CHECK, socialMediaTable);
        if (commodityInputList != null && commodityInputList.size() > 0) {
            // top result as default
            res = commodityInputList.get(0);
            for (CommodityInput input:commodityInputList) {
                boolean different = true;
                for (CommodityInput lastInput:lastInputs) {
                    // find one with different location and commodity name
                    if (input.getGeo().getId() == lastInput.getGeo().getId()) {
                        different = false;
                        break;
                    }
                }
                if (different) {
                    res = input;
                    break;
                }
            }
        }
        return res;
    }

    protected List<CommodityInput> getLastPostedCommodityInput(String socialMediaTable, int limit) throws Exception {
        List<CommodityInput> commodityInputList = new ArrayList<>();
        Connection dbConnection = this.getService().connectToDatabase();
        String selectSQL = "select i.id, " +
                "c.name, " +
                "r.id as location_id, " +
                "r.name as location, " +
                "i.price, " +
                "i.amount, " +
                "i.lat, " +
                "i.lng, " +
                "i.description, " +
                "i.date_created, " +
                "u.id as user_id," +
                "u.nama as user_name," +
                "u.username as user_username," +
                "u.alamat as user_address," +
                "u.nohp as user_phone," +
                "u.kodepos as user_postal_code," +
                "u.email as user_email " +
                "from comodity_input i join auth_user u on i.user_id = u.id " +
                "join comodity c on i.comodity_name_id = c.id " +
                "join region r on i.region_id = r.id " +
                "where i.id in (select comodity_input_id from " + socialMediaTable + " order by created_date desc limit " + limit + ")";
        Statement statement = dbConnection.createStatement();
        ResultSet rs = statement.executeQuery(selectSQL);
        while (rs.next()) {
            User user = new User(Long.valueOf(rs.getLong("user_id")), rs.getString("user_username"), rs.getString("user_name"), rs.getString("user_address"), rs.getString("user_phone"), rs.getString("user_postal_code"), rs.getString("user_email"));
            CommodityInput commodityInput = new CommodityInput(Long.valueOf(rs.getLong("id")), user, rs.getString("name"), rs.getString("location"), rs.getDouble("price"), rs.getDouble("lat"), rs.getDouble("lng"), Long.valueOf(rs.getLong("location_id")), rs.getString("description"), rs.getDate("date_created"));
            logger.debug("Last input: " + commodityInput);
            commodityInputList.add(commodityInput);
        }
        if (rs != null) rs.close();
        if (statement != null) statement.close();
        this.getService().closeDatabaseConnection();
        return commodityInputList;
    }

}
