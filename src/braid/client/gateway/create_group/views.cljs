(ns braid.client.gateway.create-group.views
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [dispatch subscribe]]
    [braid.client.gateway.helper-views :refer [form-view field-view]])
  (:import
    [goog.events KeyCodes]))

(defn group-name-field-view []
  [field-view
   {:id :gateway.action.create-group/group-name
    :ns :gateway.create-group
    :title "Group Name"
    :class "group-name"
    :type "text"
    :placeholder "Team Awesome"
    :help-text [:div
                [:p "Your group's name will show up in menus and headings."]
                [:p "It doesn't need to be formal and can always be changed later."]]}])

(defn group-url-field-view []
  [field-view
   {:id :gateway.action.create-group/group-url
    :ns :gateway.create-group
    :title "Group URL"
    :class "group-url"
    :type "text"
    :placeholder "awesome"
    :pre-input [:span.domain "braid.chat∕"]
    :help-text [:div
                [:p "Pick something short and recognizeable."]
                [:p "Lowercase letters, numbers and dashes only."]]
    :on-key-down (fn [e]
                   (when (and
                           (not (contains? #{KeyCodes.LEFT KeyCodes.RIGHT
                                             KeyCodes.UP KeyCodes.DOWN
                                             KeyCodes.TAB KeyCodes.BACKSPACE}
                                           (.. e -keyCode)))
                           (not (re-matches #"[A-Za-z0-9-]" (.. e -key))))
                     (.preventDefault e)))
    :on-change-transform string/lower-case}])

(defn group-type-field-view []
  (let [field-id :gateway.action.create-group/group-type
        value @(subscribe [:gateway.create-group/field-value field-id])
        status @(subscribe [:gateway.create-group/field-status field-id])
        errors @(subscribe [:gateway.create-group/field-errors field-id])]
    [:div.option.group-type
     {:class (name status)}
     [:h2 "Group Type"]
     (when (= :invalid status)
       [:div.error-message (first errors)])
     [:label {:class (when (= "public" value) "checked")}
      [:input {:type "radio"
               :name "type"
               :value "public"
               :checked (when (= "public" value))
               :on-blur (fn [_]
                          (dispatch [:gateway.create-group/blur field-id]))
               :on-click (fn [e]
                           (let [value (.. e -target -value)]
                             (dispatch [:gateway.create-group/update-value field-id value])))}]
      [:span "Public Group"]
      [:div.explanation
       [:p "Anyone can find your group through the Braid Group Directory."]
       [:p "Unlimited everything. Free forever."]]]
     [:label {:class (when (= "private" value) "checked")}
      [:input {:type "radio"
               :name "type"
               :value "private"
               :checked (when (= "private" value))
               :on-blur (fn [_]
                          (dispatch [:gateway.create-group/blur :type]))
               :on-click (fn [e]
                           (let [value (.. e -target -value)]
                             (dispatch [:gateway.create-group/update-value field-id value])))}]
      [:span "Private Group"]
      [:div.explanation
       [:p "Invite-only and hidden from the Braid Group Directory."]
       [:p "Free to evaluate, then pay-what-you-want."]]]]))

(defn button-view []
  (let [fields-valid? @(subscribe [:gateway.create-group/fields-valid?
                                  [:gateway.action.create-group/group-name
                                   :gateway.action.create-group/group-url
                                   :gateway.action.create-group/group-type]])
        sending? @(subscribe [:gateway.action.create-group/sending?])]
    [:button.submit
     {:type "submit"
      :class (str (when (not fields-valid?) "disabled") " "
                  (when sending? "sending"))}
     "Create your Braid group"]))

(defn error-view []
  (let [error @(subscribe [:gateway.action.create-group/error])]
    (when error
      [:div.error-message
       "An error occured. Please try again."])))

(defn create-group-view []
  [form-view
   {:title "Start a New Braid Group"
    :class "create-group"
    :disabled? @(subscribe [:gateway.action.create-group/disabled?])
    :on-submit
    (fn [e]
      (.preventDefault e)
      (dispatch [:gateway.create-group/submit-form
                 {:validate-fields
                  [:gateway.action.create-group/group-name
                   :gateway.action.create-group/group-url
                   :gateway.action.create-group/group-type]
                  :dispatch-when-valid [:gateway.action.create-group/remote-create-group]}]))}
   [group-name-field-view]
   [group-url-field-view]
   [group-type-field-view]
   [button-view]
   [error-view]])
