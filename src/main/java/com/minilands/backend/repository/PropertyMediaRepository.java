package com.minilands.backend.repository;

import com.minilands.backend.entity.PropertyMedia;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyMediaRepository extends MongoRepository<PropertyMedia, String> {

    List<PropertyMedia> findByPropertyIdOrderByDisplayOrderAsc(String propertyId);

    Optional<PropertyMedia> findByPropertyIdAndPrimaryTrue(String propertyId);
}
