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

(rf/reg-event-fx
 :fetch-notes
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/api/notes"
                 :response-format (ajax/transit-response-format)
                 :on-success       [:set-notes]}}))

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
   (keys(:notes db))))

(rf/reg-sub
 :note
 (fn [db [_ id]]
   ((:notes db) id)))

