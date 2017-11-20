;; Originally based on https://dataissexy.wordpress.com/2016/12/06/quick-recipe-for-kafka-streams-in-clojure/
;; Then updated to use Processor API (instead of Streams DSL)
;; See https://docs.confluent.io/current/streams/developer-guide/processor-api.html

(ns kafka-diff-stream-clj.core
  (:import [org.apache.kafka.common.serialization Serdes]
           [org.apache.kafka.streams.processor Processor ProcessorContext ProcessorSupplier TopologyBuilder]
           [org.apache.kafka.streams KafkaStreams StreamsConfig]
           [org.apache.kafka.streams.state Stores]))

(def props
  {StreamsConfig/APPLICATION_ID_CONFIG, "simple-differ"
   StreamsConfig/BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"
   StreamsConfig/KEY_SERDE_CLASS_CONFIG, (.getName (.getClass (Serdes/String)))
   StreamsConfig/VALUE_SERDE_CLASS_CONFIG, (.getName (.getClass (Serdes/String)))})

(def INPUT "topic-input")
(def OUTPUT "topic-output")
(def STORE-NAME "Diff-Store")
(def SCHEDULE 1000) ;; time units are normally milliseconds

(def config
  (StreamsConfig. props))

(defn- print-kv-iter
  [iter]
  (when (.hasNext iter)
    (let [kv (.next iter)]
      (prn kv))
    (print-kv-iter iter)))

(defn print-kv-store
  "Print all the keys/values in a KV store. For dev debugging."
  [kv-store]
  (let [iter (.all kv-store)]
    (print-kv-iter iter)
    (.close iter)))

(defn str-array
  "Helper for passing a string to a Java method that expects a vararg of strings."
  [s]
  (into-array String [s]))

(defn format-diff
  "Helper for building string payload value for diff records."
  [old new]
  (str old " -> " new))

(deftype DiffProcessor [^{:volatile-mutable true} context
                        ^{:volatile-mutable true} kv-store]
  Processor

  (^void init [this ^ProcessorContext c]
   (set! context c)
   (set! kv-store (.getStateStore context STORE-NAME))

   ;; FIXME: remove after dev
   (println "*** init ***")
   (print-kv-store kv-store)

   (.schedule context SCHEDULE))

  (process [this k v]
    ;; FIXME: remove after dev
    (println "*** process ***")
    (prn {:k k :v v})
    (print-kv-store kv-store)

    (let [old (.get kv-store k)]
      (when (not= old v)
        (.put kv-store k v)
        (when old
          (.forward context k (format-diff old v))))))

  (punctuate [this timestamp]
    (.commit context))

  (close [this]
    (.close kv-store)))

(def diff-store-supplier
  (-> (Stores/create STORE-NAME)
      (.withKeys (Serdes/String))
      (.withValues (Serdes/String))
      (.persistent)
      (.build)))

(def builder (TopologyBuilder.))

(-> builder
    (.addSource "Source" (str-array INPUT))
    (.addProcessor "Process"
                   (reify ProcessorSupplier
                     (get [this] (->DiffProcessor nil nil)))
                   (str-array "Source"))
    (.addStateStore diff-store-supplier (str-array "Process"))
    (.addSink "Sink" OUTPUT (str-array "Process")))

(def streams
 (KafkaStreams. builder config))

(defn -main [& args]
  (println "Starting...")

  ;; Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
  ;; FIXME: Is this working? Not seeing println output :(
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(do
                                (println "Stopping...")
                                (.close streams)
                                (println "Bye!"))))

  (.start streams))
