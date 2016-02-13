package operations;

import db.DBConnection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


@WebServlet(name = "operations.PreProcessor")
public class PreProcessor extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public PreProcessor() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        DBConnection dbWork;
        dbWork  = DBConnection.createApplication();

        String sDate = request.getParameter("start");
        String eDate = request.getParameter("end");
        String env = request.getParameter("env");
        String app = request.getParameter("app");
        System.out.println("Env: " + env);
        System.out.println("App : " + app);
        System.out.println("Start Date : " + sDate);
        System.out.println("End Date : " + eDate);
        try (Connection connection = dbWork.getConnection()) {

            ThroughputJSON chart = new ThroughputJSON();
            String json = chart.TotalThroughput(sDate, eDate, env, app, connection, dbWork);
            System.out.println("json : " + json);
            response.getWriter().write(json);

        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
            e.printStackTrace();

        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}

