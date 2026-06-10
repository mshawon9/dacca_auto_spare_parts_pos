package com.daccaauto.pos.repository;

import com.daccaauto.pos.entity.VehicleApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleApplicationRepository extends JpaRepository<VehicleApplicationEntity, Long> {

    List<VehicleApplicationEntity> findAllByOrderByDisplayNameAsc();

    List<VehicleApplicationEntity> findByVehicleMakeIdOrderByDisplayNameAsc(Long makeId);

    List<VehicleApplicationEntity> findByVehicleMakeIdAndVehicleModelIdOrderByDisplayNameAsc(Long makeId, Long modelId);

    @Query("""
        select (count(v) > 0)
        from VehicleApplicationEntity v
        where v.vehicleMake.id = :makeId
          and v.vehicleModel.id = :modelId
          and lower(coalesce(v.variantLabel, '')) = lower(coalesce(:variantLabel, ''))
          and ((:yearFrom is null and v.yearFrom is null) or v.yearFrom = :yearFrom)
          and ((:yearTo is null and v.yearTo is null) or v.yearTo = :yearTo)
        """)
    boolean existsDuplicate(
            @Param("makeId") Long makeId,
            @Param("modelId") Long modelId,
            @Param("variantLabel") String variantLabel,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo
    );

    @Query("""
        select (count(v) > 0)
        from VehicleApplicationEntity v
        where v.vehicleMake.id = :makeId
          and v.vehicleModel.id = :modelId
          and lower(coalesce(v.variantLabel, '')) = lower(coalesce(:variantLabel, ''))
          and ((:yearFrom is null and v.yearFrom is null) or v.yearFrom = :yearFrom)
          and ((:yearTo is null and v.yearTo is null) or v.yearTo = :yearTo)
          and v.id <> :id
        """)
    boolean existsDuplicateExcludingId(
            @Param("makeId") Long makeId,
            @Param("modelId") Long modelId,
            @Param("variantLabel") String variantLabel,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo,
            @Param("id") Long id
    );
}