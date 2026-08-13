package dev.ivfrost.hydro_backend.devices;

public record PinResponse(int pinNumber, PinMode mode, String label) {}