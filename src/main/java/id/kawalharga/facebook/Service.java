package id.kawalharga.facebook;

import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.Post;
import facebook4j.auth.AccessToken;
import id.kawalharga.model.CommodityInput;
import id.kawalharga.model.Geolocation;
import id.kawalharga.model.User;
import id.kawalharga.social.AbstractService;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class Service extends AbstractService {

    static final Logger logger = Logger.getLogger(Service.class);
    static final String FACEBOOK4J_UPDATED = "facebook4j.updated.properties";
    static final String FACEBOOK4J_OAUTH_TOKEN_FIELD = "oauth.accessToken";
    static final String SOCIAL_MEDIA_TABLE = "post_fb";

    private Facebook facebook;

    public Service(String dbConfig) throws Exception {
        super(dbConfig);
        facebook = new FacebookFactory().getInstance();

        // create updated config file if not exist
        File facebook4jConfig = new File(FACEBOOK4J_UPDATED);
        if(!facebook4jConfig.exists()) {
            facebook4jConfig.createNewFile();
            logger.info("New config file created at: " + facebook4jConfig.getAbsolutePath());
        }

        // load properties
        FileInputStream input = new FileInputStream(facebook4jConfig);
        logger.info("Updated config file loaded from: " + facebook4jConfig.getAbsolutePath());
        Properties facebook4jProp = new Properties();
        facebook4jProp.load(input);
        input.close();
        if (facebook4jProp.getProperty(FACEBOOK4J_OAUTH_TOKEN_FIELD) != null) {
            facebook.setOAuthAccessToken(new AccessToken(facebook4jProp.getProperty(FACEBOOK4J_OAUTH_TOKEN_FIELD), null));
        }

        logger.info("Renewing access token");
        String shortLivedToken = facebook.getOAuthAccessToken().getToken();
        AccessToken extendedToken = facebook.extendTokenExpiration(shortLivedToken);
        facebook.setOAuthAccessToken(extendedToken);

        // save extended token
        FileOutputStream out = new FileOutputStream(facebook4jConfig);
        facebook4jProp.setProperty(FACEBOOK4J_OAUTH_TOKEN_FIELD, extendedToken.getToken());
        facebook4jProp.store(out, null);
        out.close();


        this.createTableIfNotExist();
    }

    public id.kawalharga.database.Service getService() {
        return service;
    }

    public void renewAccessToken() throws Exception {
        logger.info("Renewing access token");
        String shortLivedToken = facebook.getOAuthAccessToken().getToken();
        AccessToken extendedToken = facebook.extendTokenExpiration(shortLivedToken);
        facebook.setOAuthAccessToken(extendedToken);
    }

    public String post(CommodityInput commodityInput) throws Exception {
        String id = null;
        URL googleMapUrl = new URL(this.getGoogleMapUrlString(commodityInput.getGeo()));
        try {
            String message = commodityInput.toString();
            id = facebook.postLink(googleMapUrl, message);
            logger.info("Posted: " + id);
        } catch (Exception e) {
            throw e;
        }
        return id;
    }

    public Post getPost(String id) throws Exception {
        return facebook.getPost(id);
    }

    void createTableIfNotExist() throws Exception {
        Connection connection = this.getService().connectToDatabase();
        Statement statement = connection.createStatement();
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + SOCIAL_MEDIA_TABLE + "("
                + "id VARCHAR(50) NOT NULL, "
                + "comodity_input_id BIGINT NOT NULL, "
                + "text VARCHAR(500) NOT NULL, "
                + "likes INT DEFAULT 0, "
                + "dislikes INT DEFAULT 0, "
                + "neutrals INT DEFAULT 0, "
                + "created_date DATE NOT NULL, " + "PRIMARY KEY (id) "
                + ")";
        boolean result = statement.execute(createTableSQL);
        if (result) {
            logger.info(createTableSQL);
        }
        this.getService().closeDatabaseConnection();
    }

    boolean insertPost(Post post, CommodityInput commodityInput) throws Exception {
        Connection dbConnection = this.getService().connectToDatabase();
        String insertSQL = "INSERT INTO " + SOCIAL_MEDIA_TABLE + " (id, comodity_input_id, text, created_date) VALUES (?, ?, ?, ?)";
        PreparedStatement statement = dbConnection.prepareStatement(insertSQL);
        statement.setString(1, post.getId());
        statement.setLong(2, commodityInput.getId());
        statement.setString(3, post.getMessage());
        statement.setDate(4, new java.sql.Date(post.getCreatedTime().getTime()));
        logger.debug(statement);
        boolean success = statement.execute();
        if (statement != null) statement.close();
        this.getService().closeDatabaseConnection();
        return success;
    }

    Date getLastPostedInputCreatedDate() throws Exception {
        Date lastId = null;
        Connection dbConnection = this.getService().connectToDatabase();
        String selectSQL = "SELECT created_date FROM " + SOCIAL_MEDIA_TABLE + " ORDER BY created_date DESC LIMIT 1";
        Statement statement = dbConnection.createStatement();
        logger.debug(statement);
        ResultSet rs = statement.executeQuery(selectSQL);
        if (rs.next()) {
            lastId = rs.getDate("created_date");
            logger.info("Last post id: " + lastId);
        }
        if (rs != null) rs.close();
        if (statement != null) statement.close();
        this.getService().closeDatabaseConnection();
        return lastId;
    }

    @Override
    public void postSingleTodayInput() throws Exception {
        CommodityInput input = this.getInputToBePosted(SOCIAL_MEDIA_TABLE);
        if (input != null) {
            String postId = this.post(input);
            Post post = this.getPost(postId);
            this.insertPost(post, input);
        } else {
            logger.info("Nothing to be posted yet");
        }
    }

    public List<String> getLastPostIds(int limit) throws Exception {
        List<String> postIds = new ArrayList<>();
        Connection dbConnection = this.getService().connectToDatabase();
        String selectSQL = "SELECT id FROM " + SOCIAL_MEDIA_TABLE + " ORDER BY created_date DESC LIMIT " + limit;
        Statement statement = dbConnection.createStatement();
        ResultSet rs = statement.executeQuery(selectSQL);
        while (rs.next()) {
            postIds.add(rs.getString("id"));
        }
        if (rs != null) rs.close();
        if (statement != null) statement.close();
        this.getService().closeDatabaseConnection();
        return postIds;
    }

    public void updatePostsStatus(List<String> postIds) throws Exception {
        logger.info("Updating posts status");
        Connection dbConnection = this.getService().connectToDatabase();
        String sql = "UPDATE " + SOCIAL_MEDIA_TABLE + " SET likes = ?, dislikes = ?, neutrals = ? WHERE id = ?";
        PreparedStatement statement = dbConnection.prepareStatement(sql);
        for (String postId:postIds) {
            try {
                Post post = this.getPost(postId);
                logger.debug("FB_ID " + postId + " likes: " + post.getLikes().getCount());
                statement.setInt(1, post.getLikes().size()); // TODO include positive reactions
                statement.setInt(2, 0); // TODO
                statement.setInt(3, 0); // TODO
                statement.setString(4, postId);
                boolean success = statement.execute();
            } catch (Exception e) {
                // ignore failure
                // e.printStackTrace();
            }
        }
        if (statement != null) statement.close();
        this.getService().closeDatabaseConnection();
    }

    @Override
    public List<CommodityInput> getLastPostedCommodityInput(int limit) throws Exception {
        return this.getLastPostedCommodityInput(SOCIAL_MEDIA_TABLE, limit);
    }
}
