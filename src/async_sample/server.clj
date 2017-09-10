(ns async-sample.server
  (:require [clojure.core.async :refer [<! >! go-loop chan]]
            [clojure.string :refer [upper-case]]
            [async-connect.netty :refer [bytebuf->string string->bytebuf]]
            [async-connect.server :refer [run-server close-wait]])
  (:import [io.netty.handler.codec
              LineBasedFrameDecoder])
  (:gen-class))

(defn- channel-initializer
  [netty-ch config]
  (.. netty-ch
      (pipeline)
      (addLast "framedecoder" (LineBasedFrameDecoder. 5048)))
    netty-ch)


(defn- request-handler
  [ctx read-ch write-ch]
  (go-loop
    []
    ;; データが来たら
    (when-let [data (<! read-ch)]
      (let [upper (upper-case @data)]
        ;; 大文字にして書き返す
        (when (>! write-ch {:message (str upper "\n"), :flush? true})
          (recur))))))


(def config
  {:server.config/port    3000
   :server.config/address "localhost"
   :server.config/read-channel-builder   (fn [_] (chan 1 bytebuf->string))
   :server.config/write-channel-builder  (fn [_] (chan 1 string->bytebuf))
   :server.config/channel-initializer    channel-initializer
   :server.config/server-handler-factory (fn [address port] request-handler)})


(defn -main
  [& args]
  (let [server (run-server config)]
    (close-wait server #(println "Server stopped."))))
