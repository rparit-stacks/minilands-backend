package com.minilands.backend.repository;

import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.enums.ListingVisibility;
import com.minilands.backend.entity.enums.PropertyStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends MongoRepository<Property, String> {

    List<Property> findByStatus(PropertyStatus status);

    List<Property> findByStatusInAndListingVisibilityOrderByFeaturedDescDisplayOrderAscCreatedAtDesc(
            Collection<PropertyStatus> statuses,
            ListingVisibility listingVisibility);

    List<Property> findByStatusInOrderByFeaturedDescDisplayOrderAscCreatedAtDesc(
            Collection<PropertyStatus> statuses);

    Optional<Property> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, String id);
}
