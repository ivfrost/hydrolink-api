package dev.ivfrost.hydro_backend.storage;

public class FileUploadException extends RuntimeException {

  public FileUploadException(String message, Exception e) {
    super(message, e);
  }
}
