package project.group.remotegreenhouse.GreenHouse;
public class GreenHouse {
    private static final String TAG = "GreenHouse";
    private String name;

    private double stateValues[];   // state of the GreenHouse =  temperature, humidity, pressure, moisture, brightness
    private double controlValues[]; // values from the user = LED-Brightness, LED-On-Time, LED-Off-Time
    private Plant plants[];

    public GreenHouse() {
        setName("NEW GREENHOUSE");
        this.stateValues   = new double[5];
        this.controlValues = new double[5];

        this.plants = new Plant[6];
        for (int i=0; i<this.plants.length; i++) {
            this.plants[i] = new Plant("NEW PLANT " + i);
        }
    }
    public GreenHouse(String name) {
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
    public void setStateValues(double[] values) {
        if (values.length == stateValues.length) {
            this.stateValues = values;
        } else {
            System.err.println(TAG + ".setStateValues(): unequal lengths");
        }
    }
    public void setControlValues(double[] values) {
        if (values.length == this.controlValues.length) {
            this.controlValues = values;
        } else {
            System.err.println(TAG + ".setControlValues(): unequal lengths");
        }
    }

    public void setStateValue(int position, double value) {
        if (position >= 0 && position <= this.stateValues.length) {
            this.stateValues[position] = value;
            System.out.println(TAG + ".setStateValue(): Value(" + position + ") set to '" + value + "'.");
        } else {
            System.err.println(TAG + ".setStateValue(): IndexOutOfBound!");
        }
    }
    public void setControlValue(int position, double value) {
        if (position >= 0 && position <= this.controlValues.length) {
            this.stateValues[position] = value;
            System.out.println(TAG + ".setControlValue(): Value(" + position + ") set to '" + value + "'.");
        } else {
            System.err.println(TAG + ".setControlValue(): IndexOutOfBound!");
        }
    }

    public void setPlant(int position, Plant plant) {
        if (position >= 0 && position <= 6) {
            this.plants[position] = plant;
            System.err.println(TAG + ".setPlant(): Plant(" + position + ") set to: " + plant.toString());
        } else {
            System.err.println(TAG + ".setPlant(): IndexOutOfBound!");
        }
    }
    public void setPlants(Plant[] plants) {
        if (plants.length == this.plants.length) {
            this.plants = plants;
        } else {
            System.err.println(TAG + ".setPlants(): unequal lengths");
        }
    }

    // GETTER
    public String   getName() {
        return this.name;
    }
    public double[] getStateValues() {
        return this.stateValues;
    }
    public double[] getControlValues() {
        return this.controlValues;
    }
    public Plant[]  getPlants() {
        return this.plants;
    }
    public Plant    getPlant(int position) {
        return this.plants[position];
    }

    public String toString() {
        return TAG + " '" + getName() + "'";
    }
}