package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AttachmentTypeTests {

  @ParameterizedTest
  @CsvSource({
    "image/jpeg, PHOTO",
    "Image/JPG, PHOTO",
    "image/heic, PHOTO",
    "audio/mp4, AUDIO",
    "audio/x-m4a, AUDIO",
    "audio/aac; codecs=mp4a, AUDIO",
    "audio/mp3, AUDIO"
  })
  void mediaTypesOfPhonesAreRecognized(String contentType, AttachmentType type) {
    assertThat(AttachmentType.of(contentType)).contains(type);
  }

  @Test
  void otherMediaTypesAreNotAdmitted() {
    assertThat(AttachmentType.of("video/mp4")).isEmpty();
    assertThat(AttachmentType.of(null)).isEmpty();
  }

  @Test
  void photoAdmitsTenMegabytesAndAudioFive() {
    assertThat(AttachmentType.PHOTO.maxBytes()).isEqualTo(10L * 1024 * 1024);
    assertThat(AttachmentType.AUDIO.maxBytes()).isEqualTo(5L * 1024 * 1024);
  }
}
