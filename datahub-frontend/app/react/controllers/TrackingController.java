package react.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import react.auth.Authenticator;
import javax.annotation.Nonnull;
import javax.inject.Inject;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import react.graphql.PlayQueryContext;

import java.util.Properties;

public class TrackingController extends Controller {

    private final Boolean _isEnabled;
    private final Config _config;
    private final KafkaProducer<String, String> _producer;
    private final String _topic;

    @Inject
    public TrackingController(@Nonnull Config config) {
        _config = config;
        _isEnabled = !config.hasPath("analytics.enabled") || config.getBoolean("analytics.enabled");
        if (_isEnabled) {
            _producer = createKafkaProducer();
            _topic = config.getString("analytics.tracking.topic");
        } else {
            _producer = null;
            _topic = null;
        }
    }

    @Security.Authenticated(Authenticator.class)
    @Nonnull
    public Result track() throws Exception {
        if (!_isEnabled) {
            // If tracking is disabled, simply return a 200.
            return status(200);
        }

        JsonNode event;
        try {
            event = request().body().asJson();
        } catch (Exception e) {
            return badRequest();
        }
        try {
            final String actor = new PlayQueryContext(ctx(), _config).getActor();
            final ProducerRecord<String, String> record = new ProducerRecord<>(
                    _topic,
                    actor,
                    event.toString());
             _producer.send(record);
             _producer.flush();
             return ok();
        } catch(Exception e) {
            return internalServerError(e.getMessage());
        }
    }

    @Override
    protected void finalize() {
        _producer.close();
    }

    private KafkaProducer createKafkaProducer() {
        final Properties props = new Properties();
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "datahub-frontend");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, _config.getString("analytics.kafka.bootstrap.server"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer"); // Actor urn.
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer"); // JSON object.

        final String securityProtocolConfig = "analytics.kafka.security.protocol";
        if (_config.hasPath(securityProtocolConfig)
            && _config.getString(securityProtocolConfig).equals(SecurityProtocol.SSL)) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, _config.getString(securityProtocolConfig));
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, _config.getString("analytics.kafka.ssl.key.password"));

            props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, _config.getString("analytics.kafka.ssl.keystore.type"));
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, _config.getString("analytics.kafka.ssl.keystore.location"));
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, _config.getString("analytics.kafka.ssl.keystore.password"));

            props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, _config.getString("analytics.kafka.ssl.truststore.type"));
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, _config.getString("analytics.kafka.ssl.truststore.location"));
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, _config.getString("analytics.kafka.ssl.truststore.password"));

            props.put(SslConfigs.SSL_PROTOCOL_CONFIG, _config.getString("analytics.kafka.ssl.protocol"));
            props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, _config.getString("analytics.kafka.ssl.endpoint.identification.algorithm"));
        }

        return new KafkaProducer(props);
    }
}
