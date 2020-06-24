(ns frodo.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]))

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
 :no-op
 (fn [db _]
   db))

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
                   :on-success [:remove-note note-id]}})))


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
 (fn [db [_ db-notes]]
   (let [notes (reduce #(assoc %1 (:id %2) %2) {} db-notes)]
     (assoc db :notes notes))))

(rf/reg-event-db
 :reset-note
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
 (fn [db [_ id]]
   (let [notes (:notes db)]
     (prn "remove note called")
     (prn id)
     (prn (dissoc notes id))
     (prn (assoc db :notes (dissoc notes id)))
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


(rf/reg-event-fx
 :update-note
 (fn [{:keys [db]} [_ id]]
   (let [note ((:notes db) id)]
     {:http-xhrio {:method :post
                   :uri "/api/notes/update"
                   :format (ajax/transit-request-format)
                   :response-format (ajax/transit-response-format)
                   :params note
                   :on-success [:no-op]}})))
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
