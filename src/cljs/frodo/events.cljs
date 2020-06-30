(ns frodo.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

(def focus
  (rf/->interceptor
   :id :focus
   :after (fn [context]
            (.focus (.getElementById js/document "notebook"))
            context)))

(def scroll-into-view
  (rf/->interceptor
   :id :scroll-into-view
   :after (fn [context]
            (let [current-note-id (get-in context [:effects :db :current-note-id] )]
              (.scrollIntoView (.getElementById js/document (str "note-" current-note-id)))
              context))))
;;dispatchers

(rf/reg-event-db
 :common/navigate
 (fn [db [_ match]]
   (let [old-match (:common/route db)
         new-match (assoc match :controllers
                          (rfc/apply-controllers (:controllers old-match) match))]
     (assoc db :common/route new-match))))

(rf/reg-fx
 :common/navigate-fx!
 (fn [[k & [params query]]]
   (rfe/push-state k params query)))

(rf/reg-event-fx
 :common/navigate!
 (fn [_ [_ url-key params query]]
   {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))


(rf/reg-event-db
 :set-focus
 [focus]
 (fn [db _]
   db))


(rf/reg-event-db
 :set-initial-note-id
 [focus]
 (fn [db [_ id]]
   (let [first-key (first (keys (:notes db)))]
     (assoc db :current-note-id first-key))))

(rf/reg-event-db
 :set-current-note-id
 (fn [db [_ id]]
   (prn ":set-current-note-id called")
   (prn id)
   (assoc db :current-note-id id)))

(rf/reg-event-fx
 :delete-note
 (fn [{:keys [db]} _]
   (let [note-id (:current-note-id db)]
     {:http-xhrio {:method :post
                   :uri "/api/notes/delete"
                   :format (ajax/transit-request-format)
                   :response-format (ajax/transit-response-format)
                   :params {:id note-id}
                   :on-success [:remove-note note-id]}
      :dispatch [:next-note]})))


(rf/reg-event-fx
 :fetch-notes
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/api/notes"
                 :response-format (ajax/transit-response-format)
                 :on-success       [:set-notes]}}))


; Notes returned from backend database as a vector of maps where a single
; map represents a database row. Convert to a single map where each key is the
; id of the database row and the value is the row itself. This makes accessing
; a specific note easier.

(rf/reg-event-db
 :set-notes
   [focus]
 (fn [db [_ db-notes]]
   (let [notes (reduce #(assoc %1 (:id %2) %2) {} db-notes)
         db (assoc db :notes notes)]
    
         (assoc db :current-note-id (first (keys (:notes db)))))))

(rf/reg-event-db
 :reset-note
 [focus]
 (fn [db _]
   (assoc db :note {:content "" :creation_ts (.now js/Date)})))

(rf/reg-event-db
 :update-new-note
 (fn [db [_ content]]
     (assoc-in db [:note :content] content)))

(rf/reg-event-db
 :new-blank-note
 (fn [db _]
   (let [note {:content "" :creation_ts (.now js/Date) }]
     (assoc db :note note))))


(rf/reg-event-db
 :remove-note
 [focus]
 (fn [db [_ id]]
   (let [notes (:notes db)]
     (assoc db :notes (dissoc notes id)))))

(rf/reg-event-fx
 :add-note
 (fn [{:keys [db]} [_ note result]]
   {:db
   (let [id (:id result)
         note (assoc note :id id)
         notes (:notes db)
         ]
     (assoc db :notes (assoc notes id note)))
    :dispatch [:new-blank-note]}))


(rf/reg-event-fx
 :create-note
 (fn [{:keys [db]} _ ]
   (let [note (:note db)]
     (prn ":create-note")
     {:http-xhrio {:method :post
                   :uri "/api/notes/create"
                   :format (ajax/transit-request-format)
                   :response-format (ajax/transit-response-format)
                   :params note
                   :on-success [:add-note note]}})))

(rf/reg-event-db
 :next-note
 [scroll-into-view]
 (fn [db _]
   (prn ":next-note-called")
   (let [note-ids (keys (:notes db))
         current-note-id (:current-note-id db)
         next-note-ids (drop-while #(not= current-note-id %) note-ids)
         next-note-id (second next-note-ids)]
    
     (if next-note-id
       (assoc db :current-note-id next-note-id)
       (assoc db :current-note-id (last note-ids))))))

(rf/reg-event-db
 :previous-note
 [scroll-into-view]
 (fn [db _]
   (let [note-ids (reverse (keys (:notes db)))
         current-note-id (:current-note-id db)
         next-note-ids (drop-while #(not= current-note-id %) note-ids)
         next-note-id (second next-note-ids)]
   
     (if next-note-id
       (assoc db :current-note-id next-note-id)
       (assoc db :current-note-id (last note-ids))))))

(rf/reg-event-fx
 :update-note
 (fn [{:keys [db]} [_ id]]
   (let [note ((:notes db) id)]
     {:http-xhrio {:method :post
                   :uri "/api/notes/update"
                   :format (ajax/transit-request-format)
                   :response-format (ajax/transit-response-format)
                   :params note
                   :on-success [:set-focus]}})))
(rf/reg-event-db
 :note-change
 (fn [db [_ id field new-content]]
   (update-in db [:notes id field] (fn [] new-content))))

(rf/reg-event-fx
 :fetch-docs
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/docs"
                 :response-format (ajax/raw-response-format)
                 :on-success       [:set-docs]}}))

(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))

(rf/reg-event-fx
 :page/init-home
 (fn [_ _]
   {:dispatch [:fetch-docs]}))

;;subscriptions

(rf/reg-sub
 :common/route
 (fn [db _]
   (-> db :common/route)))

(rf/reg-sub
 :common/page-id
 :<- [:common/route]
 (fn [route _]
   (-> route :data :name)))

(rf/reg-sub
 :common/page
 :<- [:common/route]
 (fn [route _]
   (-> route :data :view)))

(rf/reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(rf/reg-sub
 :common/error
 (fn [db _]
   (:common/error db)))

(rf/reg-sub
 :note-ids
 (fn [db _]
   (keys (:notes db))))

(rf/reg-sub
 :note
 (fn [db [_ id]]
   ((:notes db) id)))

(rf/reg-sub
 :new-note
 (fn [db _]
   (:note db)))

(rf/reg-sub
 :current-note-id
 (fn [db _]
   (:current-note-id db)))

