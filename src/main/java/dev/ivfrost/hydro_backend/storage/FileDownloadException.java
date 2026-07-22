package dev.ivfrost.hydro_backend.storage;

public class FileDownloadException extends RuntimeException {

  public FileDownloadException(String message, Exception e) {
    super(message, e);
  }
}
