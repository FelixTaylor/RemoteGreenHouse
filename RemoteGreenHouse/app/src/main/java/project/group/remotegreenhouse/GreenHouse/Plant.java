package project.group.remotegreenhouse.GreenHouse;
public class Plant {
    private static final String TAG = "Plant";

    private String name;
    private double moisture;

    public Plant() {
      setName("NEW PLANT");
    }
    public Plant(String name) {
        this();
        setName(name);
    }

    // SETTER
    public void setName(String name) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
            System.out.println(TAG + ".setName(): Name set to '" + name + "'.");
        } else {
            System.err.println(TAG + ".setName(): param is null/empty.");
        }
    }
    public void setMoisture(double moisture) {
        this.moisture = moisture;
    }

    // GETTER
    public String getName() {
        return this.name;
    }
    public double getMoisture() {
        return this.moisture;
    }

    @Override public String toString() {
        return "Plant{" +  "name='" + name + '\'' + ", moisture=" + moisture + '}';
    }
}