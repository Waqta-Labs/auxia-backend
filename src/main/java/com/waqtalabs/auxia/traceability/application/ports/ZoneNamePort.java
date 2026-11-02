package com.waqtalabs.auxia.traceability.application.ports;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.util.Collection;
import java.util.Map;

/** Asks Emergency Management the names of the zones of the deliveries (US-19). */
public interface ZoneNamePort {

  /** Name of each zone that exists; an unknown one is left out. */
  Map<ZoneId, String> zoneNames(Collection<ZoneId> zoneIds);
}
