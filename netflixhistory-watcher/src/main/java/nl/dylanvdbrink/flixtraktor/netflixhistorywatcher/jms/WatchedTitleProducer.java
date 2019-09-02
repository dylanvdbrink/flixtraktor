package nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.jms;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.jms.*;

@Component
@CommonsLog
public class WatchedTitleProducer {

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${activemq.topic}")
    private String queueName;

    @SuppressWarnings("squid:S2095")
    public void sendMessage(final String message) {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(destination);

            if (message != null && message.length() != 0) {
                producer.send(session.createTextMessage(message));
            }

            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            log.error("Could not send message", e);
        }
    }
}
