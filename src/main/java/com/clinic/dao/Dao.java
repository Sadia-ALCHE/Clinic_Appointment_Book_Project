package com.clinic.dao;

import java.util.List;
import java.util.Optional;


// This is the generic Data Access Object root interface defining standard CRUD operations.
// This file is basically declaring methods that every domain entity (Patient, Doctor) inherits
// @param <T> represents the Domain entity type
// @param <K> represents the Primary key identifier type

public interface Dao<T, K> {
    Optional<T> findById(K id);
    List<T> findAll();
    T save(T entity);
    boolean update(T entity);
    boolean deleteById(K id);
}