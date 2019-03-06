(ns status-im.ui.components.toolbar.view
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.list-selection :as list-selection]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.styles :as components.styles]
            [status-im.ui.components.toolbar.actions :as actions]
            [status-im.ui.components.toolbar.styles :as styles]
            [status-im.utils.platform :as platform]
            [status-im.utils.core :as utils]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.common.common :as components.common]))

;; Navigation item

(defn nav-item
  [{:keys [handler accessibility-label style] :or {handler #(re-frame/dispatch [:navigate-back])}} item]
  [react/touchable-highlight
   (merge {:on-press handler}
          (when accessibility-label
            {:accessibility-label accessibility-label}))
   [react/view {:style style}
    item]])

(defn nav-button
  [{:keys [icon icon-opts unread-messages?] :as props}]
  [nav-item (merge {:style (styles/nav-item-button unread-messages?)} props)
   [vector-icons/icon icon (if unread-messages?
                             (assoc icon-opts :color :active)
                             icon-opts)]])

(defview nav-button-with-count [props]
  (letsubs [unread-messages-number [:chats/unread-messages-number]]
    (let [unread-messages? (pos? unread-messages-number)]
      [react/view {:flex-direction :row}
       [nav-button (assoc props :unread-messages? unread-messages?)]
       (when unread-messages?
         [nav-item (merge {:style styles/counter-container} props)
          [components.common/counter unread-messages-number]])])))

(defn nav-text
  ([text] (nav-text nil text))
  ([{:keys [handler] :as props} text]
   [react/text (utils/deep-merge {:style    styles/item-text
                                  :on-press (or handler #(re-frame/dispatch [:navigate-back]))}
                                 props)
    text]))

(defn nav-clear-text
  ([text] (nav-clear-text nil text))
  ([props text]
   (nav-text (merge props styles/item-text-white-background) text)))

(def default-nav-back [nav-button actions/default-back])
(def default-nav-close [nav-button actions/default-close])

(defn nav-back-count
  ([]
   [nav-button-with-count actions/default-back])
  ([{:keys [home?]}]
   [nav-button-with-count (if home? actions/home-back actions/default-back)]))

(defn default-done
  "Renders a touchable icon on Android or a label or iOS."
  [{:keys [icon] :as props}]
  (if platform/ios?
    [react/view
     [nav-text props
      (i18n/label :t/done)]]
    [react/view
     [nav-button (merge props {:icon (or icon :main-icons/close)})]]))

;; Content

(defn content-wrapper [content]
  [react/view {:style {:flex 1}}
   content])

(defn content-title
  ([title] (content-title nil title))
  ([title-style title]
   (content-title title-style title nil nil))
  ([title-style title subtitle-style subtitle]
   (content-title title-style title subtitle-style subtitle nil))
  ([title-style title subtitle-style subtitle additional-text-props]
   [react/view {:style styles/toolbar-title-container}
    [react/text (merge {:style (merge styles/toolbar-title-text title-style)
                        :font :toolbar-title
                        :numberOfLines 1
                        :ellipsizeMode :tail}
                       additional-text-props) title]
    (when subtitle
      [react/text {:style subtitle-style}
       subtitle])]))

;; Actions

(defn text-action [{:keys [style handler disabled? accessibility-label]} title]
  [react/text (cond-> {:style (merge styles/item-text
                                     style
                                     (when disabled?
                                       styles/toolbar-text-action-disabled))
                       :on-press   (when-not disabled?
                                     handler)
                       :uppercase? true}
                accessibility-label
                (assoc :accessibility-label accessibility-label))
   title])

(def blank-action [react/view {:style {:flex 1}}])

(defn- icon-action [icon {:keys [overlay-style] :as icon-opts} handler]
  [react/touchable-highlight {:on-press handler
                              :style {:width 24
                                      :height 24}}
   [react/view
    (when overlay-style
      [react/view overlay-style])
    [vector-icons/icon icon icon-opts]]])

(defn- option-actions [icon icon-opts options]
  [icon-action icon icon-opts
   #(list-selection/show {:options options})])

(defn actions [v]
  [react/view {:style {:flex-direction :row}}
   (for [{:keys [image icon icon-opts options handler]} v]
     (with-meta
       (cond (= image :blank)
             blank-action

             options
             [option-actions icon icon-opts options]

             :else
             [icon-action icon icon-opts handler])
       {:key (str "action-" (or image icon))}))])

(defn separator
  "TODO: refactor when implementing top bar component"
  [separator-color]
  [react/view {:style {:height 1
                       :background-color (or separator-color
                                             colors/gray-lighter)}}])

;;TODO remove
(defn toolbar
  ([props nav-item content-item] (toolbar props nav-item content-item nil))
  ([{:keys [style separator-color transparent? browser?]}
    nav-item
    content-item
    action-items]
   [react/view {:style (cond-> {:height styles/toolbar-height}
                         ;; i.e. for qr code scanner
                         transparent?
                         (assoc :background-color :transparent
                                :z-index          1))}
    [react/view {:style (merge {:height 55
                                :flex   1}
                               style)}
     (when content-item
       (if browser?
         content-item
         [react/view {:position         :absolute
                      :left             88
                      :right            88
                      :height           55
                      :justify-content  :center
                      :align-items      :center}
          content-item]))
     (when nav-item
       [react/view {:style {:position        :absolute
                            :left            16
                            :height          55
                            :justify-content :center
                            :align-items     :center}}
        nav-item])
     [react/view {:position :absolute
                  :right 16
                  :height 55
                  :justify-content  :center
                  :align-items      :center}
      action-items]]
    (when-not transparent?
      [separator separator-color])]))

;;TODO remove
(defn simple-toolbar
  "A simple toolbar composed of a nav-back item and a single line title."
  ([] (simple-toolbar nil))
  ([title] (simple-toolbar title false))
  ([title modal?] (toolbar nil (if modal? default-nav-close default-nav-back) [content-title title])))

(defn top-bar-content
  [title subtitle]
  [react/view {:style styles/toolbar-title-container}
   [react/text {:style styles/toolbar-title-text
                :font :toolbar-title
                :numberOfLines 1
                :ellipsizeMode :tail}
    title]
   (when subtitle
     [react/text {:style {}}
      subtitle])])

;;TODO replace toolbar with this
;;TODO WIP
(defn regular-top-bar
  [{:keys [title
           text-link
           icon
           second-icon
           subtitle
           modal-close
           private-chat
           group-chat
           separator-color]}]
  [react/view {:style {:height styles/toolbar-height}}
   [react/view {:style {:height 55
                        :flex   1}}
    [react/view {:position         :absolute
                 :left             88
                 :right            88
                 :height           55
                 :justify-content  :center
                 :align-items      :center}
     [top-bar-content title subtitle]]
    (if modal-close
      ;;TODO modal-close
      [react/view]
      [react/view {:style {:position        :absolute
                           :left            16
                           :height          55
                           :justify-content :center
                           :align-items     :center}}
       nav-item])
    (when icon
      [react/view {:position :absolute
                   :right 16
                   :height 55
                   :justify-content  :center
                   :align-items      :center}
       [icon-action icon]])
    (when second-icon
      [react/view {:position :absolute
                   :right 40
                   :height 55
                   :justify-content  :center
                   :align-items      :center}
       [icon-action second-icon]])]
   [separator separator-color]])

;;TODO handle browser differently (url bar instead of text ask Maciej)
(defn browser-top-bar [])

;;TODO not used yet can be done later
(defn big-top-bar [])
