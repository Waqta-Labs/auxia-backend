package com.waqtalabs.auxia.emergencymanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AttachToCitizenReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AttachToCitizenReportCommandHandler.Attachment;
import com.waqtalabs.auxia.emergencymanagement.application.commands.AttachToCitizenReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.SosAttachmentStorage;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportAttachmentsQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SosAttachmentQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.CitizenReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.InvalidSosAttachmentException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.SosAttachmentNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.SosAttachment;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AttachmentType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosAttachmentId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosCategory;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.VulnerablePeople;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.SosAttachmentRepository;
import com.waqtalabs.auxia.shared.application.exceptions.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SosAttachmentUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-06T14:41:05Z"), ZoneOffset.ofHours(-5));
  private static final byte[] PHOTO = "foto".getBytes();

  private final InMemoryCitizenReportRepository citizenReports =
      new InMemoryCitizenReportRepository();
  private final InMemoryFieldReportRepository fieldReports = new InMemoryFieldReportRepository();
  private final InMemoryAttachments attachments = new InMemoryAttachments();
  private final Map<String, byte[]> files = new HashMap<>();
  private final SosAttachmentStorage storage =
      new SosAttachmentStorage() {
        @Override
        public String store(String name, String contentType, byte[] content) {
          files.put("blob://" + name, content);
          return "blob://" + name;
        }

        @Override
        public Optional<byte[]> read(String storageUrl) {
          return Optional.ofNullable(files.get(storageUrl));
        }
      };
  private final List<String> acquiredBy = new ArrayList<>();
  private boolean limitReached;

  private final AttachToCitizenReportCommandHandler attach =
      new AttachToCitizenReportCommandHandler(
          citizenReports,
          attachments,
          storage,
          sender -> {
            if (limitReached) {
              throw new RateLimitExceededException(Duration.ofMinutes(1));
            }
            acquiredBy.add(sender);
          },
          CLOCK);
  private final SosAttachmentQueryService queries =
      new SosAttachmentQueryService(fieldReports, citizenReports, attachments, storage);

  private final FieldReportId fieldReport = new FieldReportId(UUID.randomUUID());
  private final CitizenReport sos = citizenReports.save(sos(fieldReport));

  @Test
  void photoIsStoredWithItsNormalizedTypeAndARetryReturnsItWithoutCountingIt() {
    SosAttachmentId id = new SosAttachmentId(UUID.randomUUID());

    Attachment first = attach.handle(command(id, "Image/JPG", PHOTO));
    limitReached = true;
    Attachment retry = attach.handle(command(id, "image/jpeg", PHOTO));

    assertThat(first.created()).isTrue();
    assertThat(first.attachment().type()).isEqualTo(AttachmentType.PHOTO);
    assertThat(first.attachment().contentType()).isEqualTo("image/jpeg");
    assertThat(first.attachment().size()).isEqualTo(PHOTO.length);
    assertThat(first.attachment().receivedAt()).isEqualTo(LocalDateTime.of(2026, 11, 6, 9, 41, 5));
    assertThat(retry).isEqualTo(new Attachment(first.attachment(), false));
    assertThat(acquiredBy).containsExactly("190.12.4.7");
  }

  @Test
  void sosHasOnePhotoAndOneAudio() {
    attach.handle(command(newId(), "image/png", PHOTO));
    attach.handle(command(newId(), "audio/x-m4a", "audio".getBytes()));

    assertThatThrownBy(() -> attach.handle(command(newId(), "image/webp", PHOTO)))
        .isInstanceOf(InvalidSosAttachmentException.class)
        .hasMessageContaining("already has a PHOTO");
    assertThat(attachments.findByCitizenReportId(sos.id()))
        .extracting(SosAttachment::type)
        .containsExactly(AttachmentType.PHOTO, AttachmentType.AUDIO);
  }

  @Test
  void unsupportedEmptyOrTooLargeFilesAreRejected() {
    byte[] sixMegabytes = new byte[6 * 1024 * 1024];

    assertThatThrownBy(() -> attach.handle(command(newId(), "video/mp4", PHOTO)))
        .isInstanceOf(InvalidSosAttachmentException.class);
    assertThatThrownBy(() -> attach.handle(command(newId(), "image/jpeg", new byte[0])))
        .isInstanceOf(InvalidSosAttachmentException.class);
    assertThatThrownBy(() -> attach.handle(command(newId(), "audio/mp4", sixMegabytes)))
        .isInstanceOf(InvalidSosAttachmentException.class)
        .hasMessageContaining("up to 5 MB");
    assertThat(files).isEmpty();
  }

  @Test
  void fileOfAnSosThatHasNotArrivedIsNotFoundSoTheAppRetries() {
    var command =
        new AttachToCitizenReportCommand(
            new CitizenReportId(UUID.randomUUID()), newId(), "image/jpeg", PHOTO, "190.12.4.7");

    assertThatThrownBy(() -> attach.handle(command))
        .isInstanceOf(CitizenReportNotFoundException.class);
  }

  @Test
  void newFileOverTheLimitOfTheSenderIsRejected() {
    limitReached = true;

    assertThatThrownBy(() -> attach.handle(command(newId(), "image/jpeg", PHOTO)))
        .isInstanceOf(RateLimitExceededException.class);
    assertThat(files).isEmpty();
  }

  @Test
  void authorityListsAndReadsTheFilesOfTheSosBehindAFieldReport() {
    SosAttachmentId photo = newId();
    attach.handle(command(photo, "image/jpeg", PHOTO));
    fieldReports.save(
        FieldReport.reconstitute(
            fieldReport,
            new EmergencyId(UUID.randomUUID()),
            null,
            "SOS Médico",
            ReportSource.CITIZEN,
            LocalDateTime.now(CLOCK),
            null,
            null));

    assertThat(queries.attachments(new GetFieldReportAttachmentsQuery(fieldReport, null)))
        .extracting(SosAttachment::id)
        .containsExactly(photo);
    assertThat(queries.file(new GetFieldReportAttachmentsQuery(fieldReport, photo)).content())
        .isEqualTo(PHOTO);
    assertThatThrownBy(() -> queries.file(new GetFieldReportAttachmentsQuery(fieldReport, newId())))
        .isInstanceOf(SosAttachmentNotFoundException.class);
  }

  private AttachToCitizenReportCommand command(
      SosAttachmentId id, String contentType, byte[] content) {
    return new AttachToCitizenReportCommand(sos.id(), id, contentType, content, "190.12.4.7");
  }

  private static SosAttachmentId newId() {
    return new SosAttachmentId(UUID.randomUUID());
  }

  private static CitizenReport sos(FieldReportId fieldReport) {
    return CitizenReport.receive(
        new CitizenReportId(UUID.randomUUID()),
        SosCategory.MEDICAL,
        "SOS Médico",
        LocalDateTime.now(CLOCK),
        LocalDateTime.now(CLOCK),
        null,
        VulnerablePeople.NONE,
        List.of(),
        null,
        fieldReport);
  }

  /** Test double of the repository, the photo first. */
  private static final class InMemoryAttachments implements SosAttachmentRepository {

    private final Map<SosAttachmentId, SosAttachment> saved = new LinkedHashMap<>();

    @Override
    public SosAttachment save(SosAttachment attachment) {
      saved.put(attachment.id(), attachment);
      return attachment;
    }

    @Override
    public Optional<SosAttachment> findById(SosAttachmentId id) {
      return Optional.ofNullable(saved.get(id));
    }

    @Override
    public List<SosAttachment> findByCitizenReportId(CitizenReportId citizenReportId) {
      return saved.values().stream()
          .filter(attachment -> attachment.citizenReportId().equals(citizenReportId))
          .sorted(Comparator.comparing(SosAttachment::type))
          .toList();
    }
  }
}
