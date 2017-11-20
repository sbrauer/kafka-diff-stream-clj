# kafka-diff-stream-clj

A proof-of-concept Kafka Streams processor (in Clojure) that does simple diffing.
Note this uses the Processor API (not the higher-level DSL).

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
# Note: Kill Kafka BEFORE killing Zookeeper.
bin/kafka-server-start.sh config/server.properties

# Create input topic
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic-input

# Create output topic
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topic-output

# Watch the output topic (in foreground; do this in a separate terminal; kill with Ctrl-C):
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic topic-output --property print.key=true --property key.separator=":" --from-beginning

# Produce some input message (runs in foreground; enter one message per line, where each message is a key and value separated by a colon, such as `foo:Hello World`; Ctrl-D to exit)
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topic-input --property parse.key=true --property key.separator=:
```

Now to run the app and produce some input data...

```
# Run the stream differ app (in foreground; do this in a separate terminal; kill with Ctrl-C):
lein run
```

Input messages using the `kafka-console-producer.sh`.
Let's say you supply an input message like this:
```
a:apple
```
That's the key `a` with the value `apple`.

Now follow that up with a new value for the key `a`:
```
a:aardvark
```

You should then see (perhaps after a very brief delay) the following in your consumer output:
```
a:apple -> aardvark
```
That's the key `a` with a value indicating that `apple` changed to `aardvark`.

Try adding more messages, including dupes (like `a:aardvark` again).
You should see a diff event output whenever a key's value changes.

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
