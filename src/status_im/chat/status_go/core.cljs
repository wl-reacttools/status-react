(ns status-im.chat.status-go.core
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [status-im.native-module.core :as status]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.types :as types]))

(defn- invoke-api
  [api-method params on-success on-failure]
  (let [args    {:jsonrpc "2.0"
                 :id      10
                 :method  api-method
                 :params  params}
        payload (.stringify js/JSON (clj->js args))]
    (status/call-private-rpc payload
                             (handlers/response-handler on-success on-failure))))

(defn- invoke-api-with-default-handler
  [api-method params]
  (invoke-api api-method
              params
              #(log/info "join - success!")
              #(log/info "join - failure!")))

(defn- unkeywordize-chat-names
  [chats-response]
  (assoc chats-response
         :chats
         (into {}
               (map
                (fn [[k v]] [(name k) v])
                (:chats chats-response)))))

(re-frame/reg-fx
 :status-go/get-chats
 (fn [cofx]
   (invoke-api "status_chats"
               []
               #(re-frame/dispatch [:chats-list/load-success (unkeywordize-chat-names %)])
               #(re-frame/dispatch [:chats-list/load-failure %]))))

(re-frame/reg-fx
 :status-go/join-public-chat
 (fn [name]
   (invoke-api-with-default-handler "status_joinPublicChat" [name])))

(re-frame/reg-fx
 :status-go/start-one-on-one-chat
 (fn [pubkey]
   (invoke-api-with-default-handler "status_startOneOnOneChat" [pubkey])))

(re-frame/reg-fx
 :status-go/remove-chat
 (fn [chat-id]
   (invoke-api-with-default-handler "status_removeChat" [chat-id])))

;; TODO: group chats
