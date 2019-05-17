package mn.unitel.Main;

import mn.unitel.ocs.rabbitmq.RabbitMQOcsMessage;
import mn.unitel.promo.Main;
import mn.unitel.rabbitmq.RabbitMQConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsConsumer extends RabbitMQConsumer{
    
    private static Logger logger = LoggerFactory.getLogger(OcsConsumer.class);
    private Main gw = (Main) Main.GetInstance(Main.class);


    @Override
    public void onMessage(byte[] bytes) throws Exception {
        RabbitMQOcsMessage message = new RabbitMQOcsMessage();
        message.parse(bytes,0);
        String result = message.get_strdata();
        String seqString = message.get_seq();
        String dlg = seqString.split("\\-")[0];
        String seq = seqString.split("\\-")[1];
        String phoneno = seqString.split("\\-")[2];
        String status = "";
        if (result.contains("RESULT = SUCCESS")) {
            status = "SUCCESS";
        } else {
            status = "FAIL";
        }
        gw.updOcsSendLogAsync("uni_servicelog", seq, status, message.get_result() + "", result);
        logger.info("OCS RESULT:" + seqString + "," + status + "," + phoneno);
        //Todo onOcsResult
    }

    private static String getExpireDate(String ocsStr) {
        String str = ocsStr;
        String indexStr = "<<SUBSCRIPTION_STATE_INFORMATION>> ----------------------------------------------------------------------------------------------\n";
        String endStr = "  +--------------------------------------------------------------------------------------------------------------------------+\n"
                + "\n"
                + "<<PRODUCT_INFORMATION>> ---------------------------------------------------------------------------------------------------------\n";
        int start = str.indexOf(indexStr) + indexStr.length();
        int end = str.indexOf(endStr);

        str = str.substring(start, end);
        String[] strs = str.split("\n");
        for (int i = 0; i < strs.length; i++) {
            String[] ss = strs[i].split("\\|");
            for (int k = 0; k < ss.length; k++) {
                if (ss[k].trim().contains("*ACTIVE")) {
                    return ss[k + 2].trim().split("~")[1].trim();
                }
            }
        }
        return "";
    }

    public static int getGg(String ocsResult) {
        String str = ocsResult;
        String indexStr = "<<COUNTER_GROUP_INFORMATION>> ---------------------------------------------------------------------------------------------------\n"
                + "  +---------------------------------------------------------------------------------------------------------------------------+\n";
        String endStr = "  +---------------------------------------------------------------------------------------------------------------------------+\n"
                + "<<COUNTER_INFORMATION>> ---------------------------------------------------------------------------------------------------------\n";
        int start = str.indexOf(indexStr) + indexStr.length();
        int end = str.indexOf(endStr);

        str = str.substring(start, end);
        String point = "-1";
        String[] strs = str.split("\n");
        for (int i = 0; i < strs.length; i++) {
            String[] ss = strs[i].split("\\|");
            for (int k = 1; k < ss.length; k++) {
                if (ss[k].trim().split("\\ ")[0].equals("POINT")) {
                    point = ss[k + 1].trim().split("\\ ")[0];
                    return Integer.parseInt(point);
                }
            }
        }
        return 0;
    }

    public static String getServiceType(String ocsResult) {
        String str = ocsResult;
        String indexStr = "  +< MAIN     >---------------------------------------------------------------------------------------------------------------+\n";
        String endStr = "  +< ADDITIONAL >-------------------------------------------------------------------------------------------------------------+\n";
        int start = str.indexOf(indexStr) + indexStr.length();
        int end = str.indexOf(endStr);

        str = str.substring(start, end);
        String ServiceType = "";
        String[] strs = str.split("\n");
        String[] ss = strs[0].split("\\|");
        ServiceType = ss[0].trim();
        if (ServiceType.matches("101|102|103|104")) {
            String smartFlag = getCounterValue(ocsResult, "12611");
            if (smartFlag.equals(""))
                smartFlag = "1";
            ServiceType = ServiceType + "-" + smartFlag;
        }
        return ServiceType;


    }

    public static String getCounterValue(String ocsResult, String counterId) {
        String indexStr = "+< OPTIONAL >--------------------------------------------------------------------------------------------+\n";
        String endStr ="";
        if(ocsResult.contains("  +< ROLLOVER >--------------------------------------------------------------------------------------------+"))
            endStr = "  +< ROLLOVER >--------------------------------------------------------------------------------------------+";
        else
            endStr = "<<GROUP_INFORMATION>> -----------------------------------------------------------------------------------------------------------";

        int start = ocsResult.indexOf(indexStr) + indexStr.length();
        int end = ocsResult.indexOf(endStr);

        ocsResult = ocsResult.substring(start, end);
        String[] strs = ocsResult.split("\n");

        String counterValue = "";
        for (int i = 0; i < strs.length; i++) {
            String[] ss = strs[i].split("\\|");
            if (ss[1].trim().equals(counterId)) {
                counterValue = ss[4].trim().split("\\ ")[0].trim();
                return counterValue;
            }
        }
        return "";

    }

    public static String getCounterExpire(String ocsResult, String counterId) {
        String indexStr = "+< OPTIONAL >--------------------------------------------------------------------------------------------+\n";
        String endStr ="";
        if(ocsResult.contains("  +< ROLLOVER >--------------------------------------------------------------------------------------------+"))
            endStr = "  +< ROLLOVER >--------------------------------------------------------------------------------------------+";
        else
            endStr = "<<GROUP_INFORMATION>> -----------------------------------------------------------------------------------------------------------";
        int start = ocsResult.indexOf(indexStr) + indexStr.length();
        int end = ocsResult.indexOf(endStr);

        ocsResult = ocsResult.substring(start, end);
        String[] strs = ocsResult.split("\n");
        String counterExpire = "";
        for (int i = 0; i < strs.length; i++) {
            String[] ss = strs[i].split("\\|");
            if (ss[1].trim().equals(counterId)) {
                counterExpire = ss[7].trim();
                return counterExpire;
            }
        }
        return "";
    }


}
