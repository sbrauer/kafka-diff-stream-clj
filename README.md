# kafka-diff-stream-clj

A proof-of-concept Streams API processor (in Clojure) that does simple diffing.
It listens to 

## Usage

To try it out locally, you'll need to run Kafka.
Here's one way to do that...

```
wget http://mirror.metrocast.net/apache/kafka/1.0.0/kafka_2.11-1.0.0.tgz
tar xfz kafka_2.11-1.0.0.tgz
cd kafka_2.11-1.0.0

# Start Zookeeper (in foreground; do this is a separate terminal; kill with Ctrl-C)
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka (in foreground; do this is a separate terminal; kill with Ctrl-C)
bin/kafka-server-start.sh config/server.properties

# Create input topic
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic-input

# Create output topic
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic-output
```

Now to run the app and produce some input data...

```
# Run the stream differ app (in foreground; do this in a separate terminal; kill with Ctrl-C):
lein run

# Watch the output topic (in foreground; do this in a separate terminal; kill with Ctrl-C):
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic topic-output --property print.key=true --property key.separator=":" --from-beginning

# Produce some input message (runs in foreground; enter one message per line, where each message is a key and value separated by a colon, such as `foo:Hello World`; Ctrl-D to exit)
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topic-input --property parse.key=true --property key.separator=:
```

FIXME: Describe expected output and maybe provide examples.


## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
