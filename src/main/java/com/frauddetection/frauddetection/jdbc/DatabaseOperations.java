package com.frauddetection.frauddetection.jdbc;

import java.util.List;
import java.util.Optional;

/**
 * Generic interface defining standard database operations via JDBC.
 *
 * Demonstrates:
 * - OOP Implementation: Interfaces & Abstraction
 * - Collections & Generics: Generic type parameter {@code <T>} and {@code List<T>}
 * - Classes for Database Operations: Contract for JDBC DAO implementations
 *
 * @param <T> The entity/record type managed by this DAO
 */
public interface DatabaseOperations<T> {

    /**
     * Retrieve an entity by its unique identifier.
     *
     * @param id The primary key identifier
     * @return An Optional containing the entity if found, empty otherwise
     * @throws DatabaseOperationException If a database access error occurs
     */
    Optional<T> findById(Long id) throws DatabaseOperationException;

    /**
     * Retrieve all entities from the underlying database table.
     *
     * @return A list containing all records
     * @throws DatabaseOperationException If a database access error occurs
     */
    List<T> findAll() throws DatabaseOperationException;

    /**
     * Persist or update an entity using JDBC prepared statements.
     *
     * @param entity The entity record to persist
     * @return The persisted entity with generated keys populated
     * @throws DatabaseOperationException If a database access error occurs
     */
    T save(T entity) throws DatabaseOperationException;

    /**
     * Delete an entity by its identifier.
     *
     * @param id The primary key identifier
     * @return True if a record was deleted, false otherwise
     * @throws DatabaseOperationException If a database access error occurs
     */
    boolean delete(Long id) throws DatabaseOperationException;

    /**
     * Count the total number of records in the database table.
     *
     * @return Total row count
     * @throws DatabaseOperationException If a database access error occurs
     */
    long count() throws DatabaseOperationException;
}
