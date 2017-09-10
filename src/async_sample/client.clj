(ns async-sample.client
  (:require [async-connect.client :refer [connect connection-factory] :as async-connect]
            [async-connect.box :refer [error? exception]]
            [clojure.core.async :refer [onto-chan go-loop chan <! >!]]
            [async-connect.netty :refer [bytebuf->string string->bytebuf]])
  (:import [io.netty.handler.codec
              LineBasedFrameDecoder])
  (:gen-class))

(defn- initialize-channel
  [netty-ch config]
  (.. netty-ch
      (pipeline)
      (addLast "framedecoder" (LineBasedFrameDecoder. 5048))))

(def bootstrap
  (async-connect/make-bootstrap
    {:client.config/channel-initializer initialize-channel}))

(defn -main
  [& args]
  (let [read-ch (chan 1 bytebuf->string)
        write-ch (chan 1 string->bytebuf)
        factory (connection-factory bootstrap)
        conn (connect factory "localhost" 3000 read-ch write-ch)]

    ;; 書き込み
    (onto-chan write-ch
               (for [data (conj (vec args) "end")]
                 {:message (str data "\n"), :flush? true})
               false)

    ;; レスポンス読み取り
    (go-loop []
      (when-let [response (<! read-ch)]
        (println "response:" @response)

        (when (= @response "END")
          (async-connect/close conn)
          (println "finished.")
          (System/exit 0))

        (recur)))))
