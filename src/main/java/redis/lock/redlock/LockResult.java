package redis.lock.redlock;

public class LockResult {

    private final String resource;
    private final String value;
    private final double validity;

    public LockResult(String resource, String value, double validityTime) {
        this.resource = resource;
        this.value = value;
        this.validity = validityTime;
    }

    public String getResource() {
        return resource;
    }

    public String getValue() {
        return value;
    }

    public double getValidity() {
        return validity;
    }

}
