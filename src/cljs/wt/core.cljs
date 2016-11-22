(ns wt.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [cljs-time.core :as local]
            [accountant.core :as accountant]
            [wt.log :refer [log]]))

(defn local-date-time-format []
  (js->clj (.resolvedOptions (.DateTimeFormat js/Intl))))

(defn local-time []
  (time/local-date-time (time/now)))

;; -------------------------
;; Views

(defn hour-strip [now name offset]
  [:div.hour-columns
   [:div.name name]
   (let [start (time/plus now (time/hours offset))]
     (doall
      (for [i (range 0 24)]
        (let [next (time/plus start (time/hours i))
              hour (time/hour next)]
          ^{:key i} [:div.hour
                     (cond
                       (= hour 0) [:div.new-day
                                   [:div hour]
                                   [:div.day (format/unparse
                                              (format/formatter "dd MMM")
                                              next)]]
                       :else hour)]))))])

(defn home-page []
  (let [now (time/now)]
    [:div.hour-rows
     [:div.hour-row [hour-strip now (get (local-date-time-format) "timeZone") +0]]
     [:div.hour-row [hour-strip now "some/zone" -1]]]))

(defn about-page []
  [:div [:h2 "About wt"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (secretary/dispatch! path))
    :path-exists?
    (fn [path]
      (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
