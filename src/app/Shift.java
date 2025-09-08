package app;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;

public class Shift implements Serializable {
    private final DayOfWeek day;
    private final LocalTime start;
    private final LocalTime end;

    public Shift(DayOfWeek day, LocalTime start, LocalTime end) {
        this.day = day;
        this.start = start;
        this.end = end;
    }

    public DayOfWeek getDay() { return day; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }

    public int getHours() {
        return (int) java.time.Duration.between(start, end).toHours();
    }

    @Override
    public String toString() {
        return day + " " + start + "-" + end;
    }
}
