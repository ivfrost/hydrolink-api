package dev.ivfrost.hydro_backend.storage;

public class EmptyFileException extends RuntimeException {

  public EmptyFileException(String message) {
    super(message);
  }
}
