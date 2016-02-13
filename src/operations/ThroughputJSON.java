package operations;

import db.DBConnection;
import model.ThroughputEntry;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ThroughputJSON {

    List<ThroughputEntry> throughPutList;

    public ThroughputJSON() {

    }

    public String DefaultThroughput(String sDate, String eDate, String env, String app, Connection connection, DBConnection dbWork) {


        String json = "";
        LocalDate newDay;
        if (sDate != null && eDate != null) {
            Timestamp from = TimestampUtils.parseTimestamp(sDate, null);
            Timestamp to = TimestampUtils.parseTimestamp(eDate, null);
            Instant start = from.toInstant();
            Instant end = to.toInstant();
            ZoneId zoneId = ZoneId.of("Europe/Dublin");
            ZonedDateTime zdt = ZonedDateTime.ofInstant(start, zoneId);
            newDay = zdt.toLocalDate();
            System.out.println("newday" + newDay);

            try {
                throughPutList = dbWork.getThroughputEntriesInTimespan(env, app, from, to, connection);
                System.out.println(throughPutList.size());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (throughPutList != null) {
                json += "[";
                json += "[" + (start.getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "],");
            }

            assert throughPutList != null;
            for (ThroughputEntry chartModel : throughPutList) {

                json += "[" + (chartModel.getRetrieved().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + chartModel.getThroughput() + "],");

            }
            if (throughPutList != null) {
                json += "[" + (end.getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "]");
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

    public String TotalThroughput(String sDate, String eDate, String env, String app, Connection connection, DBConnection dbWork) {


        String json = "";
        long max = 0;
        LocalDate newDay;
        ZoneId zoneId = ZoneId.of("Europe/Dublin");
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        if (sDate != null && eDate != null) {
            Timestamp from = TimestampUtils.parseTimestamp(sDate, null);
            Timestamp to = TimestampUtils.parseTimestamp(eDate, null);
            Instant start = from.toInstant();
            Instant end = to.toInstant();

            try {
                throughPutList = dbWork.getThroughputEntriesInTimespan(env, app, from, to, connection);
                System.out.println(throughPutList.size());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (throughPutList != null) {

                json += "[";
                json += "[" + (start.getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "],");
            }

            assert throughPutList != null;
            for (ThroughputEntry chartModel : throughPutList) {

                ZonedDateTime zdt = ZonedDateTime.ofInstant(chartModel.getRetrieved(), zoneId);
                LocalDate localDate = zdt.toLocalDate();
                long entryDay = localDate.atStartOfDay(zoneId).toEpochSecond() * TimestampUtils.MILLIS_PER_SECOND;

                BigDecimal oldTotalForDate = map.get(entryDay);
                BigDecimal newTotalForDate = (null == oldTotalForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) : oldTotalForDate.add(BigDecimal.valueOf(chartModel.getThroughput()));
                if (null == newTotalForDate) {  // If we failed to get new sum.
                    // TODO: Handle error condition. Perhaps: map.remove(key);
                } else {  // Else normal, we have a new total. Store it in map.


                    map.put(entryDay, newTotalForDate);  // Replaces any old value.
                }

            }

            for (Map.Entry<Long, BigDecimal> entry : map.entrySet()) {
                System.out.printf("Key : %s and Value: %s %n", entry.getKey(), entry.getValue());
                json += "[" + (entry.getKey() + "," + entry.getValue() + "],");
            }
            if (throughPutList != null) {
                json += "[" + (end.getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "]");
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

    public String MaxThroughput(String sDate, String eDate, String env, String app, Connection connection, DBConnection dbWork) {


        String json = "";
        long max = 0;
        ZoneId zoneId = ZoneId.of("Europe/Dublin");
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        if (sDate != null && eDate != null) {
            Timestamp from = TimestampUtils.parseTimestamp(sDate, null);
            Timestamp to = TimestampUtils.parseTimestamp(eDate, null);
            Instant start = from.toInstant();
            Instant end = to.toInstant();

            try {
                throughPutList = dbWork.getThroughputEntriesInTimespan(env, app, from, to, connection);
                System.out.println("list size" + throughPutList.size());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (throughPutList != null) {

                json += "[";
                json += "[" + (start.getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "],");
            }

            assert throughPutList != null;
            for (ThroughputEntry chartModel : throughPutList) {

                ZonedDateTime zdt = ZonedDateTime.ofInstant(chartModel.getRetrieved(), zoneId);
                LocalDate localDate = zdt.toLocalDate();
                long entryDay = localDate.atStartOfDay(zoneId).toEpochSecond() * TimestampUtils.MILLIS_PER_SECOND;
                BigDecimal oldMaxForDate = map.get(entryDay);
                BigDecimal newMaxForDate = (null == oldMaxForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) :
                        (oldMaxForDate.compareTo(BigDecimal.valueOf(chartModel.getThroughput())) == 1) ?
                                oldMaxForDate : BigDecimal.valueOf(chartModel.getThroughput());

                if (null == newMaxForDate) {  // If we failed to get new sum.
                    // TODO: Handle error condition. Perhaps: map.remove(key);
                } else {  // Else normal, we have a new total. Store it in map.
                    map.put(entryDay, newMaxForDate);  // Replaces any old value.
                }

            }

            for (Map.Entry<Long, BigDecimal> entry : map.entrySet()) {
                System.out.printf("Key : %s and Value: %s %n", entry.getKey(), entry.getValue());
                json += "[" + (entry.getKey() + "," + entry.getValue() + "],");
            }
            if (throughPutList != null) {
                json += "[" + (end.getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "]");
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

}
