package mn.unitel.promo;

import mn.unitel.db.DbFactory;
import mn.unitel.dbpool.DbPoolFactory;
import mn.unitel.Main.*;
import mn.unitel.json.JSONArray;
import mn.unitel.json.JSONObject;
import mn.unitel.promo.util.DbLogMain;
import mn.unitel.promo.util.HttpUtil;
import mn.unitel.promo.util.Param;
import mn.unitel.rabbitmq.RabbitMQProducer;
import oracle.jdbc.OracleTypes;
import org.apache.commons.configuration.XMLConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Main extends DbLogMain {

    private DbFactory _db;

    private RabbitMQProducer _smsProd;

    private RabbitMQProducer _ocsProd;
    private OcsConsumer _ocsCons;

    private RabbitMQProducer _httpProd;
    private HttpConsumer _httpCons;

    private Server _server;


    public static void main(String[] args) {
        Main gw = (Main) Main.GetInstance(Main.class);
        try {
            gw.init();
            gw.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }


    @Override
    public Connection getConnection(String dbname) throws Exception {
        return _db.getConnection(dbname);
    }

    @Override
    public String getSpecNum() {
        return "service-mix-new";
    }


    @Override
    public String get_name() {
        return "1477";
    }

    @Override
    public void init() throws Exception {
        XMLConfiguration config = new XMLConfiguration();
        config.setDelimiterParsingDisabled(true);
        config.load(System.getProperty("conf", "config.xml"));

        _db = new DbPoolFactory(config);


        XMLConfiguration cnf = new XMLConfiguration();
        cnf.setDelimiterParsingDisabled(true);
        cnf.load(config.getString("sms.producer"));
        _smsProd = new RabbitMQProducer();
        _smsProd.init(cnf);

        cnf = new XMLConfiguration();
        cnf.setDelimiterParsingDisabled(true);
        cnf.load(config.getString("ocs.consumer"));
        _ocsCons = new OcsConsumer();
        _ocsCons.init(cnf);

        cnf = new XMLConfiguration();
        cnf.setDelimiterParsingDisabled(true);
        cnf.load(config.getString("ocs.producer"));
        _ocsProd = new RabbitMQProducer();
        _ocsProd.init(cnf);

        cnf = new XMLConfiguration();
        cnf.setDelimiterParsingDisabled(true);
        cnf.load(config.getString("http.consumer"));
        _httpCons = new HttpConsumer();
        _httpCons.init(cnf);

        cnf = new XMLConfiguration();
        cnf.setDelimiterParsingDisabled(true);
        cnf.load(config.getString("http.producer"));
        _httpProd = new RabbitMQProducer();
        _httpProd.init(cnf);

        _server = new Server(config.getInt("server.port", 8089));
        ServletContextHandler context = new ServletContextHandler(_server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new HelperServlet()), "/*");

    }

    @Override
    public void start() throws Exception {
        _dbDbLogThread.start();
        _smsProd.startproduce();
//        _ocsCons.startconsume();
//        _ocsProd.startproduce();
//        _httpCons.startconsume();
//        _httpProd.startproduce();
        _server.start();
    }

    @Override
    public void stop() throws Exception {
        _dbDbLogThread.stop();
        _db.shutdown();
        _server.stop();
    }

    public String checkFp(String phoneNo, String memberType, String groupId, String balance, String operator, String hasFp) {
        ArrayList<Param> params;
        Map<String, String> phoneInfo = getPhoneInfo(phoneNo);
        String entr_no = phoneInfo.get("subid");
        if ("y".equals(hasFp)) {
            if ("Member".equals(memberType)) {
                params = new ArrayList<Param>();
                params.add(new Param(OracleTypes.VARCHAR, phoneNo, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, entr_no, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, memberType, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, groupId, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, balance, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, "Y", Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, "", Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, operator, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, null, Param.Direction.OUT));
                params.add(new Param(OracleTypes.NUMBER, null, Param.Direction.OUT));
                Map row = callStoredProcedure("uni_service", "PK_MIX_NEW.INS_FP_INFO", params, this, "", null);
                String result = row.get(9).toString();
                if("000".equals(result)){
                    int seq = Integer.parseInt(row.get(10).toString());
                    result = saveVas(phoneNo, seq, entr_no);
                }
                return result;
            } else {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("content-type", "application/x-www-form-urlencoded");
                String result = "";

                String resp = null;
                try {
                    resp = HttpUtil.send_post("http://10.21.64.31:9802/get_members", "member=" + phoneNo + "&operator=" + operator, headers, 10000);
                } catch (Exception e) {
                    logger.error("ERROR ON CALLING EXTERNAL API " + e);
                    return e.toString();
                }
                JSONArray subInfo = new JSONArray(resp);
                String phone = "";
                for (int i = 0; i < subInfo.length(); i++) {
                    JSONObject obj = subInfo.getJSONObject(i);
                    if(i == subInfo.length()-1)
                        phone = phone + obj.getString("phoneno");
                    else
                        phone = phone + obj.getString("phoneno") + ",";
                }


                params = new ArrayList<Param>();
                params.add(new Param(OracleTypes.VARCHAR, phoneNo, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, entr_no, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, memberType, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, groupId, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, balance, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, "Y", Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, phone, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, operator, Param.Direction.IN));
                params.add(new Param(OracleTypes.VARCHAR, null, Param.Direction.OUT));
                params.add(new Param(OracleTypes.NUMBER, null, Param.Direction.OUT));
                Map row = callStoredProcedure("uni_service", "PK_MIX_NEW.INS_FP_INFO", params, this, "", null);
                result = row.get(9).toString();
                if("000".equals(result)){
                    int seq = Integer.parseInt(row.get(10).toString());
                    result = saveVas(phoneNo, seq, entr_no);
                }
                return result;
            }
        }else {
            params = new ArrayList<Param>();
            params.add(new Param(OracleTypes.VARCHAR, phoneNo, Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, entr_no, Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, "", Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, "", Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, "", Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, "N", Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, "", Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, operator, Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, null, Param.Direction.OUT));
            params.add(new Param(OracleTypes.NUMBER, null, Param.Direction.OUT));
            Map row = callStoredProcedure("uni_service", "PK_MIX_NEW.INS_FP_INFO", params, this, "", null);
            String result = row.get(9).toString();
            if("000".equals(result)){
                int seq = Integer.parseInt(row.get(10).toString());
                result = saveVas(phoneNo, seq, entr_no);
            }
            return result;
        }
    }

    public String saveVas(String phone, int seqNo, String entr_no){
        Double remains = 0.0;
        String crbt = "N";
        String msl = "N";
        ArrayList<Param> params;
        try {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("content-type", "application/json");

            String resp = HttpUtil.send_post("http://10.21.8.70:8085/ocs/user/976" + phone, "", headers, 10000);
            JSONObject subInfo = new JSONObject(resp);

            JSONArray values = subInfo.getJSONObject("1").getJSONArray("counter");
            JSONArray valuesProduct = subInfo.getJSONObject("1").getJSONArray("product");

            for (int i = 0; i < values.length(); i++) {
                JSONObject ob = values.getJSONObject(i);
                int counter_id = ob.getInt("counter_id");
                if (0 == counter_id) {
                    remains = ob.getDouble("counter_value");
                    break;
                }
            }
            for (int i = 0; i < valuesProduct.length(); i++) {
                JSONObject ob = valuesProduct.getJSONObject(i);
                String prod_id = ob.getString("product_id");
                if ("crbt_p".equals(prod_id))
                    crbt = "Y";
                if("msl_p".equals(prod_id))
                    msl = "Y";
                if(prod_id.startsWith("um_pack")){
                    params = new ArrayList<Param>();
                    params.add(new Param(OracleTypes.VARCHAR, entr_no, Param.Direction.IN));
                    params.add(new Param(OracleTypes.VARCHAR, prod_id, Param.Direction.IN));
                    callStoredProcedure("uni_service", "PK_MIX_NEW.INS_UMEDIA", params, this, "", null);
                }
            }

            params = new ArrayList<Param>();
            params.add(new Param(OracleTypes.NUMBER, seqNo, Param.Direction.IN));
            params.add(new Param(OracleTypes.NUMBER, remains, Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, crbt, Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, msl, Param.Direction.IN));
            params.add(new Param(OracleTypes.VARCHAR, null, Param.Direction.OUT));
            Map row = callStoredProcedure("uni_service", "PK_MIX_NEW.AFTER_DIS", params, this, "", null);
            String result = row.get(5).toString();
            return result;
        }catch (Exception ex){
            logger.error("-------------ERROR ON SAVING VAS---------------" + ex);
            return "FAIL " + ex;
        }
    }

}
