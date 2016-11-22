(ns wt.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [cljs-time.core :as time]
            [cljs-time.core :as local]
            [accountant.core :as accountant]
            [wt.log :refer [log]]))

(defn local-date-time-format []
  (js->clj (.resolvedOptions (.DateTimeFormat js/Intl))))

(defn local-time []
  (time/local-date-time (time/now)))

;; -------------------------
;; Views

(defn hour-strip [from]
  [:div
   (doall
    (for [i (range from (+ from 24))]
      (let [hr (cond
                 (> i 24) (- i 24)
                 (= i 24) 0
                 :else i)]
        ^{:key hr} [:div hr])))])

(defn home-page []
  (log (local-time))
  [:div
   (get (local-date-time-format) "timeZone")
   [hour-strip 11]])

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
