package operations;

import db.DBConnection;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;


@WebServlet(name = "operations.PreProcessor")
public class PreProcessor extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(PreProcessor.class.getName());

    public PreProcessor() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        DBConnection dbWork;
        dbWork  = DBConnection.createApplication();
        List<String> envChartList = new ArrayList<>();
        List<String> appChartList = new ArrayList<>();
        ArrayList<String> calcList = new ArrayList<>();
        String json = "[";
        String sDate = request.getParameter("start");
        String eDate = request.getParameter("end");
        String env = request.getParameter("env");
        String app = request.getParameter("app");
        String type = request.getParameter("arg");
        int grade =0;
        boolean go =false;

        /** I put this together to do the calculations, was running out of time,
         * this should be cleaned up */
        if(request.getParameter("throughput") != null){
            calcList.add("Throughput");
            go=true;
        }
        if(request.getParameter("min") != null){
            calcList.add("Min");
            go=true;
        }
        if(request.getParameter("max") != null){
            calcList.add("Max");
            go=true;
        }
        if(request.getParameter("mean") != null){
            calcList.add("Mean");
            go=true;
        }
        if(request.getParameter("total") != null){
            calcList.add("Total");
            go=true;
        }
        if(request.getParameter("extrap") != null){
            calcList.add("Linear-Extrap");
            go=true;
        }
        if(request.getParameter("extrap2") != null){
            calcList.add("P-Regression-Extrap");
            go=true;
        }
        if(request.getParameter("grade") != null){
            grade = Integer.parseInt(request.getParameter("grade"));
        }

        JSONArray envAppList = new JSONArray();

        /** Makes a connection to the DB and depending on the type parameter, will either
         * pull all the env / application names from the DB for the dashboard GUI
         * or will get the requested calculations */
        try (Connection connection = dbWork.getConnection()) {
            if(type.equals("list")) {
                List<String> envList = dbWork.getEnvironments(connection);
                envList.forEach(envAppList::put);
                envAppList.put("END");
                List<String> appList = dbWork.getApplications(connection);
                appList.forEach(envAppList::put);
                response.getWriter().write(String.valueOf(envAppList));
            }else {
                if(!env.equals("null")) {
                    try {
                        JSONArray envArray = new JSONArray(env);
                        for (int i = 0; i < envArray.length(); i++) {
                            envChartList.add(envArray.getString(i));
                        }
                        JSONArray appArry = new JSONArray(app);
                        for (int i = 0; i < appArry.length(); i++) {
                            appChartList.add(appArry.getString(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        logger.fatal("Exception happen PreProcessor app / env array fill", e);
                    }
                }

                int count =0;
                int num = (envChartList.size() * appChartList.size());

                for(String environment: envChartList){
                    for(String application: appChartList){
                        ThroughputJSON chart = new ThroughputJSON(sDate, eDate, environment, application, connection, dbWork, calcList, grade);

                        json += chart.Generate(type);
                        if(count != num -1 ){
                            if(go) {
                                if(!json.equals("["))
                                json += ",";
                            }
                        }
                        count++;
                    }
                }
                json += "]";
                System.out.println("json : " + json);
                response.getWriter().write(json);
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.fatal("Exception happen PreProcessor", e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}

