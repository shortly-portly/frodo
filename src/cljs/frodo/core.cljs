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
   [reitit.frontend.easy :as rfe])
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
       [nav-link "#/notes" "Notes" :notes]
       [nav-link "#/test" "Test" :test]]]]))

(defn textarea-input []
  (fn [content update-fn]
    (let [row-count (count (re-seq #"\n" content))]
      [:div.form-group
       [:textarea
        {:value     content                                     ;; initial value
         :rows      (if (< row-count 5) 7 (+ row-count 2))
         :class     "form-control"
         :on-key-up #(.stopPropagation %)
         :on-change #(update-fn (-> % .-target .-value))}]])))

(defn new-note-component
  []
  (fn  []
    (let [note @(rf/subscribe [:new-note])]
      [:div {:class "shadow p-3 mb-5 bg-white rounded"}
       [textarea-input (:content note)
        #(rf/dispatch [:update-new-note %])]

       [:div.container
        [:div.row
         [:div.col-sm
          [:button.btn.btn-outline-primary {:type "button"
                                            :on-click #(rf/dispatch [:create-note])}
           "save"]]

         [:div.col-sm
          [:div.float-sm-right
           [:button.btn.btn-outline-secondary {:type "button"
                                               :on-click #(rf/dispatch [:reset-note])}
            "cancel"]]]]]])))

(defn note-component
  [id]
  (let [editing (r/atom false)
        note    (r/atom @(rf/subscribe [:note id]))
        save   #(do (rf/dispatch [:note-change id :content %])
                    (rf/dispatch [:update-note id])
                    (reset! editing false))

        cancel #(do (reset! note @(rf/subscribe [:note id]))
                    (reset! editing false)
                    (rf/dispatch [:set-focus])

                    )]


    (fn []
      [:div {:class (str "shadow p-3 mb-5 rounded " (if (= id @(rf/subscribe [:current-note-id])) "selected" "not-selected"))
             :id (str "note-" id)
             :on-double-click #(reset! editing (not @editing))}
       [:div (str (js/Date. (:creation_ts @note)))]
       (if @editing
         [:div

          [textarea-input (:content @note)
           #(swap! note assoc :content %)]

          [:div.container
           [:div.row
            [:div.col-sm
             [:button.btn.btn-outline-primary {:type "button"
                                               :on-click #(save (:content @note))} "save"]]

            [:div.float-sm-right

             [:button.btn.btn-outline-warning.mr-2 {:type "button"
                                                    :data-toggle "modal"
                                                    :on-click (rf/dispatch [:set-current-note-id (:id @note)])
                                                    :data-target "#exampleModal1"} "delete"]
             [:button.btn.btn-outline-secondary {:type "button"
                                                 :on-click cancel} "cancel"]]]]]

         [:div.active
          [:div {
                 :dangerouslySetInnerHTML {:__html (md->html (:content @note))}}]])])))

(defn modal-component []
  (fn []

    [:div.modal.fade.show {:id "exampleModal1" :role "dialog"}
     [:div.modal-dialog {:role "document"}
      [:div.modal-content
       [:div.modal-header
        [:h5.modal-title "Delete Note?"]
        [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
         [:span {:aria-hidden "true" :dangerouslySetInnerHTML {:__html "&times;"}}]]]
       [:div.modal-body
        [:p ]]
       [:div.modal-footer
        [:button.btn.btn-primary {:type "button"
                                  :on-click #(rf/dispatch [:delete-note])
                                  :data-dismiss "modal"} "Yes"]
        [:button.btn.btn-secondary {:type "button"
                                    :data-dismiss "modal"
                                    :on-click #(rf/dispatch [:set-focus])
                                    } "Cancel"]]]]]))

(defn test-page []
  [:div
  
   [modal-component]
   [:div {:data-toggle "modal" :data-target "#exampleModal1"}]
   [:button.btn.btn-primary {:data-toggle "modal" :data-target "#exampleModal1"} "Launch Demo Modal"]])

(defn notes-page []
  (println "notes-page called")
  [:section.section>div.container>div.content
   [:div
    {:id "notebook"
     :tabIndex "0"
     :on-key-up #(case (.-which %)
                   78 (rf/dispatch [:next-note])
                   79 (rf/dispatch [:previous-note]))
             }


   [:h1 "Notes Page"]
    [:div
     [:span "Next note id " @(rf/subscribe [:current-note-id])]]
   [new-note-component]
   (for [id @(rf/subscribe [:note-ids])]
     (doall
      [:div
       [:div
        [:hr]
        ]
       ^{:key id} [note-component id]]))

   [:div
    [modal-component]]]
   ])

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
               :view #'notes-page}]
    ["/test" {:name :test
              :view #'test-page}]]))

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
  (rf/dispatch [:new-blank-note])
  (rf/dispatch [:fetch-notes])
  )

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
