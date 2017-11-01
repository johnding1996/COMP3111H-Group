package database.querier;

import database.connection.SQLPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
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
     * table
     * Table name of a specific Querier.
     */
    protected String table;

    /**
     * idx_field
     * Name of index field of the table.
     */
    protected String idx_field;

    /**
     * desc_field
     * Name of description field of the table.
     */
    protected String desc_field;

    /**
     * fields
     * List of all fields of the table.
     */
    protected List<String> fields;

    /**
     * critical_fields
     * Set of all not-nullable fields of the table.
     * Also the set of minimum fields in the corresponding JSONObject.
     */
    protected Set<String> critical_fields;

    /**
     * queryLimit
     * The maximum number of rows returned when use search method.
     */
    protected int queryLimit;

    /**
     * constructor
     * Connect to redis server and create the instance's jedis instance.
     */
    Querier() {
        sql = SQLPool.getConnection();
        try {
            sql.setAutoCommit(true);
        } catch (SQLException e) {
            log.error("Failed to add SQL connection to auto-commit.", e);
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

    /*
    /**
     * finalize
     * Override the original finalizer to check whether the connection is closed.
     * Deprecated according to CodeFactor style suggestion.
     */
    /*
    @Override
    public void finalize() throws java.lang.Throwable {
        if (!sql.isClosed()) {
            log.error("SQL connection is not closed when destroying the Keeper class.");
        }
        super.finalize();
    }
    */

    /**
     * get
     * Base get all method.
     * @return JSONArray of all rows in the table
     */
    public JSONArray get() {
        String query = String.format("SELECT * FROM %s", table);
        ResultSet rs = executeQuery(query);
        return parseResult(rs, fields, critical_fields);
    }

    /**
     * get
     * Base get method.
     * @param key index object
     * @return JSONObject of the corresponding row
     */
    public JSONObject get(Object key) {
        String keyString = parseKey(key);
        if (keyString == null) {
            return new JSONObject();
        }
        String query = String.format("SELECT * FROM %s WHERE %s = '%s' LIMIT %d;", table, idx_field, keyString, 1);
        ResultSet rs = executeQuery(query);
        return parseResult(rs, fields, critical_fields).getJSONObject(0);
    }

    /**
     * search
     * Base search method, to be override by sub-class methods.
     * @param desc description string
     * @return JSONArray as the search result
     */
    public JSONArray search(String desc) {
        String query = String.format("SELECT * FROM %s WHERE %s = '%s' LIMIT %d;", table, desc_field, desc, queryLimit);
        ResultSet rs = executeQuery(query);
        return parseResult(rs, fields, critical_fields);
    }


    /**
     * add
     * Base add method, to be override by sub-class methods.
     * @param jsonArray JSONArray as the information to store
     * @return whether add successfully or not
     */
    public boolean add(JSONArray jsonArray) {
        List<String> rows = new ArrayList<>();
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject jsonObject;
            try {
                jsonObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                log.error("Parsing JSONArray failed when get JSONObject.", e);
                return false;
            }
            Map<String, String> map = parseInput(jsonObject, fields, critical_fields, true);
            if (map == null) {
                log.error(String.format("Add row %s to table %s failed when parsing.", jsonObject.toString(), table));
                return false;
            }
            List<String> values = new ArrayList<>(map.values());
            rows.add("(" + String.join(", ", values) + ")");
        }
        String query = String.format("INSERT INTO %s (%s) VALUES %s;",
                table, String.join(", ", fields), String.join(", ", rows));
        return executeUpdate(query);
    }

    /**
     * delete
     * Base delete method.
     * @param key index object
     * @return whether deleting successfully or not
     */
    public boolean delete(Object key) {
        String keyString = parseKey(key);
        if (keyString == null) {
            return false;
        }
        String query = String.format("DELETE FROM %s WHERE %s = '%s';", table, idx_field, keyString);
        return executeUpdate(query);
    }

    /**
     * update
     * Abstract update method, to be override by sub-class methods.
     * @param jsonArray JSONArray as the information to update
     * @return whether update successfully or not
     */
    public boolean update(JSONArray jsonArray) {
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject jsonObject;
            try {
                jsonObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                log.error("Parsing JSONArray failed when get JSONObject.", e);
                return false;
            }
            Map<String, String> map = parseInput(jsonObject, fields, new HashSet<>(Arrays.asList(idx_field)), false);
            if (map == null) {
                log.error(String.format("Update row %s of table %s failed when parsing.", jsonObject.toString(), table));
                return false;
            }
            List<String> assignments = new ArrayList<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if(!idx_field.equals(entry.getKey())) {
                    assignments.add(entry.getKey() + " = " + entry.getValue());
                }
            }

            String query = String.format("UPDATE %s SET %s WHERE %s='%s';",
                    table, String.join(", ", assignments), idx_field, map.get(idx_field));
            if (!executeUpdate(query)){
                log.error(String.format("Failed to update row with index %s of table %s when executing SQL query.",
                        map.get(idx_field), table));
            }
        }
        return true;
    }

    /**
     * executeQuery
     * Utility method to execute arbitrary query which returns result add.
     * @param query statement to execute
     * @return ResultSet containing the result
     */
    protected ResultSet executeQuery(String query) {
        try {
            PreparedStatement stmt = sql.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            stmt.closeOnCompletion();
            return rs;
        } catch (SQLException e) {
            log.error(String.format("Failed to execute query: %s.", query), e);
            return null;
        }
    }

    /**
     * executeUpdate
     * Utility method to execute arbitrary query which does not return result add.
     * @param query statement to execute
     * @return whether the statement is executed successfully or not
     */
    protected boolean executeUpdate(String query) {
        try{
            PreparedStatement stmt = sql.prepareStatement(query);
            stmt.executeUpdate();
            stmt.close();
            return true;
        } catch (SQLException e) {
            log.error(String.format("Failed to execute query: %s.", query), e);
            return false;
        }
    }

    /**
     * parseInput
     * Utility method to parse input flat JSONObject to a map of fields to SQL literals.
     * @param jsonObject input JSONObject
     * @param fields list of fields
     * @param critical_fields set of not-nullable fields
     * @return map of fileds to values
     */
    protected Map<String, String> parseInput(JSONObject jsonObject, List<String> fields, Set<String> critical_fields, boolean isStrict) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String field: fields) {
            if (jsonObject.has(field)) {
                Object value = jsonObject.get(field);
                if (value instanceof String) {
                    map.put(field, "'" + jsonObject.getString(field)+ "'");
                } else if (value instanceof Integer) {
                    map.put(field, Integer.toString(jsonObject.getInt(field)));
                } else if (value instanceof Long) {
                    map.put(field, Long.toString(jsonObject.getLong(field)));
                } else if (value instanceof Double) {
                    map.put(field, Double.toString(jsonObject.getDouble(field)));
                } else if (value instanceof Boolean) {
                    map.put(field, jsonObject.getBoolean(field) ? "TRUE" : "FALSE");
                } else {
                    log.error(String.format("Encountered invalid value (%s) when parsing input Json.", value.toString()));
                    return null;
                }
            } else if (critical_fields.contains(field)) {
                log.error(String.format("Missing critical field (%s) when parsing input Json.", field));
                return null;
            } else if (isStrict) {
                map.put(field, "NULL");
            }
        }
        return map;
    }

    /**
     * parseResult
     * Utility method to parse ResultSet into a JSONArray of flat JSONObjects.
     * @param rs ResultSet of query
     * @param fields list of fields
     * @param critical_fields set of not-nullable fields
     * @return JSONArray as the parsed results
     */
    protected JSONArray parseResult(ResultSet rs, List<String> fields, Set<String> critical_fields) {
        JSONArray jsonArray = new JSONArray();
        try{
            while (rs.next()) {
                JSONObject jsonObject = new JSONObject();
                for (String field: fields) {
                    String value = rs.getString(field);
                    if (value == null) {
                        if (critical_fields.contains(field)) {
                            log.error(String.format("Failed to parse result set due to missing filed %s.", field));
                            return null;
                        } else {
                            continue;
                        }
                    }
                    if (value.equals("t") || value.equals("f")) {
                        jsonObject.put(field, value.equals("t"));
                    }
                    Object valueObject;
                    Scanner scanner = new Scanner(value);
                    if (scanner.hasNextInt()) {
                        valueObject = scanner.nextInt();
                    } else if (scanner.hasNextLong()) {
                        valueObject = scanner.nextLong();
                    } else if (scanner.hasNextDouble()) {
                        valueObject = scanner.nextDouble();
                    } else {
                        valueObject = value;
                    }
                    // Check if there is some characters left, since we are parsing a single value
                    if (!scanner.hasNext()) {
                        jsonObject.put(field, valueObject);
                    } else {
                        jsonObject.put(field, value);
                    }
                    // An unsuccessful try-catch parser
                    /*
                    try {
                        int valueInteger = Integer.parseInt(field);
                        jsonObject.put(field, valueInteger);
                    } catch (NumberFormatException e) {
                        try {
                            long valueLong = Long.parseLong(field);
                            jsonObject.put(field, valueLong);
                        } catch (NumberFormatException e0) {
                            try {
                                double valueDoble = Double.parseDouble(field);
                                jsonObject.put(field, valueDoble);
                            } catch (NumberFormatException e2) {
                                jsonObject.put(field, value);
                            }
                        }
                    }
                    */
                }
                jsonArray.put(jsonObject);
            }
            rs.close();
            return jsonArray;
        } catch (SQLException e) {
            log.error("Failed to parse result set due to SQL exception." , e);
            try{
                rs.close();
                return null;
            } catch (SQLException e0) {
                log.error("Failed to close result set due to SQL exception." , e0);
                return null;
            }
        }
    }

    /**
     * parseKey
     * Utility method to parse a key object (either String or int) to String
     * @param key input key object
     * @return key string
     */
    protected String parseKey(Object key) {
        if (key instanceof String) {
            return (String)key;
        } else if (key instanceof Integer) {
            return Integer.toString((int)key);
        } else {
            log.error(String.format("Invalid key %s", key.toString()));
            return null;
        }
    }

    /**
     * setQueryLimit
     * Change the query limit.
     * @param queryLimit number of rows to return when searching
     */
    public void setQueryLimit(int queryLimit) {
        this.queryLimit = queryLimit;
    }

}
