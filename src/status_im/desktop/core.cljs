(ns status-im.desktop.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            status-im.utils.db
            status-im.ui.screens.db
            status-im.ui.screens.events
            status-im.ui.screens.subs
            status-im.data-store.core
            [status-im.ui.screens.views :as views]
            [status-im.ui.components.react :as react]
            [status-im.notifications.core :as notifications]
            [status-im.core :as core]
            [status-im.utils.snoopy :as snoopy]))

(defn app-state-change-handler [state]
  (re-frame/dispatch [:app-state-change state]))

(defn app-root [props]
  (reagent/create-class
   {:component-will-mount
    (fn []
      (.addEventListener react/app-state "change" app-state-change-handler))
    :component-did-mount
    (fn [this]
      (re-frame/dispatch [:set-initial-props (reagent/props this)]))
    :component-will-unmount
    (fn []
      (.stop react/http-bridge)
      (.removeEventListener react/app-state "change" app-state-change-handler))
    :display-name "root"
    :reagent-render views/main}))

(defn init []
  (core/init app-root))
