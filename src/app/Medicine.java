package app;

import java.io.Serializable;

public class Medicine implements Serializable {
    private final String name;

    public Medicine(String name) { this.name = name; }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }
}
