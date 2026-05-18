package com.minilands.backend.repository;

import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.enums.PropertyStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PropertyRepository extends MongoRepository<Property, String> {

    List<Property> findByStatus(PropertyStatus status);
}
