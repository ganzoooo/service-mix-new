package mn.unitel.promo;

import mn.unitel.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by dulguun.b on 8/19/16.
 */
public class HelperServlet extends HttpServlet {
    public static Logger logger = LoggerFactory.getLogger(HelperServlet.class);
    Main _sv = (Main) Main.GetInstance(Main.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        OutputStream out = resp.getOutputStream();
        try {
            String uri = req.getRequestURI();
            String outstr = "";

            if(uri.equals("/subscribe_number") ){

            }else if(uri.equals("/check_fp")){
                String phone_no = req.getParameter("phoneNo");
                String member_type = req.getParameter("memberType");
                String group_id = req.getParameter("groupId");
                String balance = req.getParameter("balance");
                String operator = req.getParameter("operator");
                String hasFp = req.getParameter("hasFp");
                outstr = _sv.checkFp(phone_no, member_type, group_id, balance, operator, hasFp);
            }else {
                resp.setStatus(404);
                return;
            }
            out.write(outstr.getBytes());
        } catch (Exception e) {
            resp.setStatus(200);
            logger.error("", e);
            out.write(("FAIL:" + e.getMessage()).getBytes("UTF-8"));
            return;
        }
    }
}