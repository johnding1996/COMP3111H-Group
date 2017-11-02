/**
 * This package serves as the connection pool for SQL and Redis.
 * In order to keep thread-safe, other classes will utilize the connection
 * pool initialized as static variables inside the wrapper class here.
 * @author mcding
 * @version 1.2.1
 */
package database.connection;