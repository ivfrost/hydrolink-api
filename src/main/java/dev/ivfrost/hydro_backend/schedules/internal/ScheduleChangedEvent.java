package dev.ivfrost.hydro_backend.schedules.internal;


public record ScheduleChangedEvent(String deviceKey, long version) {

}
