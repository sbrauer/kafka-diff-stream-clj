;; Based on https://dataissexy.wordpress.com/2016/12/06/quick-recipe-for-kafka-streams-in-clojure/
(ns kafka-diff-stream-clj.core
  (:import [org.apache.kafka.common.serialization Serdes]
           [org.apache.kafka.streams KafkaStreams StreamsConfig]
           [org.apache.kafka.streams.kstream Aggregator Initializer KStreamBuilder ValueMapper]))

(def props
  {StreamsConfig/APPLICATION_ID_CONFIG, "simple-differ"
   StreamsConfig/BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"
   StreamsConfig/KEY_SERDE_CLASS_CONFIG, (.getName (.getClass (Serdes/String)))
   StreamsConfig/VALUE_SERDE_CLASS_CONFIG, (.getName (.getClass (Serdes/String)))})

(def INPUT "topic-input")
(def OUTPUT "topic-output")

(def config
  (StreamsConfig. props))

(def builder
  (KStreamBuilder.))

(def input-topic
  ;; Note we could have more than one input topic (simply add more topic names to the vector).
 (into-array String [INPUT]))

(->
 (.stream builder input-topic)
 (.groupByKey)
 (.aggregate (reify Initializer (apply [this] "INIT")) ; FIXME we really want a map like {:old nil :new nil} or even just an empty map
             (reify Aggregator (apply [this k v agg]
                                 (str agg " -> " v))) ; FIXME: do something smarter... this will keep growing forever!
             (Serdes/String)
             "diff-store")
 (.to OUTPUT))

(def streams
 (KafkaStreams. builder config))

(defn -main [& args]
  (println "Starting...")
  (.start streams)

  ;; Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(.close streams))))
