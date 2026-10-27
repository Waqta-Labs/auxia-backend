package com.waqtalabs.auxia.emergencymanagement.domain.services;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Tries to identify the zone of a field report from its text (US-01, scenario 2).
 *
 * <p>A zone is identified when its name appears in the text as whole words, ignoring case and
 * accents. If no zone or more than one zone of the emergency is mentioned, the zone cannot be
 * identified and the report stays pending zone assignment.
 */
public class ZoneIdentificationService {

  private static final Pattern ACCENTS = Pattern.compile("\\p{M}");
  private static final Pattern SPACES = Pattern.compile("\\s+");

  /**
   * Returns the only zone of {@code candidates} mentioned in {@code text}.
   *
   * @param candidates zones of the emergency of the report
   */
  public Optional<Zone> identifyZone(String text, List<Zone> candidates) {
    String normalizedText = normalize(text);
    List<Zone> mentioned =
        candidates.stream().filter(zone -> mentions(normalizedText, zone.name())).toList();
    return mentioned.size() == 1 ? Optional.of(mentioned.getFirst()) : Optional.empty();
  }

  private static boolean mentions(String normalizedText, String zoneName) {
    String name = Pattern.quote(normalize(zoneName));
    return Pattern.compile("(?<![\\p{L}\\p{N}])" + name + "(?![\\p{L}\\p{N}])")
        .matcher(normalizedText)
        .find();
  }

  private static String normalize(String value) {
    String withoutAccents =
        ACCENTS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
    return SPACES.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
  }
}
