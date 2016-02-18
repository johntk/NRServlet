package operations;

import db.DBConnection;
import model.ThroughputEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private String start;
    private String end;
    private String env;
    private String app;
    private Connection connection;
    private DBConnection dbWork;
    private String json;
    private ZoneId zoneId;
    private Map<Long, BigDecimal> map;
    private long entryDay;
    private long newDayCheck;
    private long dayCount;
    private BigDecimal valueForDate;
    private BigDecimal newTotalForDate;

    public ThroughputJSON(String sDate, String eDate, String env, String app, Connection connection, DBConnection dbWork) {

        this.sDate = sDate;
        this.eDate = eDate;
        this.from = TimestampUtils.parseTimestamp(sDate, null);
        this.to = TimestampUtils.parseTimestamp(eDate, null);
        this.env = env;
        this.app = app;
        this.connection = connection;
        this.dbWork = dbWork;
        this.json = "[{";
        this.zoneId = ZoneId.of("Europe/Dublin");
        this.map = new LinkedHashMap<>();
        this.dayCount = 0;
        this.start = "\"name\": \"total\", \"data\":[[" + (from.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "],");
        this.end = "[" + (to.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "]]}");

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
            // TODO: Add an outer for loop for each calculation
            // Loop through the throughput pulled form the DB and do the desired calculation on it
            assert throughPutList != null;
            int count = 1;
            for (ThroughputEntry chartModel : throughPutList) {

                // Convert the epochSecond value of the period pulled for the DB to it's day for insertion into a Map
                ZonedDateTime zdt = ZonedDateTime.ofInstant(chartModel.getRetrieved(), zoneId);
                LocalDate localDate = zdt.toLocalDate();
                entryDay = localDate.atStartOfDay(zoneId).toEpochSecond() * TimestampUtils.MILLIS_PER_SECOND;
                valueForDate = map.get(entryDay);

                if (count == 1) {
                    json += "\"name\": \"total\", \"data\":[[" + (from.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "],");
                }
                Total(chartModel);
                if (count == throughPutList.size()) {

                    for (Map.Entry<Long, BigDecimal> entry : map.entrySet()) {

                        json += "[" + (entry.getKey() + "," + entry.getValue() + "],");
                    }
                    json += end;
                    map.clear();
                    count =0;
                }
                count++;
            }

            // Loop through the throughput pulled form the DB and do the desired calculation on it
            assert throughPutList != null;
            for (ThroughputEntry chartModel : throughPutList) {

                // Convert the epochSecond value of the period pulled for the DB to it's day for insertion into a Map
                ZonedDateTime zdt = ZonedDateTime.ofInstant(chartModel.getRetrieved(), zoneId);
                LocalDate localDate = zdt.toLocalDate();
                entryDay = localDate.atStartOfDay(zoneId).toEpochSecond() * TimestampUtils.MILLIS_PER_SECOND;
                valueForDate = map.get(entryDay);

                if (count == 1) {
                    json += ",{" + "\"name\": \"default\", \"data\":[[" + (from.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "],");
                }
                Default(chartModel);
                if (count == throughPutList.size()) {

                    for (Map.Entry<Long, BigDecimal> entry : map.entrySet()) {

                        json += "[" + (entry.getKey() + "," + entry.getValue() + "],");
                    }
                    json += end+ "]";
                    map.clear();
                    count =0;
                }
                count++;
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

        newTotalForDate = (null == valueForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) :
                valueForDate.add(BigDecimal.valueOf(chartModel.getThroughput()));

        if (null == newTotalForDate) {  // If we failed to get new value.
            // TODO: Handle error condition.
        } else {  // Else normal, we have a new total. Store it in map.

            map.put(entryDay, newTotalForDate);  // Replaces any old value.s
        }
    }

    public void Max(ThroughputEntry chartModel) {

        // Check if the valueForDate has been set yet, if no, set it with the value pulled from the db,
        // if yes compare the value with the new value pulled for the DB to see which one is bigger, set the newMaxForDate
        BigDecimal newMaxForDate = (null == valueForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) :
                (valueForDate.compareTo(BigDecimal.valueOf(chartModel.getThroughput())) == 1) ?
                        valueForDate : BigDecimal.valueOf(chartModel.getThroughput());

        if (null == newMaxForDate) {  // If we failed to get new value.
            // TODO: Handle error condition.
        } else {  // Else normal, we have a new max. Store it in map.

            map.put(entryDay, newMaxForDate);  // Replaces any old value.
        }
    }

    public void Min(ThroughputEntry chartModel) {

        // Check if the valueForDate has been set yet, if no, set it with the value pulled from the db,
        // if yes compare the value with the new value pulled for the DB to see which one is smaller, set the newMinForDate
        BigDecimal newMinForDate = (null == valueForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) :
                (valueForDate.compareTo(BigDecimal.valueOf(chartModel.getThroughput())) == -1) ?
                        valueForDate : BigDecimal.valueOf(chartModel.getThroughput());

        if (null == newMinForDate) {  // If we failed to get new value.
            // TODO: Handle error condition.
        } else {  // Else normal, we have a new max. Store it in map.

            map.put(entryDay, newMinForDate);  // Replaces any old value.
        }
    }

    public void Mean(ThroughputEntry chartModel) {

        // Check if the newDayCheck var has been set, if not set it to the current entryDay
        if (newDayCheck == 0) {
            newDayCheck = entryDay;
        }
        // Check if the current entryDay and newDayCheck var's match,
        // if no, we have a new entryDay and need to calc the average of the old entryDay and insert into the map
        // Set the dayCount = 0 and newTotalForDate = 0
        if (entryDay != newDayCheck) {
            newTotalForDate = newTotalForDate.divide(BigDecimal.valueOf(dayCount), 3, RoundingMode.CEILING);
            map.put(newDayCheck, newTotalForDate);
            System.out.println("Day " + newDayCheck + "day Count" + dayCount + "Value: " + newTotalForDate);
            dayCount = 0;
            newTotalForDate = new BigDecimal(0);
        }

        newTotalForDate = (null == valueForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) :
                valueForDate.add(BigDecimal.valueOf(chartModel.getThroughput()));

        if (null == newTotalForDate) {  // If we failed to get new value.
            // TODO: Handle error condition.
        } else {  // Else normal, we have a new total. Store it in map.

            dayCount++;
            newDayCheck = entryDay;
            map.put(entryDay, newTotalForDate);  // Replaces any old value.s
        }
    }
}