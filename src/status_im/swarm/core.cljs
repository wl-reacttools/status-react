(ns status-im.swarm.core
  (:refer-clojure :exclude [cat])
  (:require [status-im.utils.fx :as fx]
            [re-frame.core :as re-frame]
            [status-im.native-module.core :as status]
            [clojure.string :as string]
            [status-im.utils.handlers :as handlers]
            [status-im.js-dependencies :as dependencies]))

(def utils dependencies/web3-utils)

(def status-profile-topic "status-profile")

;; we currently use a swarm gateway but this detail is not relevant
;; outside of this namespace
(def swarm-gateway "https://swarm-gateways.net")
(def bzz-url (str swarm-gateway "/bzz:/"))
(def bzz-feed-url (str swarm-gateway "/bzz-feed:/"))

(defn bzz-read-feed-url
  [name user]
  (str bzz-feed-url
       "?name=" name
       "&user=" user))

(defn bzz-read-feed-template-url
  [name user]
  (str (bzz-read-feed-url name user) "&meta=1"))

(defn bzz-post-feed-update-url
  [{:keys [topic user level time protocol-version signature]}]
  (str bzz-feed-url
       "?topic=" topic
       "&user=" user
       "&level=" level
       "&time=" time
       "&protocolVersion=" protocol-version
       "&signature=" signature))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn number-to-hex
  [n]
  (subs (.numberToHex utils n) 2))

(defn pad-left
  [str n]
  (.padLeft utils str n))

(defn pad-right
  [str n]
  (.padRight utils str n))

(defn UInt32LE
  "Converts an integer into little endian in an hex encoded string"
  [i]
  (->> i
       number-to-hex
       reverse
       (partition 2)
       (map reverse)
       flatten
       (apply str)))

(defn utf8-to-hex
  [s]
  (subs (.utf8ToHex utils s) 2))

(defn sha3 [s]
  (.sha3 utils s))

(defn hex-to-bytes [s]
  (.hexToBytes utils s))

(defn bytes-to-hex [b]
  (.bytesToHex utils b))

(defn digest
  [data {:keys [feed epoch protocolVersion]}]
  (let [{:keys [topic user]} feed
        {:keys [time level]} epoch
        digest (str "0x"
                    (pad-left (number-to-hex 0) 2)
                    (pad-left "" 14)
                    (subs topic 2)
                    (subs user 2)
                    (pad-right (UInt32LE time) 14)
                    (pad-left (number-to-hex level) 2)
                    (utf8-to-hex data))]
    (sha3 digest)))

(re-frame/reg-fx
 ::sign-digest
 (fn [{:keys [digest callback]}]
   (status/private-sign-hash
    digest
    callback)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; swarm api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(fx/defn upload-data
  [cofx {:keys [data on-success on-failure]}]
  {:http-post (cond-> {:url bzz-url
                       :data data
                       :opts {:headers {"Content-Type" "application/json"}}
                       :timeout-ms 5000
                       :success-event-creator
                       (fn [{:keys [response-body]}]
                         (on-success response-body))}
                on-failure
                (assoc :failure-event-creator on-failure))})

(fx/defn post-feed-update
  [cofx {:keys [data signature feed-template] :as update-params}]
  (let [{:keys [feed epoch protocolVersion]} feed-template
        {:keys [topic user]} feed
        {:keys [time level]} epoch]
    (let [on-success (fn [response]
                       [:swarm.callback/post-feed-update-success
                        (assoc update-params :response (js/JSON.parse response))])
          on-failure (fn [error]
                       [:swarm.callback/post-feed-update-error
                        (assoc update-params :error error)])]
      {:http-post (cond-> {:url (bzz-post-feed-update-url {:topic topic
                                                           :user user
                                                           :level level
                                                           :time time
                                                           :protocol-version protocolVersion
                                                           :signature signature})
                           :data data
                           :timeout-ms 5000
                           :success-event-creator
                           (fn [{:keys [response-body]}]
                             (on-success response-body))}
                    on-failure
                    (assoc :failure-event-creator on-failure))})))

(fx/defn get-feed-template
  [cofx {:keys [name user] :as update-params}]
  (let [on-success (fn [response]
                     (let [feed-template (js->clj (js/JSON.parse response)
                                                  :keywordize-keys true)]
                       [:swarm.callback/get-feed-template-success
                        (assoc update-params :feed-template feed-template)]))
        on-failure (fn [error]
                     [:swarm.callback/get-feed-template-failure
                      (assoc update-params :error error)])]
    {:http-get (cond-> {:url (bzz-read-feed-template-url name user)
                        :timeout-ms 5000
                        :success-event-creator
                        (fn [response]
                          (on-success response))}
                 on-failure
                 (assoc :failure-event-creator on-failure))}))

(fx/defn read-file
  [cofx {:keys [swarm-hash on-success on-failure]}]
  {:http-get (cond-> {:url (str bzz-url swarm-hash "/")
                      :timeout-ms 5000
                      :success-event-creator
                      (fn [response]
                        (on-success response))}
               on-failure
               (assoc :failure-event-creator on-failure))})

(fx/defn read-feed
  [cofx {:keys [user name on-success on-failure]}]
  (println (bzz-read-feed-url name user))
  {:http-get (cond-> {:url (bzz-read-feed-url name user)
                      :timeout-ms 5000
                      :success-event-creator
                      (fn [response]
                        (on-success response))}
               on-failure
               (assoc :failure-event-creator on-failure))})

(fx/defn read-profile
  [cofx {:keys [user on-success on-failure]}]
  (read-feed cofx {:user user
                   :name status-profile-topic
                   :on-success (fn [swarm-hash]
                                 (println swarm-hash)
                                 [:swarm.profile.callback/read-feed-success
                                  {:swarm-hash swarm-hash
                                   :on-success on-success
                                   :on-failure on-failure}])}))

(fx/defn sign-feed-update
  [cofx {:keys [data feed-template] :as update-params}]
  {::sign-digest {:digest (digest data feed-template)
                  :callback (fn [signature]
                              (re-frame/dispatch
                               [:swarm.callback/sign-digest-success
                                (assoc update-params :signature signature)]))}})

(fx/defn update-feed
  [cofx data]
  (when-let [user-address (get-in cofx [:db :account/account :address])]
    (upload-data cofx
                 {:data data
                  :on-success (fn [swarm-hash]
                                [:swarm.callback/upload-data-success
                                 {:user (str "0x" user-address)
                                  :name status-profile-topic
                                  :data swarm-hash}])})))

(fx/defn verify-feed
  [cofx update-params]
  (read-feed cofx
             (assoc update-params
                    :on-success #(re-frame/dispatch
                                  [:swarm.callback/update-feed-success])
                    :on-error #(re-frame/dispatch
                                [:swarm.callback/update-feed-error
                                 (assoc update-params :error %)]))))

(handlers/register-handler-fx
 :swarm.callback/upload-data-success
 (fn [cofx [_ update-params]]
   (get-feed-template cofx update-params)))

(handlers/register-handler-fx
 :swarm.callback/get-feed-template-success
 (fn [cofx [_ update-params]]
   (sign-feed-update cofx update-params)))

(handlers/register-handler-fx
 :swarm.callback/get-feed-template-failure
 (fn [cofx [_ update-params]]
   #_(println update-params)))

(handlers/register-handler-fx
 :swarm.callback/sign-digest-success
 (fn [cofx [_ update-params]]
   (post-feed-update cofx update-params)))

(handlers/register-handler-fx
 :swarm.callback/post-feed-update-success
 (fn [cofx [_ update-params]]
   (verify-feed cofx update-params)))

(handlers/register-handler-fx
 :swarm.callback/post-feed-update-error
 (fn [cofx [_ update-params]]
   #_(println update-params)))

(handlers/register-handler-fx
 :swarm.callback/verify-feed-success
 (fn [cofx [_ update-params]]
   (println update-params)))

(handlers/register-handler-fx
 :swarm.callback/read-feed-success
 (fn [cofx [_ params]]
   (read-file cofx params)))

(handlers/register-handler-fx
 :swarm.callback/read-file-success
 (fn [cofx [_ update-params]]
   (println update-params)))

(handlers/register-handler-fx
 :swarm.profile.callback/read-feed-success
 (fn [cofx [_ params]]
   (read-file cofx params)))

(handlers/register-handler-fx
 :update-feed
 (fn [cofx [_ data]]
   (update-feed cofx data)))

(handlers/register-handler-fx
 :read-feed
 (fn [cofx _]
   (when-let [user-address (get-in cofx [:db :account/account :address])]
     (read-feed cofx {:user (str "0x" user-address)
                      :name status-profile-topic
                      :on-success (fn [response]
                                    [:swarm.callback/verify-feed-success response])
                      :on-failure (fn [response]
                                    [:swarm.callback/verify-feed-success response])}))))

(handlers/register-handler-fx
 :upload-data
 (fn [cofx [_ data]]
   (when-let [user-address (get-in cofx [:db :account/account :address])]
     (upload-data cofx {:data data
                        :on-success (fn [response]
                                      [:swarm.callback/verify-feed-success response])
                        :on-failure (fn [response]
                                      [:swarm.callback/verify-feed-success response])}))))

(handlers/register-handler-fx
 :read-profile
 (fn [cofx _]
   (when-let [user-address (get-in cofx [:db :account/account :address])]
     (read-profile cofx {:user (str "0x" user-address)
                         :on-success (fn [response]
                                       [:swarm.callback/verify-feed-success response])
                         :on-failure (fn [response]
                                       [:swarm.callback/verify-feed-success response])}))))
