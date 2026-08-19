package dev.ivfrost.hydro_backend.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(value = {"pageable", "sort"}, ignoreUnknown = true)
public class RestResponsePage<T> extends PageImpl<T> {

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public RestResponsePage(@JsonProperty("content") List<T> content,
      @JsonProperty("number") int number,
      @JsonProperty("size") int size,
      @JsonProperty("totalElements") long totalElements) {
    super(content, PageRequest.of(number, size == 0 ? 1 : size), totalElements);
  }

  public RestResponsePage(List<T> content, Pageable pageable, long total) {
    super(content, pageable, total);
  }

  public RestResponsePage(List<T> content) {
    super(content);
  }

  public RestResponsePage() {
    super(new ArrayList<>());
  }
}