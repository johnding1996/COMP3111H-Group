package database.querier;

import database.connection.SQLPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;


/**
 * {@link Querier}
 * Abstract super-class of all queriers.
 * Handles sql JDBC connections and provide I/O method interfaces.
 * @author mcding
 * @version 1.1
 */
@Slf4j
public abstract class Querier {
    protected Connection sql;

    /**
     * constructor
     * Connect to redis server and create the instance's jedis instance.
     */
    Querier() {
        sql = SQLPool.getConnection();
        try {
            sql.setAutoCommit(true);
        } catch (SQLException e) {
            log.error("Failed to set SQL connection to auto-commit.", e);
        } catch (NullPointerException e) {
            log.error("");
        }
    }


    /**
     * close
     * Close the connection once it is not used anymore.
     */
    public void close() {
        SQLPool.closeConnection(sql);
    }

    /**
     * finalize
     * Override the original finalizer to check whether the connection is closed.
     */
    @Override
    public void finalize() throws java.lang.Throwable {
        if (!sql.isClosed()) {
            log.error("SQL connection is not closed when destroying the Keeper class.");
        }
        super.finalize();
    }


    /**
     * get
     * Abstract get method, to be override by sub-class methods.
     * @param key string to search
     * @return JSONArray as the search result
     */
    public abstract JSONArray get(String key);

    /**
     * set
     * Abstract set method, to be override by sub-class methods.
     * @param jsonArray JSONArray as the information to store
     * @return whether set successfully or not
     */
    public abstract boolean set(JSONArray jsonArray);

    /**
     * executeQuery
     * Utility method to execute arbitrary query.
     * @param query string to execute
     * @return ResultSet containing the result
     */
    protected ResultSet executeQuery(String query) {
        try {
            PreparedStatement stmt = sql.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            rs.close();
            stmt.close();
            return rs;
        } catch (SQLException e) {
            log.error(String.format("Failed to execute query: %s.", query), e);
            return null;
        }
    }

    /**
     * parseResult
     * Utility method to parse result set into a JSONArray of flat JSONObjects.
     * @param rs resultSet of query
     * @param fields list of field string
     * @return JSONArray as the parsed results
     */
    protected JSONArray parseResult(ResultSet rs, List<String> fields) {
        JSONArray jsonArray = new JSONArray();
        try{
            while (rs.next()) {
                JSONObject jsonObject = new JSONObject();
                for (String field: fields) {
                    jsonObject.put(field, rs.getString(field));
                }
                jsonArray.put(jsonObject);
            }
            return jsonArray;
        } catch (SQLException e) {
            log.error("Failed to parse result set due to SQL exception." , e);
            return null;
        }
    }

    /**
     * insertData
     * Insert data in JSONArray to corresponding table following the list of fields.
     * @param table table to insert to
     * @param fields list of fields
     * @param jsonArray JSONArray which contains data rows (each as a JSONObject) to insert
     * @return whether insert data successfully or not
     */
    protected boolean insertData(String table, List<String> fields, JSONArray jsonArray) {
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject foodJson = jsonArray.getJSONObject(i);
            List<String> values = new ArrayList<>();
            for (String field: fields) {
                values.add(foodJson.getString(field));
            }
            String query = String.format("INSERT INTO %s (%s) VALUES (%s);",
                    table, String.join(", ", fields), String.join(", ", values));
            ResultSet rs = executeQuery(query);
            if (rs == null) {
                return false;
            }
        }
        return true;
    }

}
