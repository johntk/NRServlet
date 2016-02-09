package operations;

import db.DBConnection;
import model.ThroughputEntry;
import org.joda.time.Instant;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;


@WebServlet(name = "operations.PreProcessor")
public class PreProcessor extends HttpServlet {
    private static final long serialVersionUID = 1L;
    DBConnection interop;
    List<ThroughputEntry> throughPutList;

    public PreProcessor() {
        super();
        interop  = DBConnection.createApplication();;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {}


    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String json = "";
        String sDate = request.getParameter("start");
        String eDate = request.getParameter("end");
        String app = request.getParameter("app");
        String env = request.getParameter("env");
        System.out.println("App : " + app);
        System.out.println("Start Date : " + sDate);
        System.out.println("End Date : " + eDate);

//        Instant from =  TimestampUtils.parseTimestamp(sDate,null);
//        Instant to = TimestampUtils.parseTimestamp(eDate,null);

        if (sDate != null && eDate != null) {
            Timestamp from = TimestampUtils.parseTimestamp(sDate, null);
            Timestamp to = TimestampUtils.parseTimestamp(eDate, null);
            System.out.println("Start Date : " + from);
            System.out.println("End Date : " + to);

            try {
                throughPutList = interop.getThroughputEntriesInTimespan(env, app, from, to);
            } catch (Exception e) {
                e.printStackTrace();
            }

            int counter = 1;
            if (throughPutList != null) {
                json += "[";
            }
            for (ThroughputEntry chartModel : throughPutList) {
                json += "[" + (chartModel.getRetrieved().getEpochSecond()* TimestampUtils.MILLIS_PER_SECOND + "," + chartModel.getThroughput() +  "]");

                if (counter < throughPutList.size()) {
                    json += ",";
                }
                counter++;
            }
            if (throughPutList != null) {
                json += "]";
            }
            if (throughPutList == null) {
                json = "No record found";
            }
        } else {
            json = "Date must be selected." + "App : " + app + " " + eDate;

        }
        response.getWriter().write(json);
    }
}

