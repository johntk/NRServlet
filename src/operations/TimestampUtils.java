package operations;


import com.ibm.perf.utils.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/** This class is used to format the joda Instants */
public final class TimestampUtils {

    public static final long MILLIS_PER_SECOND = 1000;

    private TimestampUtils() { }

    private static final DateTimeFormatter FORMAT_TIMESTAMP = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

    public static String format(Instant timestamp) {
        return FORMAT_TIMESTAMP.print(timestamp);
    }


    public static Instant parseTimestamp(String value, Instant defaultValue) {
        String cleanValue = StringUtils.clean(value).toUpperCase();

        if (cleanValue.isEmpty()) {
            return defaultValue;
        }

        try {
            return new Instant(format(DateTime.parse(value).toInstant()));
        } catch (NumberFormatException e) {
            /* Fail silently */
        }

        return defaultValue;
    }

}
