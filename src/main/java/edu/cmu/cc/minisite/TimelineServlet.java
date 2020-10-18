package edu.cmu.cc.minisite;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * In this task you will populate a user's timeline.
 * This task helps you understand the concept of fan-out. 
 * Practice writing complex fan-out queries that span multiple databases.
 *
 * Task 4 (1):
 * Get the name and profile of the user as you did in Task 1
 * Put them as fields in the result JSON object
 *
 * Task 4 (2);
 * Get the follower name and profiles as you did in Task 2
 * Put them in the result JSON object as one array
 *
 * Task 4 (3):
 * From the user's followees, get the 30 most popular comments
 * and put them in the result JSON object as one JSON array.
 * (Remember to find their parent and grandparent)
 *
 * The posts should be sorted:
 * First by ups in descending order.
 * Break tie by the timestamp in descending order.
 */
public class TimelineServlet extends HttpServlet {

    /**
     * Your initialization code goes here.
     */
    public TimelineServlet() {
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

        // DON'T modify this method.
        String id = request.getParameter("id");
        String result = getTimeline(id);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.print(result);
        writer.close();
    }

    /**
     * Method to get given user's timeline.
     *
     * @param id user id
     * @return timeline of this user
     */
    private String getTimeline(String id) {
        JsonObject result = new JsonObject();
        // TODO: implement this method
        // JsonArray followers = getFollowers(id);
        // String profile_image_url = getUrl(id);
        // JsonArray comments = get30Comments(followers);

        // result.addProperty("followers", followers);
        // result.addProperty("comments", comments);
        // result.addProperty("profile", profile_image_url);
        // result.addProperty("name", id);
        return result.toString();
    }

    // public JsonArray get30Comments(JsonArray followers) {
    //     JsonArray comments = new JsonArray();
    //     JsonObject parentComment, grandParentComment;
    //     Filters filter = Filters.eq("uid", id); //TODO
    //     Sorts sort = Sorts.descending("timestamp", "ups");
    //     Projections projection = Projections.fields(Projections.excludeId());
        
    //     MongoCursor<Document> cursor = collection
    //                     .find(filter)
    //                     .sort(sort)
    //                     .projection(projection)
    //                     .limit(30)
    //                     .iterator();

    //     try {
    //         while (cursor.hasNext()) {
    //              JsonObject comment = new JsonParser().parse(cursor.next().toJson()).getAsJsonObject();
    //              //TODO
    //              comments.add(comment);
    //          }
    //      } finally {
    //          cursor.close();
    //      }

    //     return comments;
    // }

    // public String getUrl(String id) {
    //     String profile_image_url;
    //     Map<String,Object> parameters = Collections.singletonMap( "username", id );

    //     try (Session session = driver.session()) {
    //         StatementResult rs = session.run("MATCH (follower:User)-[r:FOLLOWS]->(followee:User) WHERE followee.username = $username RETURN followee", parameters);
    //         record = rs.next();
    //         profile_image_url = record.get(0).get("url").asString();
    //     }
    //     return profile_image_url;
    // }


    // public JsonArray getFollowers(String id) {
    //     JsonArray followers = new JsonArray();
    //     // TODO: To be implemented
    //     JsonObject follower = new JsonObject();
    //     String follower_name, profile_image_url;
    //     Record record;
    //     Map<String,Object> parameters = Collections.singletonMap( "username", id );

    //     try (Session session = driver.session()) {
    //         StatementResult rs = session.run("MATCH (follower:User)-[r:FOLLOWS]->(followee:User) WHERE followee.username = $username RETURN follower ORDER BY follower.username", parameters);
    //         while (rs.hasNext()) {
    //             record = rs.next();
    //             follower = new JsonObject();

    //             follower_name = record.get(0).get("username").asString();
    //             profile_image_url = record.get(0).get("url").asString();

    //             follower.addProperty("profile", profile_image_url);
    //             follower.addProperty("name", follower_name);
    //             followers.add(follower);

    //         }
    //     }
    
    //     return followers;
    // }
}

