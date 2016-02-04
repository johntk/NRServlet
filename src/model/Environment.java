package model;

import java.util.HashMap;
import java.util.Map;

/** This class models the environments based on the variables pulled from the .properties,
 * each Environment can have multiple Applications, Servers or Plugins identified by a unique id */
public class Environment {

    private final String name;
    private String apiKey;
    private String metricNames;
    private String URL;

    private final Map<String, Application> applications;

    public Environment(String envName) {
        this.name = envName;
        this.apiKey = null;
        this.applications = new HashMap<>();
        this.metricNames = null;
        this.URL = null;
    }

    public String getName() {return name;}

    public String getApiKey() {return apiKey;}

    public void setApiKey(String envKey) {this.apiKey = envKey;}

    public String getMetricNames() {return metricNames;}

    public void setMetricNames(String metricNames) {this.metricNames = metricNames;}

    public String getURL() {return URL;}

    public void setURL(String URL) {this.URL = URL;}

    public Map<String, Application> getApplications() {return applications;}

    public void addApplication(Application app) {
        String appName = app.getName();
        applications.put(appName, app);
    }
}
