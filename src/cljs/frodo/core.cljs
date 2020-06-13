(ns frodo.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [markdown.core :refer [md->html]]
   [frodo.ajax :as ajax]
   [frodo.events]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [clojure.string :as string])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "frodo"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/about" "About" :about]
       [nav-link "#/notes" "Notes" :notes]]]]))

(defn textarea-input []
  (fn [content update-fn]
    (let [row-count (count (re-seq #"\n" content))]
      [:div.form-group
       [:textarea
        {:value     content                                     ;; initial value
         :rows      (if (< row-count 5) 7 (+ row-count 2))
         :class     "form-control"
         :on-change #(update-fn (-> % .-target .-value))}]])))

(defn note-component
  [id]
  (let [editing (r/atom false)
        note    (r/atom @(rf/subscribe [:note id]))
        save   #(do (rf/dispatch [:note-change id :content %])
                    (rf/dispatch [:update-note id])
                    (reset! editing false))

        cancel #(do (reset! note @(rf/subscribe [:note id]))
                    (reset! editing false))]
    (fn []
      [:div {:class "shadow p-3 mb-5 bg-white rounded"
             :on-double-click #(reset! editing (not @editing))}
       (if @editing
         [:div

          [textarea-input (:content @note)
           #(swap! note assoc :content %)]

          [:div.container
           [:div.row
            [:div.col-sm
             [:button.btn.btn-outline-primary {:type "button"
                                               :on-click #(save (:content @note))} "save"]]

            [:div.col-sm
             [:div.float-sm-right
              [:button.btn.btn-outline-secondary {:type "button"
                                                  :on-click cancel} "cancel"]]]]]]

         [:div
          [:div {:dangerouslySetInnerHTML {:__html (md->html (:content @note))}}]])])))

(defn notes-page []
  [:section.section>div.container>div.content
   [:h1 "Notes Page"]
   (for [id @(rf/subscribe [:note-ids])]
     ^{:key id} [note-component id])])

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        #'home-page
          :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
    ["/about" {:name :about
               :view #'about-page}]
    ["/notes" {:name :notes
               :view #'notes-page}]]))

(defn start-router! []
  (rfe/start!
   router
   navigate!
   {}))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app"))
  (rf/dispatch [:fetch-notes]))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
