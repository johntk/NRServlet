package operations;

import db.DBConnection;
import model.ThroughputEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.NevilleInterpolator;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;


public class ThroughputJSON {

    List<ThroughputEntry> throughPutList;
    private String sDate;
    private String eDate;
    private Timestamp from;
    private Timestamp to;
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
    private int count;
    private int throughCount;
    private BigDecimal valueForDate;
    private BigDecimal newTotalForDate;
    private List<Double> time;
    private List<Double> throughput;
    private double secCount;
    private List<String> calcList;
    private double[] x1;
    private double[] y1;
    private double[] x2;
    private int grade;


    public ThroughputJSON(String sDate, String eDate, String env, String app, Connection connection, DBConnection dbWork, ArrayList calcList, int grade) {

        this.sDate = sDate;
        this.eDate = eDate;
        this.from = TimestampUtils.parseTimestamp(sDate, null);
        this.to = TimestampUtils.parseTimestamp(eDate, null);
        this.env = env;
        this.app = app;
        this.connection = connection;
        this.dbWork = dbWork;
        this.json = "";
        this.zoneId = ZoneId.of("Europe/Dublin");
        this.map = new LinkedHashMap<>();
        this.dayCount = 0;
        this.count = 0;
        this.throughCount = 0;
        this.end = "[" + (to.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "]]}");
        this.time = new ArrayList<>();
        this.throughput = new ArrayList<>();
        this.secCount = 0;
        this.calcList = calcList;
        this.grade = grade;
        System.out.println("Date : " + from + " " + to);
    }

    public ThroughputJSON(){

        this.zoneId = ZoneId.of("Europe/Dublin");
        this.map = new LinkedHashMap<>();
        this.count = 0;
    }

    public void Generate(ThroughputEntry chartModel){

        // Convert the epochSecond value of the period pulled for the DB to it's day for insertion into a Map
        ZonedDateTime zdt = ZonedDateTime.ofInstant(chartModel.getRetrieved(), zoneId);
        LocalDate localDate = zdt.toLocalDate();
        entryDay = localDate.atStartOfDay(zoneId).toEpochSecond() * TimestampUtils.MILLIS_PER_SECOND;
        valueForDate = map.get(entryDay);
        count++;
        if(count == 6) entryDay = 0;
    }

    public String Generate(String type) {

        // Check that we have dates to work with
        if (sDate != null && eDate != null) {

            // Pull data from DB
            try {
                throughPutList = dbWork.getThroughputEntriesInTimespan(env, app, from, to, connection);

            } catch (Exception e) {
                e.printStackTrace();
            }

            assert throughPutList != null;
            int count = 1;
            int meanCount = 0;
            for (String calc : calcList) {
                for (ThroughputEntry chartModel : throughPutList) {

                    // Convert the epochSecond value of the period pulled for the DB to it's day for insertion into a Map
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(chartModel.getRetrieved(), zoneId);
                    LocalDate localDate = zdt.toLocalDate();
                    entryDay = localDate.atStartOfDay(zoneId).toEpochSecond() * TimestampUtils.MILLIS_PER_SECOND;
                    valueForDate = map.get(entryDay);

                    if (count == 1) {
                        json += "{\"name\": \"" + env + "-" + app + "-" + calc + "\", \"data\":[[" + (from.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "],");
                    }
                    if (calc.equals("Throughput")) {
                        Default(chartModel);
                    } else if (calc.equals("Total")) {
                        Total(chartModel);
                    } else if (calc.equals("Min")) {
                        Min(chartModel);
                    } else if (calc.equals("Max")) {
                        Max(chartModel);
                    } else if (calc.equals("Mean")) {
                        meanCount++;
                        if (meanCount == throughPutList.size()) {
                            entryDay = 0;
                        }
                        Mean(chartModel);

                    } else if (calc.equals("Linear-Extrap")) {
                        Extrapolate(chartModel, 1);
                    } else if (calc.equals("P-Regression-Extrap")) {
                        Extrapolate(chartModel, 0);
                    }
                    if (count == throughPutList.size()) {

                        for (Map.Entry<Long, BigDecimal> entry : map.entrySet()) {

                            json += "[" + (entry.getKey() + "," + entry.getValue() + "],");
                        }
//                        System.out.println(calc.indexOf(calc) + " " + (calcList.size() -1) );
                        if (calcList.indexOf(calc) != (calcList.size() - 1)) {

                            if (newDayCheck > to.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND) {
                                json += "]},";
                            } else {
                                json += end + ",";
                            }

                        } else {
//                            System.out.println("Newdaycheck : " + newDayCheck + " To " + to.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND);
                            if (newDayCheck > to.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND) {
                                json += "]}";
                            } else {
                                json += end;
                            }
                        }

                        map.clear();
                        count = 0;
                    }
                    count++;
                }
            }
//            json += "]";

            if (throughPutList == null) {
                json = "No record found";
            }
        } else {
            json = "Date must be selected." + "App : " + app + " " + eDate;
        }
        return json;
    }

    public Map<Long, BigDecimal>  GetMap() {

        return map;
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
//        System.out.println(chartModel.getThroughput());
        if (newDayCheck == 0) {
            newDayCheck = entryDay;
        }
        // Check if the current entryDay and newDayCheck var's match,
        // if no, we have a new entryDay and need to calc the average of the old entryDay and insert into the map
        // Set the dayCount = 0 and newTotalForDate = 0
        if (entryDay != newDayCheck) {
//            System.out.println(newTotalForDate);
            newTotalForDate = newTotalForDate.divide(BigDecimal.valueOf(dayCount), 3, RoundingMode.CEILING);
            map.put(newDayCheck, newTotalForDate);
//            System.out.println("Day " + newDayCheck + " day Count " + dayCount + " Value: " + newTotalForDate);
            dayCount = 0;
            newTotalForDate = new BigDecimal(0);
        }
        if (entryDay != 0) {

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

    public void Extrapolate(ThroughputEntry chartModel, int version) {

        double[] extrapolationData;
        // Future times
        double minute = secCount;
        secCount += 1;

        // Convert throughput time to seconds starting from x0
        if (count == 0) {
            time.add(minute);
            throughput.add(chartModel.getThroughput());
            count++;
        } else {
            if (minute > time.get(count - 1)) {
                time.add(minute);
                throughput.add(chartModel.getThroughput());
                count++;
            }
        }
        throughCount++;

        // Convert ArrayList to array for consumption by Apache functions
        if (throughCount == throughPutList.size()) {

            count = 0;
            x1 = new double[time.size()];
            y1 = new double[throughput.size()];

            Instant extrapEnd = to.toInstant();
            Instant extrapStart = throughPutList.get(throughPutList.size() - 1).getPeriodEnd();
            long extrapMinutes = Duration.between(extrapStart, extrapEnd).getSeconds() / 60;
            x2 = new double[(int) extrapMinutes];

            for (Double min : time) {
                x1[count] = min;
                count++;
            }

            count = 0;
            for (Double through : throughput) {
                y1[count] = through;
                count++;
            }

            // Add future dates to x2
            for (int i = 0; i < extrapMinutes; i++) {
                x2[i] = time.get(time.size() - 1) + (i + 1);
            }
            if (version == 1) {
                extrapolationData = linearExtrapolation(x1, y1, x2);
            } else {
                extrapolationData = polynomialRegresion(x1, y1, x2, grade);
            }

            int addMinute = 60000;
            for (double extrapData : extrapolationData) {

                long extrapDate = (throughPutList.get(throughPutList.size() - 1).getPeriodEnd().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND) + addMinute;
                if (extrapDate < (to.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND)) {
                    map.put(extrapDate, BigDecimal.valueOf(extrapData));
                }
                addMinute += 60000;
            }
        }
    }

    public double[] linearExtrapolation(double[] x, double[] y, double[] xi) {
        SimpleRegression reg = new SimpleRegression();
        for (int i = 0; i < x.length; i++) {
            reg.addData(x[i], y[i]);
        }
        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            yi[i] = reg.predict(xi[i]);
        }
        throughCount =0;
        return yi;
    }

    public double[] polynomialRegresion(double[] x, double[] y, double xi[], int grade) {
        System.out.println(grade);
        PolynomialRegression regression = new PolynomialRegression(x, y, grade);

        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            yi[i] = regression.predict(xi[i]);
        }
        throughCount =0;
        return yi;
    }


    // Data returned grows for large input of y1 e.g. 10000~, same vale returned for small input of y1, e.g. 300~
    public static double[] LinearInterpolator(double[] x1, double[] y1, double[] x2) {
        final PolynomialSplineFunction function = new LinearInterpolator().interpolate(x1, y1);

        final PolynomialFunction[] splines = function.getPolynomials();
        final PolynomialFunction firstFunction = splines[0];
        final PolynomialFunction lastFunction = splines[splines.length - 1];

        final double[] knots = function.getKnots();
        final double firstKnot = knots[0];
        final double lastKnot = knots[knots.length - 1];

        double[] resultList = Arrays.stream(x2).map(aDouble -> {
            if (aDouble > lastKnot) {
                return lastFunction.value(aDouble - knots[knots.length - 2]);
            } else if (aDouble < firstKnot)
                return firstFunction.value(aDouble - knots[0]);
            return function.value(aDouble);
        }).toArray();

        return resultList;
    }

    // Data returned is small in value, small input of y1 extrap returned = -1.7425115345396154E19~,
    // large input of y1 extrap returned = 6.211606668395878E46~
    public double[] NevilleInterpolator(double[] x1, double[] y1, double[] x2) {
        NevilleInterpolator li = new NevilleInterpolator();
        PolynomialFunctionLagrangeForm psf = li.interpolate(x1, y1);

        double[] yi = new double[x2.length];
        for (int i = 0; i < x2.length; i++) {
            yi[i] = psf.value(x2[i]);
        }
        return yi;
    }
}