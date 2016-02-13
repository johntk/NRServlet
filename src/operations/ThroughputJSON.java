package operations;

import db.DBConnection;
import model.ThroughputEntry;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ThroughputJSON {

    List<ThroughputEntry> throughPutList;
    private String sDate;
    private String eDate;
    private Timestamp from;
    private Timestamp to;
    private Instant start;
    private Instant end;
    private String env;
    private String app;
    private Connection connection;
    private DBConnection dbWork;
    private String json;
    private ZoneId zoneId = ZoneId.of("Europe/Dublin");
    private Map<Long, BigDecimal> map = new LinkedHashMap<>();

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
        this.start = from.toInstant();
        this.end = to.toInstant();
    }

    public String Generate(String type) {

        if (sDate != null && eDate != null) {

            try {
                this.throughPutList = this.dbWork.getThroughputEntriesInTimespan(this.env, this.app, this.from, this.to, this.connection);
                System.out.println(this.throughPutList.size());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (throughPutList != null) {

                json += "[" + (start.getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "],");
            }

            assert this.throughPutList != null;
            for (ThroughputEntry chartModel : this.throughPutList) {

                if (type == "defaultChart") {

                    Default(chartModel);

                } else if (type == "totalChart") {

                    Total(chartModel);

                } else if (type == "maxChart") {

                    Max(chartModel);

                } else if (type == "minChart") {


                } else if (type == "meanChart") {


                } else {

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


    public void Default(ThroughputEntry chartModel) {
        
        map.put(chartModel.getRetrieved().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND, BigDecimal.valueOf(chartModel.getThroughput()));
    }

    public void Total(ThroughputEntry chartModel) {

        ZonedDateTime zdt = ZonedDateTime.ofInstant(chartModel.getRetrieved(), zoneId);
        LocalDate localDate = zdt.toLocalDate();
        long entryDay = localDate.atStartOfDay(zoneId).toEpochSecond() * TimestampUtils.MILLIS_PER_SECOND;
        BigDecimal oldTotalForDate = map.get(entryDay);
        BigDecimal newTotalForDate = (null == oldTotalForDate) ? BigDecimal.valueOf(chartModel.getThroughput()) :
                oldTotalForDate.add(BigDecimal.valueOf(chartModel.getThroughput()));

        if (null == newTotalForDate) {  // If we failed to get new sum.
            // TODO: Handle error condition. Perhaps: map.remove(key);
        } else {  // Else normal, we have a new total. Store it in map.

            map.put(entryDay, newTotalForDate);  // Replaces any old value.
        }

    }

    public void Max(ThroughputEntry chartModel) {

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

    public String Min() {
        return "";
    }

    public String Mean() {
        return "";
    }

}
