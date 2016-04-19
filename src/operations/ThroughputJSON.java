package operations;

import db.DBConnection;
import model.ThroughputEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.interpolation.NevilleInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.joda.time.DateTime;

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

    public ThroughputJSON(String sDate, String eDate, String env, String app, Connection connection, DBConnection dbWork, ArrayList calcList) {

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
        this.throughCount =0;
        this.end = "[" + (to.toInstant().getEpochSecond() * TimestampUtils.MILLIS_PER_SECOND + "," + null + "]]}");
        this.time = new ArrayList<>();
        this.throughput = new ArrayList<>();
        this.secCount =0;
        this.calcList = calcList;
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
            for (String calc : calcList) {
                System.out.println(calc);
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
                    if (calc.equals("throughput")) {
                        Default(chartModel);
                    } else if (calc.equals("total")) {
                        Total(chartModel);
                    } else if (calc.equals("min")) {
                        Min(chartModel);
                    } else if (calc.equals("max")) {
                        Max(chartModel);
                    } else if (calc.equals("mean")) {
                        meanCount++;
                        if (meanCount == throughPutList.size()) {
                            entryDay = 0;
                        }
                        Mean(chartModel);
                    } else if (calc.equals("extrap")) {

                        Extrapolation(chartModel);
                    }
                    if (count == throughPutList.size()) {

                        for (Map.Entry<Long, BigDecimal> entry : map.entrySet()) {

                            json += "[" + (entry.getKey() + "," + entry.getValue() + "],");
                        }
                        System.out.println(calc.indexOf(calc) + " " + (calcList.size() -1) );
                        if (calcList.indexOf(calc) != (calcList.size() -1)) {

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
//            System.out.println("Day " + newDayCheck + "day Count" + dayCount + "Value: " + newTotalForDate);
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

    public void Extrapolation(ThroughputEntry chartModel) {

//        double minute = (double)  chartModel.getRetrieved().getEpochSecond();
        double minute = secCount;
        secCount+=1.0;

        if (count == 0) {
            time.add(minute);
            throughput.add(chartModel.getThroughput());
            count++;
        } else {
            if (minute > time.get(count -1)) {
//                System.out.println("Through " + chartModel.getThroughput());
                time.add(minute);
                throughput.add(chartModel.getThroughput());
                count++;
            }
        }
        //        System.out.println("Min " + minute);
        //        System.out.println("Through " + chartModel.getThroughput());
        throughCount++;
/**
 * Run interpolateLinear once add returned value to data set, run interpolateLinear agin to see if the value fluctuates
 * Need to resize minArray & throughArray, possibly right function to achive this
 */
        if (throughCount == throughPutList.size()) {
            System.out.println( count + " Minute  " +minute);
            count =0;
            double [] minArray = new double[time.size()];
            double [] throughArray = new double[throughput.size()];
            double [] extrapArray = new double [8];
//            Instant timer = Instant.now();
            extrapArray[0] = time.get(time.size() -1) + 1.0;
            extrapArray[1] = time.get(time.size() -1) + 2.0;
            extrapArray[2] = time.get(time.size() -1) + 3.0;
            extrapArray[3] = time.get(time.size() -1) + 4.0;
            extrapArray[4] = time.get(time.size() -1) + 5.0;
            extrapArray[5] = time.get(time.size() -1) + 6.0;
            extrapArray[6] = time.get(time.size() -1) + 7.0;
            extrapArray[7] = time.get(time.size() -1) + 8.0;

            for(Double min : time){
//                System.out.println(" Min " + min);
                minArray[count] = min;
                count++;
            }
//            for(double futureTime : extrapArray){
//                System.out.println(futureTime);
//            }
            count =0;
            for(Double through : throughput){
//                System.out.println("Through " +through);
                throughArray[count] = through;
                count++;
            }
//            minArray[minArray.length -1] = extrapArray[extrapArray.length -1] + 1;
//            throughArray[throughArray.length -1] = throughArray[throughArray.length -1];
//            System.out.println( "Min: " +  extrapArray[extrapArray.length -1] + " " +  "Through: " +  throughArray[throughArray.length -1]);

//            double [] test = interpolateLinear(minArray, throughArray, extrapArray);

//            minArray[minArray.length] = time.get(time.size() -1) + 1;
//            throughArray[throughArray.length] = test[0];
//            extrapArray[1] = time.get(time.size() -1) + 2;

            double [] test2 = linearInterp(minArray, throughArray, extrapArray);
            for(int i =0; i < test2.length; i++){
                System.out.println("x2 (Future date value): " +extrapArray[i] + "\n" + "yi (Extrapolated value): " +  (1000 + test2[i]));
            }
        }
    }

    public static double[] interpolateLinear(double[] x1, double[] y1, double[] x2) {
        final PolynomialSplineFunction function = new  LoessInterpolator().interpolate(x1, y1);

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

    public double[] linearInterp(double[] x, double[] y, double[] xi) {
        NevilleInterpolator   li = new NevilleInterpolator (); // or other interpolator
        PolynomialFunctionLagrangeForm  psf = li.interpolate(x, y);

        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            yi[i] = psf.value(xi[i]);
        }
        return yi;
    }
}