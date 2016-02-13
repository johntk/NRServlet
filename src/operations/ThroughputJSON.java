package operations;

import db.DBConnection;
import model.ThroughputEntry;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

public class ThroughputJSON {

    List<ThroughputEntry> throughPutList;
    private String sDate;
    private String eDate;
    private Timestamp from;
    private Timestamp to;
    private String env;
    private String app;
    private Connection connection;
    private DBConnection dbWork;
    private String json;
    private ZoneId zoneId = ZoneId.of("Europe/Dublin");
    private Map<Long, BigDecimal> map = new LinkedHashMap<>();
    private long entryDay;
    private BigDecimal valueForDate;

    public ThroughputJSON(String sDate, String eDate, String env, String app, Connection connection, DBConnection dbWork) {

        this.sDate = sDate;
        this.eDate = eDate;
        this.env = env;
        this.app = app;
        this.connection = connection;
        this.dbWork = dbWork;
        this.json = "[";
        this.from = TimestampUtils.parseTimestamp(sDate, null);
        this.to = TimestampUtils.parseTimestamp(eDate, null);

    }

    public String Generate(String type) {

        // Check that we have dates to work with
        if (sDate != null && eDate != null) {

            // Pull data from DB
            try {
                throughPutList = dbWork.getThroughputEntriesInTimespan(env, app, from, to, connection);
                System.out.println(throughPutList.size());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Check that are list is not null and add the starting date for our chart to the JSON string
            if (throughPutList != null) {

                json += "[" + (from.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "],");
            }

            // Loop through the throughput pulled form the DB and do the desired calculation on it
            assert throughPutList != null;
            for (ThroughputEntry chartModel : throughPutList) {

                ZonedDateTime zdt = ZonedDateTime.ofInstant(chartModel.getRetrieved(), zoneId);
                LocalDate localDate = zdt.toLocalDate();
                entryDay = localDate.atStartOfDay(zoneId).toEpochSecond() * TimestampUtils.MILLIS_PER_SECOND;
                valueForDate = map.get(entryDay);

                if (Objects.equals(type, "totalChart")) {

                    Total(chartModel);

                } else if (Objects.equals(type, "maxChart")) {

                    Max(chartModel);

                } else if (Objects.equals(type, "minChart")) {

                    Min(chartModel);

                } else if (Objects.equals(type, "meanChart")) {

                    Mean(chartModel);

                } else {

                    Default(chartModel);
                }
            }

            for (Map.Entry<Long, BigDecimal> entry : map.entrySet()) {

                json += "[" + (entry.getKey() + "," + entry.getValue() + "],");
            }

            if (throughPutList != null) {
                json += "[" + (to.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "]");
                json += "]";
            }
            if (throughPutList == null) {
                json = "No record found";
            }
        } else {
            json = "Date must be selected." + "App : " + app + " " + eDate;

        }
        return json;
    }


    public void Default(ThroughputEntry chartModel) {

        map.put(chartModel.getRetrieved().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND, BigDecimal.valueOf(chartModel.getThroughput()));
    }

    public void Total(ThroughputEntry chartModel) {

        BigDecimal newTotalForDate = (null == valueForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) :
                valueForDate.add(BigDecimal.valueOf(chartModel.getThroughput()));

        if (null == newTotalForDate) {  // If we failed to get new value.
            // TODO: Handle error condition.
        } else {  // Else normal, we have a new total. Store it in map.

            map.put(entryDay, newTotalForDate);  // Replaces any old value.
        }
    }

    public void Max(ThroughputEntry chartModel) {

        BigDecimal newMaxForDate = (null == this.valueForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) :
                (valueForDate.compareTo(BigDecimal.valueOf(chartModel.getThroughput())) == 1) ?
                        valueForDate : BigDecimal.valueOf(chartModel.getThroughput());

        if (null == newMaxForDate) {  // If we failed to get new value.
            // TODO: Handle error condition.
        } else {  // Else normal, we have a new max. Store it in map.

            map.put(entryDay, newMaxForDate);  // Replaces any old value.
        }
    }

    public void Min(ThroughputEntry chartModel) {

        BigDecimal newMaxForDate = (null == this.valueForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) :
                (valueForDate.compareTo(BigDecimal.valueOf(chartModel.getThroughput())) == -1) ?
                        valueForDate : BigDecimal.valueOf(chartModel.getThroughput());

        if (null == newMaxForDate) {  // If we failed to get new value.
            // TODO: Handle error condition.
        } else {  // Else normal, we have a new max. Store it in map.

            map.put(entryDay, newMaxForDate);  // Replaces any old value.
        }
    }

    public void Mean(ThroughputEntry chartModel) {


    }
}
