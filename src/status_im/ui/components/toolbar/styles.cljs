(ns status-im.ui.components.toolbar.styles
  (:require-macros [status-im.utils.styles :refer [defstyle defnstyle]])
  (:require [status-im.ui.components.colors :as colors]))

(def toolbar-height 56)
(def toolbar-icon-width 24)
(def toolbar-icon-height 24)
(def toolbar-icon-spacing 24)

(def toolbar
  {:height 55
   :flex   1})

(def toolbar-title-container
  {:justify-content :center
   :align-items     :center
   :flex-direction  :column
   :margin-left     6})

(defstyle toolbar-title-text
  {:color          colors/black
   :letter-spacing -0.2
   :font-size      17
   :font-weight    :bold
   :text-align     :center
   :margin-left    15
   :margin-right   15})

(defn toolbar-actions-container [actions-count custom]
  (merge {:flex-direction :row}
         (when-not custom {:margin-right 4})
         (when (and (zero? actions-count) (not custom))
           {:width (+ toolbar-icon-width toolbar-icon-spacing)})))

(def action-default
  {:width  24
   :height 24})

(defn nav-item-button [unread-messages?]
  {:margin-right (if unread-messages? -5 13)})

(def item-text
  {:color     colors/blue
   :font-size 17})

(defstyle item-text-action
  {:color   colors/blue
   :ios     {:font-size      15
             :letter-spacing -0.2}
   :android {:font-size      14
             :letter-spacing 0.5}})

(def toolbar-text-action-disabled {:color colors/gray})

(def item-text-white-background {:color colors/blue})

(def counter-container
  {:top 3})

(def icon-add
  {:width  24
   :height 24
   :color  colors/blue})

(def icon-add-illuminated
  {:width           24
   :height          24
   :color           colors/blue
   :container-style {:background-color (colors/alpha colors/blue 0.12)
                     :border-radius    32
                     :width            32
                     :height           32
                     :display          :flex
                     :justify-content  :center
                     :align-items      :center}})
