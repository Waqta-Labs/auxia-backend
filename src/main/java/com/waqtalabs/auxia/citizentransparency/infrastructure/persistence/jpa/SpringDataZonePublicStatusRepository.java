package com.waqtalabs.auxia.citizentransparency.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataZonePublicStatusRepository
    extends JpaRepository<ZonePublicStatusJpaEntity, UUID> {

  List<ZonePublicStatusJpaEntity> findByEmergencyIdOrderByZoneNameAsc(UUID emergencyId);

  @Query(
      """
      select zone from ZonePublicStatusJpaEntity zone, EmergencyPublicViewJpaEntity emergency
      where zone.emergencyId = emergency.emergencyId
        and lower(emergency.district) = lower(:district)
      order by zone.zoneName
      """)
  List<ZonePublicStatusJpaEntity> searchByDistrict(@Param("district") String district);

  @Query(
      """
      select zone from ZonePublicStatusJpaEntity zone, EmergencyPublicViewJpaEntity emergency
      where zone.emergencyId = emergency.emergencyId
        and lower(emergency.district) = lower(:district)
        and lower(zone.zoneName) like lower(concat('%', :name, '%'))
      order by zone.zoneName
      """)
  List<ZonePublicStatusJpaEntity> searchByDistrictAndName(
      @Param("district") String district, @Param("name") String name);
}
