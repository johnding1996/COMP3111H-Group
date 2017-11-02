/**
 * Package database is one of the basic module of the dieting-chatbot project.
 * It servers us the wrapper and I/O interface of SQL database and Redis cache.
 * It is designed to be thread-safe for multiple concurrent connections.
 * Useful classes include: querier.PartialFoodQuerier, querier.PartialFoodQuerier, querier.UserQuerier
 * keeper.StateKeeper, keeper.HistKeeper, keeper.LogKeeper.
 * @author mcding
 * @version 1.2
 */
package database;