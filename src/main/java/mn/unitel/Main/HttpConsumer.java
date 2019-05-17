package mn.unitel.Main;

import mn.unitel.http.rabbitmq.RabbitMQHttpMessage;
import mn.unitel.promo.Main;
import mn.unitel.rabbitmq.RabbitMQConsumer;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;

public class HttpConsumer extends RabbitMQConsumer {

    private static Logger logger = LoggerFactory.getLogger(HttpConsumer.class);
    private Main gw = (Main) Main.GetInstance(Main.class);
    
    private static ConfigurationNode get_node(ConfigurationNode node, String name) {
        if (node.getChildrenCount(name) > 0) {
            return node.getChildren(name).get(0);
        }

        for (ConfigurationNode n : node.getChildren()) {
            ConfigurationNode nd = get_node(n, name);
            if (nd != null) {
                return nd;
            }
        }

        return null;
    }

    @Override
    public void onMessage(byte[] bytes) throws Exception {
        RabbitMQHttpMessage httpMessage = new RabbitMQHttpMessage();
        httpMessage.parse(bytes, 0);
        String[] seqparams = httpMessage.get_seq().split("-");
        String dlg = seqparams[0];
        String seq = seqparams[1];
        String phoneno = seqparams[2];
        XMLConfiguration cnf = new XMLConfiguration();
        cnf.setDelimiterParsingDisabled(true);
        cnf.load(new StringReader(httpMessage.get_data("result").toString()));
        ConfigurationNode node = get_node(cnf.getRootNode(), "Id");
        String status = node.getValue().toString();
        gw.updCbsSendLogAsync("uni_servicelog", seq, status, httpMessage.get_data("response").toString(), httpMessage.get_data("result").toString());
        if (status.equals("SUCCESS")) {
            //Todo Success
        } else {
            //Todo fail
        }

    }
}
