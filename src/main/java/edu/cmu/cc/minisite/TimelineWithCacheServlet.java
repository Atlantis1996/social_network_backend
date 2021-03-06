package edu.cmu.cc.minisite;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;

import com.google.gson.JsonElement;


import java.util.Objects;




import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import java.util.ArrayList; 



import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


import java.util.Collections;
import java.util.Map;




import org.bson.Document;
import org.bson.conversions.Bson;

import com.google.gson.JsonParser;
import com.mongodb.client.MongoCursor;

import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;


/**
 * In this task you will populate a user's timeline.
 * This task helps you understand the concept of fan-out and caching.
 * Practice writing complex fan-out queries that span multiple databases.
 * Also practice using caching mechanism to boost your backend!
 *
 * Task 5 (1):
 * Get the name and profile of the user as you did in Task 1
 * Put them as fields in the result JSON object
 *
 * Task 5 (2);
 * Get the follower name and profiles as you did in Task 2
 * Put them in the result JSON object as one array
 *
 * Task 5 (3):
 * From the user's followees, get the 30 most popular comments
 * and put them in the result JSON object as one JSON array.
 * (Remember to find their parent and grandparent)
 *
 * Task 5 (4):
 * Make sure your implementation can finish a request that is sent
 * before in a short time.
 *
 * The posts should be sorted:
 * First by ups in descending order.
 * Break tie by the timestamp in descending order.
 */
public class TimelineWithCacheServlet extends HttpServlet {

    /**
     * You need to use this variable to implement your caching
     * mechanism. Please see {@link Cache#put}, {@link Cache#get}.
     *
     */
    private static Cache cache = new Cache();

    /**
     * Your initialization code goes here.
     */

    /**
     * The Neo4j driver.
     */
    private final Driver driver;

    /**
     * The endpoint of the database.
     *
     * To avoid hardcoding credentials, use environment variables to include
     * the credentials.
     *
     * e.g., before running "mvn clean package exec:java" to start the server
     * run the following commands to set the environment variables.
     * export NEO4J_HOST=...
     * export NEO4J_NAME=...
     * export NEO4J_PWD=...
     */
    private static final String NEO4J_HOST = System.getenv("NEO4J_HOST");
    /**
     * Neo4J username.
     */
    private static final String NEO4J_NAME = System.getenv("NEO4J_NAME");
    /**
     * Neo4J Password.
     */
    private static final String NEO4J_PWD = System.getenv("NEO4J_PWD");
    
    /**
     * The endpoint of the database.
     *
     * To avoid hardcoding credentials, use environment variables to include
     * the credentials.
     *
     * e.g., before running "mvn clean package exec:java" to start the server
     * run the following commands to set the environment variables.
     * export MONGO_HOST=...
     */
    private static final String MONGO_HOST = System.getenv("MONGO_HOST");
    /**
     * MongoDB server URL.
     */
    private static final String URL = "mongodb://" + MONGO_HOST + ":27017";
    /**
     * Database name.
     */
    private static final String DB_NAME = "reddit_db";
    /**
     * Collection name.
     */
    private static final String COLLECTION_NAME = "posts";
    /**
     * MongoDB connection.
     */
    private static MongoCollection<Document> collection;
    /**
     * Your initialization code goes here.
     */
    public TimelineWithCacheServlet() {
        Objects.requireNonNull(NEO4J_HOST);
        Objects.requireNonNull(NEO4J_NAME);
        Objects.requireNonNull(NEO4J_PWD);
        driver = getDriver();

        Objects.requireNonNull(MONGO_HOST);
        MongoClientURI connectionString = new MongoClientURI(URL);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        collection = database.getCollection(COLLECTION_NAME);
    }

    private Driver getDriver() {
        Objects.requireNonNull(NEO4J_HOST);
        Objects.requireNonNull(NEO4J_NAME);
        Objects.requireNonNull(NEO4J_PWD);
        return GraphDatabase.driver(
                "bolt://" + NEO4J_HOST + ":7687",
                AuthTokens.basic(NEO4J_NAME, NEO4J_PWD));
    }

    /**
     * Don't modify this method.
     *
     * @param request  the request object that is passed to the servlet
     * @param response the response object that the servlet
     *                 uses to return the headers to the client
     * @throws IOException      if an input or output error occurs
     * @throws ServletException if the request for the HEAD
     *                          could not be handled
     */
    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException {

        // DON'T modify this method
        String id = request.getParameter("id");
        String result = getTimeline(id);

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("CacheHit", String.valueOf(cache.get(id) != null));
        PrintWriter writer = response.getWriter();
        writer.print(result);
        writer.close();
    }

    /**
     * Method to get given user's timeline.
     * You are required to implement caching mechanism with
     * given cache variable.
     *
     * @param id user id
     * @return timeline of this user
     */

    private String getTimeline(String id) {
        // TODO: implement this method
        JsonObject result = new JsonObject();
        // Use the given cache variable to implement your
        // caching mechanism.
        String rs = cache.get(id);
        if (rs != null) {
            return rs;
        }
        JsonArray followers = getFollowers(id);
        JsonArray followees = getFollowees(id);
        String profileImageUrl = getUrl(id);
        JsonArray comments = get30Comments(followees);

        result.add("followers", followers);
        result.add("comments", comments);
        result.addProperty("profile", profileImageUrl);
        result.addProperty("name", id);
        if (followers.size() > 300) {
            cache.put(id, result.toString());
        }
        return result.toString();
    }

    public JsonArray get30Comments(JsonArray followees) {
        JsonArray comments = new JsonArray();
        JsonObject parentComment, grandParentComment;
        Bson filter = Filters.eq("uid", "dummy"); 
        String name, parentId, grandParentId;            
        JsonObject followee;
        ArrayList<Bson> filterList = new ArrayList<>();

        for (JsonElement fl : followees) {
            followee = fl.getAsJsonObject();
            name = followee.get("name").getAsString();
            // filter = Filters.or(filter, Filters.eq("uid", name));
            filter = Filters.eq("uid", name);
            filterList.add(filter);
        }

        if (filterList.size() == 0) {
            return comments;
        }
        
        MongoCursor<Document> cursor = collection
                        .find(Filters.or(filterList))
                        .sort(Sorts.descending("ups", "timestamp"))
                        .projection(Projections.fields(Projections.excludeId()))
                        .limit(30)
                        .iterator();
        try {
            while (cursor.hasNext()) {
                JsonObject comment = new JsonParser().parse(cursor.next().toJson()).getAsJsonObject();
              //TODO   
                parentId = comment.get("parent_id").getAsString();
                parentComment = null;
                parentComment = getParentComment(parentId);
                if (parentComment != null) {
                    comment.add("parent", parentComment);
                    grandParentId = parentComment.get("parent_id").getAsString();
                    grandParentComment = null;
                    grandParentComment = getParentComment(grandParentId);
                    if (grandParentComment != null) {
                        comment.add("grand_parent", grandParentComment);
                    }
                }
                comments.add(comment);
             }    
         } catch (Exception e) {
            e.printStackTrace();
         } finally {
             cursor.close();
         }

        return comments;
    }

    public JsonObject getParentComment(String parentId) {
        JsonObject parentComment = null;
        MongoCursor<Document> parentCursor = collection
                                    .find(Filters.eq("cid", parentId))
                                    .projection(Projections.fields(Projections.excludeId()))
                                    .iterator();
        try {
            while (parentCursor.hasNext()) {
                    parentComment = new JsonParser().parse(parentCursor.next().toJson()).getAsJsonObject();
                }
        } finally {
            parentCursor.close();
        }
        return parentComment;
    }

    public String getUrl(String id) {
        String profileImageUrl;
        Map<String,Object> parameters = Collections.singletonMap("username", id);
        Record record;
        try (Session session = driver.session()) {
            StatementResult rs = session
                                .run("MATCH (follower:User)-[r:FOLLOWS]->(followee:User) " 
                                + "WHERE followee.username = $username RETURN followee", parameters);
            record = rs.next();
            profileImageUrl = record.get(0).get("url").asString();
        }
        return profileImageUrl;
    }


    public JsonArray getFollowers(String id) {
        JsonArray followers = new JsonArray();
        JsonObject follower = new JsonObject();
        String followerName, profileImageUrl;
        Record record;
        Map<String,Object> parameters = Collections.singletonMap("username", id);

        try (Session session = driver.session()) {
            StatementResult rs = session
                                        .run("MATCH (follower:User)-[r:FOLLOWS]->(followee:User) " 
                                        + "WHERE followee.username = $username " 
                                        + "RETURN follower ORDER BY follower.username", parameters);
            while (rs.hasNext()) {
                record = rs.next();
                follower = new JsonObject();

                followerName = record.get(0).get("username").asString();
                profileImageUrl = record.get(0).get("url").asString();

                follower.addProperty("profile", profileImageUrl);
                follower.addProperty("name", followerName);
                followers.add(follower);

            }
        }
    
        return followers;
    }

    public JsonArray getFollowees(String id) {
        JsonArray followees = new JsonArray();
        JsonObject followee = new JsonObject();
        String followeeName, profileImageUrl;
        Record record;
        Map<String,Object> parameters = Collections.singletonMap("username", id);

        try (Session session = driver.session()) {
            StatementResult rs = session
                                .run("MATCH (follower:User)-[r:FOLLOWS]->(followee:User) " 
                                + "WHERE follower.username = $username " 
                                + "RETURN followee " 
                                + "ORDER BY follower.username", parameters);
            while (rs.hasNext()) {
                record = rs.next();
                followee = new JsonObject();

                followeeName = record.get(0).get("username").asString();
                profileImageUrl = record.get(0).get("url").asString();

                followee.addProperty("profile", profileImageUrl);
                followee.addProperty("name", followeeName);
                followees.add(followee);

            }
        }
    
        return followees;
    }
}

