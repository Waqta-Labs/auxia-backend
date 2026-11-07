package com.waqtalabs.auxia.emergencymanagement.domain.repositories;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import java.util.Optional;

public interface CitizenReportRepository {

  /** Stores a new SOS. An SOS is never changed once stored. */
  CitizenReport save(CitizenReport citizenReport);

  Optional<CitizenReport> findById(CitizenReportId id);

  /** The SOS that became the field report, if it came from a citizen. */
  Optional<CitizenReport> findByFieldReportId(FieldReportId fieldReportId);
}
