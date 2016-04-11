(ns braid.common.state
  (:require
    [reagent.ratom :include-macros true :refer-macros [reaction]]
    [clojure.set :refer [union intersection]]))

(defn set-active-group-id!
  [state [_ group-id]]
  (assoc state :open-group-id group-id))

(defn get-active-group
  [state _]
  (let [group-id (reaction (:open-group-id @state))]
    (reaction (get-in @state [:groups @group-id]))))

(defn get-groups
  [state _]
  (reaction (vals (:groups @state))))

(defn- thread-unseen?
  [thread]
  (> (->> (thread :messages)
          (map :created-at)
          (apply max))
     (thread :last-open-at)))

(defn get-group-unread-count
  [state [_ group-id]]
  (let [open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
        threads (reaction (@state :threads))
        tags (reaction (@state :tags))
        users (reaction (@state :users))
        group-ids->user-ids (reaction (->> @users
                                           vals
                                           (mapcat (fn [u]
                                                     (map
                                                       (fn [gid]
                                                         {:id (u :id) :group-id gid})
                                                       (u :group-ids))))
                                           (group-by :group-id)
                                           (map (fn [[k vs]]
                                                  [k (map (fn [v] (v :id)) vs)]))
                                           (into {})))
        group-user-ids (set (@group-ids->user-ids group-id))
        thread-in-group? (fn [thread]
                           (if (seq (thread :tag-ids))
                             (= group-id (:group-id (@tags (first (thread :tag-ids)))))
                             (let [user-ids-from-messages (->> (thread :messages)
                                                               (map :user-id)
                                                               set)
                                   user-ids-from-refs (set (thread :user-ids))
                                   user-ids (union user-ids-from-messages
                                                   user-ids-from-refs)]
                               (< 0 (count (intersection group-user-ids user-ids))))))
        unseen-threads (reaction
                         (->>
                           (select-keys @threads @open-thread-ids)
                           vals
                           (filter thread-unseen?)
                           (filter thread-in-group?)))]
    (reaction (count @unseen-threads))))

(defn get-page
  [state _]
  (reaction (@state :page)))

(defn get-open-threads
  [state _]
  (let [current-group-id (reaction (@state :open-group-id))
        open-thread-ids (reaction (get-in @state [:user :open-thread-ids]))
        group-for-tag (fn [tag-id]
                        (get-in @state [:tags tag-id :group-id]))
        threads (reaction (@state :threads))
        open-threads (-> @threads
                         (select-keys @open-thread-ids)
                         vals
                         (->> (filter (fn [thread]
                                (or (empty? (thread :tag-ids))
                                    (contains?
                                      (into #{} (map group-for-tag (thread :tag-ids)))
                                      @current-group-id))))))]
      (reaction open-threads)))

(defn get-users-in-group
  [state [_ group-id]]
  (reaction
    (->> (state :users)
         vals
         (filter (fn [u] (contains? (set (u :group-ids)) group-id))))))

(defn get-open-group-id
  [state _]
  (reaction (get-in @state [:open-group-id])))

(defn get-users-in-open-group
  [state _]
  (reaction @(get-users-in-group @state [nil (@state :open-group-id)])))

(defn get-user-id
  [state _]
  (reaction (get-in @state [:session :user-id])))

(defn get-all-tags
  [state _]
  (reaction (vals (get-in @state [:tags]))))

(defn get-user-subscribed-to-tag
  [state [_ tag-id]]
  (reaction (contains? (set (get-in @state [:user :subscribed-tag-ids])) tag-id)))

(defn get-group-subscribed-tags
  [state [_ group-id]]
  (reaction
    (->> (vals (get-in @state [:tags]))
         (filter (fn [tag] (= (get-in @state [:open-group-id]) (tag :group-id))))
         (filter (fn [tag] @(get-user-subscribed-to-tag state [nil (tag :id)]))))))

(defn get-user-avatar-url
  [state [_ user-id]]
  (reaction (get-in @state [:users user-id :avatar])))

(defn get-user-status
  [state [_ user-id]]
  (reaction (get-in @state [:users user-id :status])))

(defn get-search-query
  [state _]
  (reaction (get-in @state [:page :search-query])))

