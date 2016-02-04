package operations;


import db.DBConnection;
import model.ThroughputEntry;
import org.json.JSONObject;

public class JSONResponse {

    DBConnection interop;

    public JSONResponse(){

         interop = DBConnection.createApplication();
    }


    public JSONObject deafultResponse(String env, String app) throws Exception {



        JSONObject response = new JSONObject();


        return response;
    }
}
