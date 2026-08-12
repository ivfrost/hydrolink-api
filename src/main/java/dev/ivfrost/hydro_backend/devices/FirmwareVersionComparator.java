package dev.ivfrost.hydro_backend.devices;

import org.semver4j.Semver;

/**
 * Compares firmware version strings without assuming they are strict semver.
 *
 * <p>Firmware is a free-form string (max 20 chars), e.g. {@code "1.0.0"},
 * {@code "v2.1.0"} or {@code "2026.3.1-rc2"}. Parsed as coerced semver via
 * Semver4j; strings that cannot be coerced at all fall back to lexicographic
 * comparison so the OTA listener never throws.
 */
public final class FirmwareVersionComparator {

  private FirmwareVersionComparator() {
    // utility class
  }

  public static int compare(String left, String right) {
    if (left == null || right == null) {
      return Boolean.compare(left != null, right != null);
    }

    Semver leftVersion = Semver.coerce(left.trim());
    Semver rightVersion = Semver.coerce(right.trim());

    if (leftVersion == null || rightVersion == null) {
      return left.compareTo(right);
    }

    return leftVersion.compareTo(rightVersion);
  }
}