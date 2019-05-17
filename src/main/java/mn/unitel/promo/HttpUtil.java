package mn.unitel.promo;

/**
 * Created by dulguun.b on 11/21/2016.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;

public class HttpUtil {
    public static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    static {
        disableSslVerification();
    }

    private static void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public static String send_post(String url, String data, Map<String, String> headers) throws Exception {
        try {
            URLConnection urlcon = new URL(url).openConnection();
            HttpURLConnection con;
            if (urlcon instanceof HttpsURLConnection) {
                con = (HttpsURLConnection)urlcon;
            } else {
                con = (HttpURLConnection)urlcon;
            }
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");

            if (headers != null) {
                for (String key : headers.keySet()) {
                    con.setRequestProperty(key, headers.get(key));
                }
            }

            con.connect();
            OutputStream out = con.getOutputStream();
            OutputStreamWriter wout = new OutputStreamWriter(out, "UTF8");
            wout.write(data);
            wout.flush();
            out.close();

            logger.debug("Resp Code:" + con.getResponseCode());
            logger.debug("Resp Message:" + con.getResponseMessage());

            InputStream ins;
            if (con.getResponseCode() != 200) {
                ins = con.getErrorStream();
            } else {
                ins = con.getInputStream();
            }

            InputStreamReader isr = new InputStreamReader(ins);
            BufferedReader in = new BufferedReader(isr);

            String inputLine;
            String retval = "";

            while ((inputLine = in.readLine()) != null) {
                retval += inputLine + "\r\n";
            }

            in.close();
            logger.debug("Response String:" + retval);
            return retval;
        } catch (Exception e) {
            logger.debug("", e);
            throw e;
        }
    }

    public static String send_get(String url, Map<String, String> headers) throws Exception {
        try {
            URLConnection urlcon = new URL(url).openConnection();
            HttpURLConnection con;
            if (urlcon instanceof HttpsURLConnection) {
                con = (HttpsURLConnection)urlcon;
            } else {
                con = (HttpURLConnection)urlcon;
            }
            con.setDoInput(true);
            con.setDoOutput(false);
            con.setRequestMethod("GET");

            if (headers != null) {
                for (String key : headers.keySet()) {
                    con.setRequestProperty(key, headers.get(key));
                }
            }

            con.connect();
            logger.debug("Resp Code:" + con.getResponseCode());
            logger.debug("Resp Message:" + con.getResponseMessage());

            InputStream ins;
            if (con.getResponseCode() != 200) {
                ins = con.getErrorStream();
            } else {
                ins = con.getInputStream();
            }

            InputStreamReader isr = new InputStreamReader(ins);
            BufferedReader in = new BufferedReader(isr);

            String inputLine;
            String retval = "";

            while ((inputLine = in.readLine()) != null) {
                retval += inputLine + "\r\n";
            }

            in.close();
            logger.debug("Response String:" + retval);
            return retval;
        } catch (Exception e) {
            logger.debug("", e);
            throw e;
        }
    }

    public static String[] http_post(String url, byte[] data, Map<String, String> headers, int ctime, int rtime) throws Exception {
        //UniMeter meter = MonitoredGateway.umeter("send.http");
        try {
            String rcode;
            logger.debug("connecting to: " + url);
            HttpURLConnection con;
            if (url.startsWith("https")) {
                con = (HttpsURLConnection) new URL(url).openConnection();
            } else {
                con = (HttpURLConnection) new URL(url).openConnection();
            }
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setConnectTimeout(ctime);
            con.setReadTimeout(rtime);
            if (headers != null && !headers.isEmpty()) {
                for (String k : headers.keySet()) {
                    logger.debug("set header:" + k + " - " + headers.get(k));
                    con.setRequestProperty(k, headers.get(k));
                }
            } else {
                // con.setRequestProperty("SOAPAction", "");
                con.setRequestProperty("Content-Type", "application/soap+xml");
            }
            con.connect();
            OutputStream out = con.getOutputStream();
            out.write(data);
            //meter.update(data.length);
            out.flush();
            out.close();

            rcode = con.getResponseCode() + ":" + con.getResponseMessage();
            logger.debug("Resp Code:" + con.getResponseCode());
            logger.debug("Resp Message:" + con.getResponseMessage());

            InputStream ins;
            if (con.getResponseCode() != 200) {
                ins = con.getErrorStream();
            } else {
                ins = con.getInputStream();
            }

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = ins.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            String retval =  result.toString("UTF-8");

//            InputStreamReader isr = new InputStreamReader(ins);
//            BufferedReader in = new BufferedReader(isr);
//
//            String inputLine;
//            String retval = "";
//
//            while ((inputLine = in.readLine()) != null) {
//                retval += inputLine + "\r\n";
//            }
//
//            in.close();
            return new String[]{rcode, retval};
        } catch (Exception e) {
            //logger.debug(e.getMessage());
            throw e;
        } finally {
            //meter.stop();
        }
    }
}
