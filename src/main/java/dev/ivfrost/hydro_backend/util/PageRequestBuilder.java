package dev.ivfrost.hydro_backend.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageRequestBuilder {

  public static final int DEFAULT_PAGE_NUMBER = 0;
  public static final int DEFAULT_PAGE_SIZE = 25;
  public static final int MAX_PAGE_SIZE = 1000;

  public static Pageable buildPageRequest(Integer pageNumber, Integer pageSize, Sort sort) {
    int queryPageNumber = (pageNumber != null && pageNumber > 0) ? pageNumber - 1 : DEFAULT_PAGE_NUMBER;
    int queryPageSize = (pageSize == null) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

    Sort effectiveSort = (sort != null) ? sort : Sort.unsorted();
    return PageRequest.of(queryPageNumber, queryPageSize, effectiveSort);
  }

  public static Pageable buildPageRequest(Integer pageNumber, Integer pageSize) {
    return buildPageRequest(pageNumber, pageSize, Sort.by(Sort.Order.asc("id")));
  }

  public static Pageable buildPageRequest(Integer pageNumber, Integer pageSize, String sortField, Sort.Direction direction) {
    return buildPageRequest(pageNumber, pageSize, Sort.by(new Sort.Order(direction, sortField)));
  }
}