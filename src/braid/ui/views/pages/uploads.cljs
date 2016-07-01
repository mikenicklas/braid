(ns braid.ui.views.pages.uploads
  (:require [reagent.core :as r]
            [reagent.ratom :refer-macros [run!]]
            [clojure.string :as string]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.routes :as routes]
            [chat.client.views.helpers :as helpers]
            [braid.ui.views.embed :refer [embed-view]]
            [braid.ui.views.pills :as pills]
            [braid.ui.views.thread :as thread]))

(defn upload-view
  [upload]
  (let [group-id (subscribe [:open-group-id])
        thread-id (r/atom (upload :thread-id))
        thread (subscribe [:thread] [thread-id])]
    (r/create-class
      {:display-name "upload-view"

       :component-will-update
       (fn [_ [_ new-upload]]
         (reset! thread-id (new-upload :thread-id)))

       :reagent-render
       (fn [upload]
         [:tr.upload
          [:td.uploaded-file
           [embed-view (upload :url)]
           [:br]
           (js/decodeURIComponent (last (string/split (upload :url) #"/")))]
          [:td.uploader
           "Uploaded by " [pills/user-pill-view (upload :uploader-id)]]
          [:td.uploaded-thread
           [:a {:href (routes/thread-path {:group-id @group-id
                                           :thread-id (upload :thread-id)})}
            (str "Uploaded at " (helpers/format-date (upload :uploaded-at)))]
           [:br]
           (if @thread
             [:span "Tagged with " [thread/thread-tags-view @thread]]
             [:button
              {:on-click (fn [_]
                           (dispatch! :load-threads
                                      {:thread-ids [(upload :thread-id)]}))}
              "Load thread to see tags"])]])})))

(defn uploads-view
  []
  (let [group-id (subscribe [:open-group-id])
        uploads (r/atom :initial)
        error (r/atom nil)
        ; TODO: will need to page this when it gets big?
        get-uploads (run! (dispatch! :get-group-uploads
                                     {:group-id @group-id
                                      :on-success (partial reset! uploads)
                                      :on-error (partial reset! error)}))]
    (fn []
      (let [_ @get-uploads]
        [:div.page.uploads
         [:div.title "Uploads"]
         [:div.content
          (cond
            @error [:div.error @error]
            (= :initial @uploads) [:div.loading "Loading..."]
            (empty? @uploads) [:p "No uploads in this group yet"]
            :else
            [:table.uploads
             [:thead
              [:tr [:th ""] [:th ""] [:th ""]]]
             (into [:tbody]
                   (for [upload @uploads]
                     ^{:key (upload :id)}
                     [upload-view upload]))])]]))))